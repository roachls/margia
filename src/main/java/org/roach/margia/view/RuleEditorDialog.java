package org.roach.margia.view;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.DataDrivenMusicianRule;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.controller.rules.storage.RuleRepository;
import org.roach.margia.controller.rules.storage.RuleRepository.RuleSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * In-program JavaFX editor for creating, modifying, validating, and saving
 * YAML-backed {@link RuleDefinition}s.
 */
public class RuleEditorDialog extends Dialog<Void> {

    private final ListView<String> ruleListView = new ListView<>();
    private final TextField ruleNameField = new TextField();
    private final ComboBox<String> startingStateCombo = new ComboBox<>();
    private final TextArea yamlEditorArea = new TextArea();
    private final Label statusLabel = new Label();

    private final TabPane tabPane = new TabPane();
    private Tab visualTab;
    private Tab yamlTab;

    private final Button saveBtn = new Button("Save & Apply Live");
    private final Button saveAsBtn = new Button("Save As...");
    private final Button deleteBtn = new Button("Delete Selected");

    private final ObservableList<MutableProperty> propertyRows = FXCollections.observableArrayList();
    private final ObservableList<MutableState> stateRows = FXCollections.observableArrayList();

    private final List<Musician> activeMusicians;
    private boolean syncingTabs = false;

    /**
     * @param owner           the owner of this window
     * @param activeMusicians the list of active musicians
     */
    public RuleEditorDialog(Window owner, List<Musician> activeMusicians) {
        this.activeMusicians = activeMusicians != null ? activeMusicians : List.of();
        initOwner(owner);
        setTitle("Rule Editor");
        setResizable(true);
        getDialogPane().setPrefSize(940, 650);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(buildSidebar(), buildEditorPane());
        splitPane.setDividerPositions(0.24);

        VBox root = new VBox(8, splitPane, statusLabel);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.setPadding(new Insets(10));
        getDialogPane().setContent(root);

        refreshRuleList();
        if (!ruleListView.getItems().isEmpty()) {
            ruleListView.getSelectionModel().selectFirst();
        } else {
            loadDefinitionIntoEditor(createStarterTemplate());
            deleteBtn.setDisable(true);
        }
    }

    private VBox buildSidebar() {
        Label header = new Label("Rules & Templates");
        header.setStyle("-fx-font-weight: bold;");

        ruleListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    boolean isTemplate = RuleRepository.getInstance().getRuleSource(item) == RuleSource.TEMPLATE_YAML;
                    setText(isTemplate ? item + " [Template]" : item);
                }
            }
        });

        Button newBtn = new Button("New Blank");
        newBtn.setMaxWidth(Double.MAX_VALUE);
        newBtn.setOnAction(_ -> {
            ruleListView.getSelectionModel().clearSelection();
            loadDefinitionIntoEditor(createStarterTemplate());
            saveBtn.setDisable(false);
            deleteBtn.setDisable(true);
            ruleNameField.setEditable(true);
            setStatus("Editing new rule (unsaved)", false);
        });

        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(_ -> deleteSelectedRule());

        ruleListView.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal == null) {
                return;
            }
            var repo = RuleRepository.getInstance();
            RuleSource source = repo.getRuleSource(newVal);
            RuleDefinition def = (source == RuleSource.TEMPLATE_YAML) ? repo.getTemplateDefinitions().get(newVal)
                    : repo.getExternalDefinitions().get(newVal);

            if (def != null) {
                loadDefinitionIntoEditor(def);
                boolean isTemplate = (source == RuleSource.TEMPLATE_YAML);
                saveBtn.setDisable(isTemplate);
                deleteBtn.setDisable(isTemplate);
                ruleNameField.setEditable(!isTemplate);

                setStatus(isTemplate
                        ? "Loaded read-only template '" + newVal + "' (click 'Save As...' to create an editable copy)"
                        : "Loaded user rule '" + newVal + "'", false);
            }
        });

        VBox.setVgrow(ruleListView, Priority.ALWAYS);
        VBox sidebar = new VBox(8, header, ruleListView, newBtn, deleteBtn);
        sidebar.setPadding(new Insets(4));
        return sidebar;
    }

    private VBox buildEditorPane() {
        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(10);
        metaGrid.setVgap(8);
        ruleNameField.setPromptText("e.g., myCustomRule");
        startingStateCombo.setEditable(true);
        startingStateCombo.setPromptText("Starting state name");

        metaGrid.add(new Label("Rule Name:"), 0, 0);
        metaGrid.add(ruleNameField, 1, 0);
        metaGrid.add(new Label("Starting State:"), 2, 0);
        metaGrid.add(startingStateCombo, 3, 0);

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        visualTab = new Tab("Visual Builder", buildVisualBuilder());
        yamlEditorArea.setStyle("-fx-font-family: 'monospaced'; -fx-font-size: 12px;");
        yamlTab = new Tab("YAML Source", yamlEditorArea);

        tabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> {
            if (syncingTabs)
                return;
            syncingTabs = true;
            try {
                if (newTab == yamlTab) {
                    RuleDefinition def = buildDefinitionFromForm();
                    yamlEditorArea.setText(RuleRepository.getInstance().toYamlString(def));
                    setStatus("YAML updated from form", false);
                } else if (newTab == visualTab) {
                    RuleDefinition def = RuleRepository.getInstance().fromYamlString(yamlEditorArea.getText());
                    populateFormFromDefinition(def);
                    setStatus("Form updated from YAML", false);
                }
            } catch (Exception ex) {
                setStatus("Sync error: " + ex.getMessage(), true);
            } finally {
                syncingTabs = false;
            }
        });

        tabPane.getTabs().addAll(visualTab, yamlTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Button validateBtn = new Button("Validate");
        validateBtn.setOnAction(_ -> validateCurrent());

        saveBtn.setStyle("-fx-font-weight: bold;");
        saveBtn.setOnAction(_ -> saveCurrentRule());

        saveAsBtn.setOnAction(_ -> saveAsCurrentRule());

        HBox actionBar = new HBox(10, validateBtn, saveBtn, saveAsBtn);
        actionBar.setPadding(new Insets(4, 0, 0, 0));

        VBox editorPane = new VBox(10, metaGrid, tabPane, actionBar);
        editorPane.setPadding(new Insets(4));
        return editorPane;
    }

    private VBox buildVisualBuilder() {
        Label propsHeader = new Label("Configurable UI Properties (referenced via $key)");
        propsHeader.setStyle("-fx-font-weight: bold;");

        ListView<MutableProperty> propList = new ListView<>(propertyRows);
        propList.setPrefHeight(150);
        propList.setCellFactory(_ -> new PropertyCell(propertyRows::remove));

        Button addPropBtn = new Button("+ Add Property");
        addPropBtn.setOnAction(
                _ -> propertyRows.add(new MutableProperty("INT", "newParam", "New Param:", "10", "0", "100", "1", "")));

        Label statesHeader = new Label("States, Actions & Transitions");
        statesHeader.setStyle("-fx-font-weight: bold;");

        ListView<MutableState> stateList = new ListView<>(stateRows);
        VBox.setVgrow(stateList, Priority.ALWAYS);
        stateList.setCellFactory(_ -> new StateCell(stateRows::remove, this::refreshStartingStateChoices));

        Button addStateBtn = new Button("+ Add State");
        addStateBtn.setOnAction(_ -> {
            stateRows.add(new MutableState("state_" + (stateRows.size() + 1), "ALWAYS_TRANSITION", "16", "",
                    "PLAY_CHORD", ""));
            refreshStartingStateChoices();
        });

        VBox box = new VBox(8, propsHeader, propList, addPropBtn, new Separator(), statesHeader, stateList,
                addStateBtn);
        box.setPadding(new Insets(8));
        return box;
    }

    private void loadDefinitionIntoEditor(RuleDefinition def) {
        populateFormFromDefinition(def);
        try {
            yamlEditorArea.setText(RuleRepository.getInstance().toYamlString(def));
        } catch (IOException e) {
            yamlEditorArea.setText("# Error serializing YAML: " + e.getMessage());
        }
    }

    private void populateFormFromDefinition(RuleDefinition def) {
        ruleNameField.setText(def.name() != null ? def.name() : "");
        propertyRows.clear();
        for (PropertyDef p : def.properties()) {
            switch (p) {
            case PropertyDef.IntProp i -> propertyRows.add(new MutableProperty("INT", i.key(), i.label(),
                    String.valueOf(i.defaultValue()), String.valueOf(i.min()), String.valueOf(i.max()),
                    String.valueOf(i.step() != null ? i.step() : 1), ""));
            case PropertyDef.DoubleProp d -> propertyRows.add(new MutableProperty("DOUBLE", d.key(), d.label(),
                    String.valueOf(d.defaultValue()), String.valueOf(d.min()), String.valueOf(d.max()),
                    String.valueOf(d.step() != null ? d.step() : 0.1), ""));
            case PropertyDef.BooleanProp b -> propertyRows.add(new MutableProperty("BOOLEAN", b.key(), b.label(),
                    String.valueOf(b.defaultValue()), "", "", "", ""));
            case PropertyDef.StringProp s ->
                propertyRows.add(new MutableProperty("STRING", s.key(), s.label(), s.defaultValue(), "", "", "", ""));
            case PropertyDef.StringListProp sl -> propertyRows.add(new MutableProperty("STRING_LIST", sl.key(),
                    sl.label(), sl.defaultValue(), "", "", "", String.join(",", sl.values())));
            }
        }
        stateRows.clear();
        def.states().forEach((name, stateDef) -> {
            String actionSummary = serializeActionsSummary(stateDef.actions());
            switch (stateDef) {
            case StateDef.AlwaysTransition a ->
                stateRows.add(new MutableState(name, "ALWAYS_TRANSITION", "", a.nextState(), actionSummary, ""));
            case StateDef.DelayedTransition d -> stateRows.add(
                    new MutableState(name, "DELAYED_TRANSITION", String.valueOf(d.delay()), d.wrappedState(), "", ""));
            case StateDef.Countdown c ->
                stateRows.add(new MutableState(name, "COUNTDOWN", String.valueOf(c.ticks()), c.nextState(), "", ""));
            case StateDef.PseudoRandom pr -> {
                String ranges = pr.rangeTransitions().stream().map(rt -> rt.min() + "-" + rt.max() + "->" + rt.target())
                        .collect(Collectors.joining("; "));
                stateRows.add(new MutableState(name, "PSEUDO_RANDOM", String.valueOf(pr.maxRoll()), "", actionSummary,
                        ranges));
            }
            }
        });
        refreshStartingStateChoices();
        startingStateCombo.setValue(def.startingState());
    }

    private RuleDefinition buildDefinitionFromForm() {
        String name = ruleNameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be empty.");
        }
        List<PropertyDef> props = propertyRows.stream().map(MutableProperty::toPropertyDef).toList();
        Map<String, StateDef> states = new LinkedHashMap<>();
        for (MutableState ms : stateRows) {
            states.put(ms.name.trim(), ms.toStateDef());
        }
        String start = startingStateCombo.getValue();
        if (start == null || !states.containsKey(start)) {
            throw new IllegalArgumentException("Starting state '" + start + "' must match one of the defined states.");
        }
        return new RuleDefinition(name, start, props, states);
    }

    private RuleDefinition getActiveDefinition() throws IOException {
        return (tabPane.getSelectionModel().getSelectedItem() == yamlTab)
                ? RuleRepository.getInstance().fromYamlString(yamlEditorArea.getText())
                : buildDefinitionFromForm();
    }

    private void validateCurrent() {
        try {
            RuleDefinition def = getActiveDefinition();
            new DataDrivenMusicianRule(def);
            setStatus("Valid rule definition: '" + def.name() + "'", false);
        } catch (Exception ex) {
            setStatus("Validation failed: " + ex.getMessage(), true);
        }
    }

    private void saveCurrentRule() {
        try {
            RuleDefinition def = getActiveDefinition();
            if (RuleRepository.getInstance().getRuleSource(def.name()) == RuleSource.TEMPLATE_YAML) {
                setStatus("Cannot overwrite template '" + def.name() + "'. Use 'Save As...' instead.", true);
                return;
            }
            new DataDrivenMusicianRule(def);
            RuleRepository.getInstance().saveRule(def);
            int reloadedCount = 0;
            for (Musician m : activeMusicians) {
                if (m.getRule() != null && def.name().equals(m.getRule().getName())) {
                    RuleRepository.getInstance().createRuleByName(def.name()).ifPresent(updated -> {
                        m.setRule(updated);
                        updated.initActionsAfterMusicianAssigned();
                    });
                    reloadedCount++;
                }
            }
            refreshRuleList();
            ruleListView.getSelectionModel().select(def.name());
            setStatus("Saved '" + def.name() + "' (hot-reloaded " + reloadedCount + " active musician(s))", false);
        } catch (Exception ex) {
            setStatus("Save failed: " + ex.getMessage(), true);
        }
    }

    private void saveAsCurrentRule() {
        TextInputDialog prompt = new TextInputDialog(ruleNameField.getText().trim() + "_copy");
        prompt.initOwner(getDialogPane().getScene().getWindow());
        prompt.setTitle("Save Rule As");
        prompt.setHeaderText("Save a custom editable copy of this rule");
        prompt.setContentText("New rule name:");
        prompt.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty()) {
                setStatus("Rule name cannot be empty.", true);
                return;
            }
            if (RuleRepository.getInstance().getRuleSource(trimmed) == RuleSource.TEMPLATE_YAML) {
                setStatus("Cannot overwrite a built-in template name: '" + trimmed + "'", true);
                return;
            }
            try {
                RuleDefinition baseDef = getActiveDefinition();
                RuleDefinition renamedDef = new RuleDefinition(trimmed, baseDef.startingState(), baseDef.properties(),
                        baseDef.states());
                loadDefinitionIntoEditor(renamedDef);
                ruleNameField.setEditable(true);
                saveBtn.setDisable(false);
                deleteBtn.setDisable(false);
                saveCurrentRule();
            } catch (Exception ex) {
                setStatus("Save As failed: " + ex.getMessage(), true);
            }
        });
    }

    private void deleteSelectedRule() {
        String selected = ruleListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        try {
            RuleRepository.getInstance().deleteExternalRule(selected);
            refreshRuleList();
            setStatus("Deleted '" + selected + "'", false);
        } catch (IOException ex) {
            setStatus("Delete failed: " + ex.getMessage(), true);
        }
    }

    private void refreshRuleList() {
        var repo = RuleRepository.getInstance();
        List<String> combined = new ArrayList<>();
        combined.addAll(repo.getExternalDefinitions().keySet());
        combined.addAll(repo.getTemplateDefinitions().keySet());
        ruleListView.getItems().setAll(combined);
    }

    private void refreshStartingStateChoices() {
        String current = startingStateCombo.getValue();
        List<String> names = stateRows.stream().map(s -> s.name).filter(n -> !n.isBlank()).toList();
        startingStateCombo.getItems().setAll(names);
        if (current != null)
            startingStateCombo.setValue(current);
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }

    private static String serializeActionsSummary(List<ActionDef> actions) {
        if (actions == null || actions.isEmpty())
            return "";
        return actions.stream().map(a -> a.params().isEmpty() ? a.type() : a.type() + a.params())
                .collect(Collectors.joining(", "));
    }

    private static RuleDefinition createStarterTemplate() {
        return new RuleDefinition("customEchoRule", "playAndHold",
                List.of(new PropertyDef.IntProp("holdTicks", "Hold Ticks:", 8, 1, 128, 1)),
                Map.of("play",
                        new StateDef.AlwaysTransition(List.of(new ActionDef("PLAY_CHORD", Map.of())), "playAndHold"),
                        "playAndHold", new StateDef.DelayedTransition("$holdTicks", "play")));
    }

    private static class MutableProperty {
        String type, key, label, defaultVal, min, max, step, listValues;

        MutableProperty(String type, String key, String label, String defaultVal, String min, String max, String step,
                String listValues) {
            this.type = type;
            this.key = key;
            this.label = label;
            this.defaultVal = defaultVal;
            this.min = min;
            this.max = max;
            this.step = step;
            this.listValues = listValues;
        }

        PropertyDef toPropertyDef() {
            return switch (type) {
            case "INT" -> new PropertyDef.IntProp(key, label, Integer.parseInt(defaultVal), Integer.parseInt(min),
                    Integer.parseInt(max), step.isBlank() ? 1 : Integer.parseInt(step));
            case "DOUBLE" -> new PropertyDef.DoubleProp(key, label, Double.parseDouble(defaultVal),
                    Double.parseDouble(min), Double.parseDouble(max), step.isBlank() ? 0.1 : Double.parseDouble(step));
            case "BOOLEAN" -> new PropertyDef.BooleanProp(key, label, Boolean.parseBoolean(defaultVal));
            case "STRING" -> new PropertyDef.StringProp(key, label, defaultVal);
            case "STRING_LIST" -> new PropertyDef.StringListProp(key, label, defaultVal,
                    new LinkedHashSet<>(Arrays.asList(listValues.split("\s*,\s*"))));
            default -> throw new IllegalArgumentException("Unsupported property type: " + type);
            };
        }
    }

    private static class MutableState {
        String name, type, numberOrPlaceholder, targetState, actionsText, rangesText;

        MutableState(String name, String type, String numberOrPlaceholder, String targetState, String actionsText,
                String rangesText) {
            this.name = name;
            this.type = type;
            this.numberOrPlaceholder = numberOrPlaceholder;
            this.targetState = targetState;
            this.actionsText = actionsText;
            this.rangesText = rangesText;
        }

        StateDef toStateDef() {
            Object numericArg = numberOrPlaceholder.startsWith("$") ? numberOrPlaceholder
                    : (numberOrPlaceholder.isBlank() ? 0 : Integer.parseInt(numberOrPlaceholder.trim()));
            List<ActionDef> actions = parseActions(actionsText);
            return switch (type) {
            case "ALWAYS_TRANSITION" -> new StateDef.AlwaysTransition(actions, targetState.trim());
            case "DELAYED_TRANSITION" -> new StateDef.DelayedTransition(numericArg, targetState.trim());
            case "COUNTDOWN" -> new StateDef.Countdown(numericArg, targetState.trim());
            case "PSEUDO_RANDOM" -> new StateDef.PseudoRandom(numericArg, actions, parseRanges(rangesText));
            default -> throw new IllegalArgumentException("Unsupported state type: " + type);
            };
        }

        private static List<ActionDef> parseActions(String text) {
            if (text == null || text.isBlank())
                return List.of();
            List<ActionDef> list = new ArrayList<>();
            for (String token : text.split(",")) {
                String trimmed = token.trim();
                int brace = trimmed.indexOf('{');
                if (brace > 0 && trimmed.endsWith("}")) {
                    String actType = trimmed.substring(0, brace).trim();
                    String inner = trimmed.substring(brace + 1, trimmed.length() - 1);
                    Map<String, Object> params = new LinkedHashMap<>();
                    for (String kv : inner.split(";")) {
                        String[] parts = kv.split("=", 2);
                        if (parts.length == 2) {
                            String v = parts[1].trim();
                            params.put(parts[0].trim(), v.startsWith("$") ? v : Integer.parseInt(v));
                        }
                    }
                    list.add(new ActionDef(actType, params));
                } else {
                    list.add(new ActionDef(trimmed, Map.of()));
                }
            }
            return list;
        }

        private static List<RangeTransition> parseRanges(String text) {
            if (text == null || text.isBlank())
                return List.of();
            List<RangeTransition> ranges = new ArrayList<>();
            for (String part : text.split(";")) {
                String[] arrow = part.trim().split("->");
                if (arrow.length == 2) {
                    String[] bounds = arrow[0].trim().split("-");
                    ranges.add(new RangeTransition(Integer.parseInt(bounds[0].trim()),
                            Integer.parseInt(bounds[1].trim()), arrow[1].trim()));
                }
            }
            return ranges;
        }
    }

    private static class PropertyCell extends ListCell<MutableProperty> {
        private final Consumer<MutableProperty> onRemove;

        PropertyCell(Consumer<MutableProperty> onRemove) {
            this.onRemove = onRemove;
        }

        @Override
        protected void updateItem(MutableProperty item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            ComboBox<String> typeBox = new ComboBox<>(
                    FXCollections.observableArrayList("INT", "DOUBLE", "BOOLEAN", "STRING", "STRING_LIST"));
            typeBox.setValue(item.type);
            typeBox.setOnAction(_ -> item.type = typeBox.getValue());
            TextField keyField = field(item.key, "key", 110, v -> item.key = v);
            TextField labelField = field(item.label, "Label:", 130, v -> item.label = v);
            TextField defField = field(item.defaultVal, "default", 70, v -> item.defaultVal = v);
            TextField minField = field(item.min, "min", 55, v -> item.min = v);
            TextField maxField = field(item.max, "max", 55, v -> item.max = v);
            Button removeBtn = new Button("X");
            removeBtn.setOnAction(_ -> onRemove.accept(item));
            setGraphic(new HBox(6, typeBox, keyField, labelField, defField, minField, maxField, removeBtn));
        }

        private static TextField field(String val, String prompt, double width, Consumer<String> onChange) {
            TextField tf = new TextField(val);
            tf.setPromptText(prompt);
            tf.setPrefWidth(width);
            tf.textProperty().addListener((_, _, newV) -> onChange.accept(newV));
            return tf;
        }
    }

    private static class StateCell extends ListCell<MutableState> {
        private final Consumer<MutableState> onRemove;
        private final Runnable onNameChanged;

        StateCell(Consumer<MutableState> onRemove, Runnable onNameChanged) {
            this.onRemove = onRemove;
            this.onNameChanged = onNameChanged;
        }

        @Override
        protected void updateItem(MutableState item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            TextField nameField = new TextField(item.name);
            nameField.setPromptText("State Name");
            nameField.setPrefWidth(120);
            nameField.textProperty().addListener((_, _, newV) -> {
                item.name = newV;
                onNameChanged.run();
            });
            ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("ALWAYS_TRANSITION",
                    "DELAYED_TRANSITION", "PSEUDO_RANDOM", "COUNTDOWN"));
            typeBox.setValue(item.type);
            typeBox.setOnAction(_ -> item.type = typeBox.getValue());
            TextField numField = new TextField(item.numberOrPlaceholder);
            numField.setPromptText("ticks / maxRoll / $prop");
            numField.setPrefWidth(110);
            numField.textProperty().addListener((_, _, newV) -> item.numberOrPlaceholder = newV);
            TextField targetField = new TextField(item.targetState);
            targetField.setPromptText("next / wrapped state");
            targetField.setPrefWidth(120);
            targetField.textProperty().addListener((_, _, newV) -> item.targetState = newV);
            TextField actionsField = new TextField(item.actionsText);
            actionsField.setPromptText("Actions (e.g. PLAY_CHORD)");
            actionsField.setPrefWidth(160);
            actionsField.textProperty().addListener((_, _, newV) -> item.actionsText = newV);
            TextField rangesField = new TextField(item.rangesText);
            rangesField.setPromptText("Ranges (1-3->stateA; 4-6->stateB)");
            rangesField.setPrefWidth(180);
            rangesField.textProperty().addListener((_, _, newV) -> item.rangesText = newV);
            Button removeBtn = new Button("X");
            removeBtn.setOnAction(_ -> {
                onRemove.accept(item);
                onNameChanged.run();
            });
            setGraphic(new HBox(6, nameField, typeBox, numField, targetField, actionsField, rangesField, removeBtn));
        }
    }
}