package org.roach.margia.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.DataDrivenMusicianRule;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.controller.rules.storage.RuleRepository;
import org.roach.margia.controller.rules.storage.RuleRepository.RuleSource;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.*;

/**
 * In-program JavaFX editor for creating, modifying, validating, and saving
 * YAML-backed {@link RuleDefinition}s.
 */
public class RuleEditorDialog extends Dialog<Void> {

    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private ListView<String> ruleListView;
    @FXML
    private TextField ruleNameField;
    @FXML
    private ComboBox<String> startingStateCombo;
    @FXML
    private TextArea yamlEditorArea;
    @FXML
    private Label statusLabel;

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab visualTab;
    @FXML
    private Tab graphTab;
    @FXML
    private Tab yamlTab;
    @FXML
    private BorderPane graphHostPane;

    @FXML
    private ListView<MutableProperty> propListView;
    @FXML
    private ListView<MutableState> stateListView;

    @FXML
    private Button saveBtn;
    @FXML
    private Button saveAsBtn;
    @FXML
    private Button deleteBtn;

    private final ObservableList<MutableProperty> propertyRows = FXCollections.observableArrayList();
    private final ObservableList<MutableState> stateRows = FXCollections.observableArrayList();

    private final List<Musician> activeMusicians;
    private StateGraphPane graphPane;
    private boolean syncingTabs = false;

    private static final List<String> SUPPORTED_STATE_TYPES = List.of("ALWAYS_TRANSITION", "DELAYED_TRANSITION",
            "PSEUDO_RANDOM", "DIE_ROLLING", "COUNTDOWN", "CONDITIONAL", "NEVER_TRANSITION", "NOTE_BASED");

    /**
     * @param owner           the owning window
     * @param activeMusicians all active musicians
     */
    public RuleEditorDialog(Window owner, List<Musician> activeMusicians) {
        this.activeMusicians = activeMusicians != null ? activeMusicians : List.of();
        initOwner(owner);
        setTitle("Rule Editor");
        setResizable(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/roach/margia/view/RuleEditorDialog.fxml"));
        loader.setRoot(getDialogPane());
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RuleEditorDialog.fxml", e);
        }

        setOnShown(_ -> {
            if (getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.setMaximized(true);
            }
            Platform.runLater(() -> mainSplitPane.setDividerPositions(0.12));
        });
    }

    @FXML
    private void initialize() {
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
        propListView.setItems(propertyRows);
        propListView.setCellFactory(_ -> new PropertyCell(propertyRows::remove));

        stateListView.setItems(stateRows);
        stateListView.setCellFactory(
                _ -> new StateCell(stateRows::remove, this::refreshStartingStateChoices, this::openActionsEditor));

        graphPane = new StateGraphPane();
        graphHostPane.setCenter(graphPane);

        // Two-way synchronization when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((_, oldTab, newTab) -> {
            if (syncingTabs) {
                return;
            }
            syncingTabs = true;
            try {
                if (oldTab == yamlTab) {
                    RuleDefinition def = RuleRepository.getInstance().fromYamlString(yamlEditorArea.getText());
                    populateFormFromDefinition(def);
                }

                if (newTab == yamlTab) {
                    RuleDefinition def = buildDefinitionFromForm();
                    yamlEditorArea.setText(RuleRepository.getInstance().toYamlString(def));
                    setStatus("YAML updated", false);
                } else if (newTab == graphTab) {
                    graphPane.redraw();
                    Platform.runLater(graphPane::zoomToFit);
                    setStatus("Double-click to add states; Shift+drag between ovals to add transitions", false);
                } else if (newTab == visualTab) {
                    stateRows.setAll(new ArrayList<>(stateRows));
                    setStatus("Visual builder updated", false);
                }
            } catch (Exception ex) {
                setStatus("Sync error: " + ex.getMessage(), true);
            } finally {
                syncingTabs = false;
            }
        });

        refreshRuleList();
        if (!ruleListView.getItems().isEmpty()) {
            ruleListView.getSelectionModel().selectFirst();
        } else {
            loadDefinitionIntoEditor(createStarterTemplate());
            deleteBtn.setDisable(true);
        }
    }

    @FXML
    private void onNewBlankClicked() {
        ruleListView.getSelectionModel().clearSelection();
        loadDefinitionIntoEditor(createStarterTemplate());
        saveBtn.setDisable(false);
        deleteBtn.setDisable(true);
        ruleNameField.setEditable(true);
        setStatus("Editing new rule (unsaved)", false);
    }

    @FXML
    private void onAddStateClicked() {
        stateRows.add(new MutableState(nextUniqueStateName(), "ALWAYS_TRANSITION", "16", "",
                List.of(new ActionDef("PLAY_CHORD", Map.of())), ""));
        refreshStartingStateChoices();
    }

    @FXML
    private void onDeleteSelectedClicked() {
        String selected = ruleListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            RuleRepository.getInstance().deleteExternalRule(selected);
            refreshRuleList();
            setStatus("Deleted '" + selected + "'", false);
        } catch (IOException ex) {
            setStatus("Delete failed: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void onImportClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Rule YAML");
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("YAML Rule Files (*.yaml, *.yml)", "*.yaml", "*.yml"));

        File chosen = chooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (chosen == null) {
            return;
        }

        try {
            String content = Files.readString(chosen.toPath());
            RuleDefinition imported = RuleRepository.getInstance().fromYamlString(content);
            if (imported.name() == null || imported.name().isBlank()) {
                setStatus("Import failed: YAML file is missing a 'name' property.", true);
                return;
            }

            if (RuleRepository.getInstance().getRuleSource(imported.name()) == RuleSource.TEMPLATE_YAML) {
                imported = new RuleDefinition(imported.name() + "_imported", imported.startingState(),
                        imported.properties(), imported.states());
            }

            DataDrivenMusicianRule.validate(imported);
            RuleRepository.getInstance().saveRule(imported);

            refreshRuleList();
            ruleListView.getSelectionModel().select(imported.name());
            loadDefinitionIntoEditor(imported);
            ruleNameField.setEditable(true);
            saveBtn.setDisable(false);
            deleteBtn.setDisable(false);

            setStatus("Imported rule '" + imported.name() + "' from " + chosen.getName(), false);
        } catch (Exception ex) {
            setStatus("Import failed: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void onExportClicked() {
        try {
            RuleDefinition def = getActiveDefinition();
            DataDrivenMusicianRule.validate(def);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Rule YAML");
            String defaultFileName = def.name().replaceAll("[^a-zA-Z0-9._-]", "_") + ".yaml";
            chooser.setInitialFileName(defaultFileName);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML Rule Files (*.yaml)", "*.yaml"));

            File target = chooser.showSaveDialog(getDialogPane().getScene().getWindow());
            if (target == null) {
                return;
            }

            String yamlContent = RuleRepository.getInstance().toYamlString(def);
            Files.writeString(target.toPath(), yamlContent);
            setStatus("Exported '" + def.name() + "' to " + target.getAbsolutePath(), false);
        } catch (Exception ex) {
            setStatus("Export failed: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void onAddPropertyClicked() {
        propertyRows.add(new MutableProperty("INT", "newParam", "New Param:", "10", "0", "100", "1", ""));
    }

    @FXML
    private void onValidateClicked() {
        try {
            RuleDefinition def = getActiveDefinition();
            DataDrivenMusicianRule.validate(def);
            setStatus("Valid rule definition: '" + def.name() + "'", false);
        } catch (Exception ex) {
            setStatus("Validation failed: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void onSaveClicked() {
        try {
            RuleDefinition def = getActiveDefinition();
            if (RuleRepository.getInstance().getRuleSource(def.name()) == RuleSource.TEMPLATE_YAML) {
                setStatus("Cannot overwrite template '" + def.name() + "'. Use 'Save As...' instead.", true);
                return;
            }

            DataDrivenMusicianRule.validate(def);
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

    @FXML
    private void onSaveAsClicked() {
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
                onSaveClicked();
            } catch (Exception ex) {
                setStatus("Save As failed: " + ex.getMessage(), true);
            }
        });
    }

    private String nextUniqueStateName() {
        Set<String> existing = stateRows.stream().map(s -> s.name).collect(Collectors.toSet());
        int idx = stateRows.size() + 1;
        while (existing.contains("state_" + idx)) {
            idx++;
        }
        return "state_" + idx;
    }

    private void loadDefinitionIntoEditor(RuleDefinition def) {
        populateFormFromDefinition(def);
        try {
            yamlEditorArea.setText(RuleRepository.getInstance().toYamlString(def));
        } catch (IOException e) {
            yamlEditorArea.setText("# Error serializing YAML: " + e.getMessage());
        }
        if (graphPane != null) {
            graphPane.redraw();
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
            List<ActionDef> actions = stateDef.actions() != null ? new ArrayList<>(stateDef.actions()) : List.of();
            MutableState ms = switch (stateDef) {
            case StateDef.AlwaysTransition a ->
                new MutableState(name, "ALWAYS_TRANSITION", "", a.nextState(), actions, "");
            case StateDef.DelayedTransition d ->
                new MutableState(name, "DELAYED_TRANSITION", String.valueOf(d.delay()), d.wrappedState(), actions, "");
            case StateDef.Countdown c ->
                new MutableState(name, "COUNTDOWN", String.valueOf(c.ticks()), c.nextState(), actions, "");
            case StateDef.PseudoRandom pr -> {
                String ranges = pr.rangeTransitions().stream().map(rt -> rt.min() + "-" + rt.max() + "->" + rt.target())
                        .collect(Collectors.joining("; "));
                yield new MutableState(name, "PSEUDO_RANDOM", String.valueOf(pr.maxRoll()), "", actions, ranges);
            }
            case StateDef.DieRolling dr -> {
                String rangesText = dr.rangeTransitions().stream()
                        .map(rt -> rt.min() + "-" + rt.max() + "->" + rt.target()).collect(Collectors.joining("; "));
                yield new MutableState(name, "DIE_ROLLING", dr.dice(), "", actions, rangesText);
            }
            case StateDef.Conditional cond -> {
                String number = cond.condition() + ":" + cond.threshold();
                String targetState = cond.trueState() + (cond.falseState() != null ? "," + cond.falseState() : "");
                yield new MutableState(name, "CONDITIONAL", number, targetState, actions, "");
            }
            case StateDef.NeverTransition _ -> new MutableState(name, "NEVER_TRANSITION", "", "", actions, "");
            case StateDef.NoteBased nb -> {
                String rangesText = nb.noteTransitions().stream().map(rt -> rt.notes() + "->" + rt.target())
                        .collect(Collectors.joining("; "));
                yield new MutableState(name, "NOTE_BASED", "", "", actions, rangesText);
            }
            };
            if (stateDef.x() != null && stateDef.y() != null) {
                ms.x = stateDef.x();
                ms.y = stateDef.y();
            }
            stateRows.add(ms);
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
            String stateName = ms.name.trim();
            if (stateName.isEmpty()) {
                throw new IllegalArgumentException("State names cannot be empty.");
            }
            if (states.put(stateName, ms.toStateDef()) != null) {
                throw new IllegalArgumentException("Duplicate state name: '" + stateName + "'");
            }
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
        if (current != null && names.contains(current)) {
            startingStateCombo.setValue(current);
        }
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }

    private static String serializeActionsSummary(List<ActionDef> actions) {
        if (actions == null || actions.isEmpty()) {
            return "(none)";
        }
        return actions.stream().map(a -> {
            if (a.params() == null || a.params().isEmpty()) {
                return a.type();
            }
            String joinedParams = a.params().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
            return a.type() + "(" + joinedParams + ")";
        }).collect(Collectors.joining("; "));
    }

    private static RuleDefinition createStarterTemplate() {
        return new RuleDefinition("customEchoRule", "playAndHold",
                List.of(new PropertyDef.IntProp("holdTicks", "Hold Ticks:", 8, 1, 128, 1)),
                Map.of("play",
                        new StateDef.AlwaysTransition(List.of(new ActionDef("PLAY_CHORD", Map.of())), "playAndHold"),
                        "playAndHold", new StateDef.DelayedTransition("$holdTicks", "play")));
    }

    private void openActionsEditor(MutableState st, Runnable onUpdated) {
        List<String> propKeys = propertyRows.stream().map(p -> p.key).filter(k -> k != null && !k.isBlank()).toList();
        StateActionsDialog dialog = new StateActionsDialog(getDialogPane().getScene().getWindow(), st.name, st.actions,
                propKeys);
        dialog.showAndWait().ifPresent(updatedActions -> {
            st.actions = new ArrayList<>(updatedActions);
            onUpdated.run();
        });
    }

    // --- Interactive State Graph ---
    private class StateGraphPane extends BorderPane {
        private static final double RX = 68;
        private static final double RY = 34;
        private static final double MIN_ZOOM = 0.35;
        private static final double MAX_ZOOM = 2.5;

        private final Group arrowLayer = new Group();
        private final Group nodeLayer = new Group();
        private final Pane canvas = new Pane(arrowLayer, nodeLayer);
        private final Group zoomGroup = new Group(canvas);
        private final ScrollPane scrollPane = new ScrollPane(zoomGroup);
        private final ToggleButton connectModeBtn = new ToggleButton("Draw Transition Mode (or Shift+Drag)");
        private final Label zoomLabel = new Label("100%");
        private final HBox inspectorBar = new HBox(8);

        private double zoomFactor = 1.0;
        private MutableState dragSourceForArrow = null;
        private Line previewLine = null;
        private MutableState selectedState = null;

        StateGraphPane() {
            canvas.setStyle("-fx-background-color: #fafafa; -fx-border-color: #cccccc;");
            canvas.setPrefSize(1400, 900);

            Button zoomOutBtn = new Button("-");
            zoomOutBtn.setOnAction(_ -> setZoom(zoomFactor - 0.15));

            Button zoomResetBtn = new Button("100%");
            zoomResetBtn.setOnAction(_ -> setZoom(1.0));

            Button zoomInBtn = new Button("+");
            zoomInBtn.setOnAction(_ -> setZoom(zoomFactor + 0.15));

            Button zoomFitBtn = new Button("Fit");
            zoomFitBtn.setTooltip(new Tooltip("Zoom and center to fit all states"));
            zoomFitBtn.setOnAction(_ -> zoomToFit());

            Button autoLayoutBtn = new Button("Auto-Arrange");
            autoLayoutBtn.setOnAction(_ -> {
                autoLayoutStates(true);
                redraw();
            });

            Label hint = new Label("Double-click: add state | Drag: move | Shift+Drag: draw arrow | Ctrl+Scroll: zoom");
            hint.setStyle("-fx-text-fill: #555555;");

            HBox topBar = new HBox(8, connectModeBtn, new Separator(Orientation.VERTICAL), new Label("Zoom:"),
                    zoomOutBtn, zoomLabel, zoomInBtn, zoomResetBtn, zoomFitBtn, new Separator(Orientation.VERTICAL),
                    autoLayoutBtn, hint);
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setPadding(new Insets(6));
            topBar.setStyle("-fx-background-color: #eeeeee;");
            setTop(topBar);

            scrollPane.setPannable(true);
            setCenter(scrollPane);

            scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
                if (e.isControlDown()) {
                    double delta = e.getDeltaY() > 0 ? 0.1 : -0.1;
                    setZoom(zoomFactor + delta);
                    e.consume();
                }
            });

            inspectorBar.setAlignment(Pos.CENTER_LEFT);
            inspectorBar.setPadding(new Insets(6));
            inspectorBar.setStyle("-fx-background-color: #eeeeee;");
            setBottom(inspectorBar);

            canvas.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && e.getTarget() == canvas) {
                    promptNewStateAt(e.getX(), e.getY());
                }
            });

            canvas.setOnMouseDragged(e -> {
                if (dragSourceForArrow != null && previewLine != null) {
                    previewLine.setEndX(e.getX());
                    previewLine.setEndY(e.getY());
                }
            });

            canvas.setOnMouseReleased(e -> {
                if (dragSourceForArrow != null) {
                    MutableState target = findStateAt(e.getX(), e.getY());
                    if (target != null) {
                        connectStates(dragSourceForArrow, target);
                    }
                    arrowLayer.getChildren().remove(previewLine);
                    previewLine = null;
                    dragSourceForArrow = null;
                    redraw();
                }
            });
        }

        private void setZoom(double newZoom) {
            zoomFactor = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
            canvas.setScaleX(zoomFactor);
            canvas.setScaleY(zoomFactor);
            zoomLabel.setText(Math.round(zoomFactor * 100) + "%");
        }

        void zoomToFit() {
            if (stateRows.isEmpty()) {
                setZoom(1.0);
                return;
            }
            autoLayoutStates(false);

            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (MutableState st : stateRows) {
                minX = Math.min(minX, st.x - RX - 40);
                minY = Math.min(minY, st.y - RY - 50);
                maxX = Math.max(maxX, st.x + RX + 40);
                maxY = Math.max(maxY, st.y + RY + 40);
            }

            double graphWidth = Math.max(100, maxX - minX);
            double graphHeight = Math.max(100, maxY - minY);

            double viewWidth = scrollPane.getViewportBounds().getWidth();
            double viewHeight = scrollPane.getViewportBounds().getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) {
                return;
            }

            double fitScale = Math.min(viewWidth / graphWidth, viewHeight / graphHeight);
            setZoom(Math.min(1.0, fitScale));

            double centerX = (minX + maxX) / 2.0;
            double centerY = (minY + maxY) / 2.0;
            double canvasW = canvas.getPrefWidth();
            double canvasH = canvas.getPrefHeight();

            double visibleCanvasW = viewWidth / zoomFactor;
            double visibleCanvasH = viewHeight / zoomFactor;

            if (canvasW > visibleCanvasW) {
                double hVal = (centerX - visibleCanvasW / 2.0) / (canvasW - visibleCanvasW);
                scrollPane.setHvalue(Math.max(0.0, Math.min(1.0, hVal)));
            }
            if (canvasH > visibleCanvasH) {
                double vVal = (centerY - visibleCanvasH / 2.0) / (canvasH - visibleCanvasH);
                scrollPane.setVvalue(Math.max(0.0, Math.min(1.0, vVal)));
            }
        }

        void redraw() {
            autoLayoutStates(false);
            redrawArrowsOnly();

            nodeLayer.getChildren().clear();
            for (MutableState st : stateRows) {
                nodeLayer.getChildren().add(createStateNode(st));
            }
            updateInspector();
        }

        private void redrawArrowsOnly() {
            arrowLayer.getChildren().clear();
            Map<String, MutableState> byName = new LinkedHashMap<>();
            for (MutableState s : stateRows) {
                byName.put(s.name, s);
            }

            for (MutableState src : stateRows) {
                switch (src.type) {
                case "PSEUDO_RANDOM", "DIE_ROLLING" -> {
                    for (RangeTransition rt : MutableState.parseRanges(src.rangesText)) {
                        MutableState dst = byName.get(rt.target());
                        if (dst != null) {
                            drawArrow(src, dst, rt.min() + "-" + rt.max(), () -> {
                                removeRangeTransition(src, rt);
                                redraw();
                            });
                        }
                    }
                }
                case "NOTE_BASED" -> {
                    for (NoteTransition nt : MutableState.parseNoteTransitions(src.rangesText)) {
                        MutableState dst = byName.get(nt.target());
                        if (dst != null) {
                            drawArrow(src, dst, nt.notes().toString(), () -> {
                                removeNoteTransition(src, nt);
                                redraw();
                            });
                        }
                    }
                }
                case "CONDITIONAL" -> {
                    if (src.targetState != null && !src.targetState.isBlank()) {
                        List<String> targets = List.of(src.targetState.split(",", 2));
                        String trueName = targets.getFirst().trim();
                        MutableState dstTrue = byName.get(trueName);
                        if (dstTrue != null) {
                            drawArrow(src, dstTrue, "true", () -> {
                                String rem = targets.size() > 1 ? "," + targets.get(1).trim() : "";
                                src.targetState = rem;
                                redraw();
                            });
                        }
                        if (targets.size() > 1 && !targets.get(1).trim().isEmpty()) {
                            String falseName = targets.get(1).trim();
                            MutableState dstFalse = byName.get(falseName);
                            if (dstFalse != null) {
                                drawArrow(src, dstFalse, "false", () -> {
                                    src.targetState = trueName;
                                    redraw();
                                });
                            }
                        }
                    }
                }
                case "NEVER_TRANSITION" -> {
                    // Terminal state: no outgoing transitions
                }
                default -> {
                    if (src.targetState != null && !src.targetState.isBlank()) {
                        MutableState dst = byName.get(src.targetState.trim());
                        if (dst != null) {
                            String lbl = switch (src.type) {
                            case "DELAYED_TRANSITION" -> "delay " + src.numberOrPlaceholder;
                            case "COUNTDOWN" -> "count " + src.numberOrPlaceholder;
                            default -> "next";
                            };
                            drawArrow(src, dst, lbl, () -> {
                                src.targetState = "";
                                redraw();
                            });
                        }
                    }
                }
                }
            }
        }

        private StackPane createStateNode(MutableState st) {
            boolean isStart = Objects.equals(st.name, startingStateCombo.getValue());
            boolean isSelected = (st == selectedState);
            Ellipse oval = new Ellipse(RX, RY);
            oval.setFill(isSelected ? Color.web("#e3f2fd") : Color.WHITE);
            oval.setStroke(isStart ? Color.web("#2e7d32") : (isSelected ? Color.web("#1565c0") : Color.web("#37474f")));
            oval.setStrokeWidth(isStart || isSelected ? 3.0 : 1.6);

            Label nameLbl = new Label(st.name);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            Label typeLbl = new Label(abbreviateType(st.type));
            typeLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #555555;");
            VBox textBox = new VBox(1, nameLbl, typeLbl);
            textBox.setAlignment(Pos.CENTER);
            textBox.setMouseTransparent(true);

            StackPane node = new StackPane(oval, textBox);
            node.setLayoutX(st.x - RX);
            node.setLayoutY(st.y - RY);
            final double[] dragOffset = new double[2];

            node.setOnMousePressed(e -> {
                if (selectedState != st) {
                    selectedState = st;
                    redraw();
                    return;
                }
                updateInspector();
                Point2D localPt = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                if (e.isShiftDown() || connectModeBtn.isSelected()) {
                    dragSourceForArrow = st;
                    previewLine = new Line(st.x, st.y, localPt.getX(), localPt.getY());
                    previewLine.getStrokeDashArray().addAll(6.0, 4.0);
                    previewLine.setStroke(Color.web("#1565c0"));
                    previewLine.setStrokeWidth(2.0);
                    previewLine.setMouseTransparent(true);
                    arrowLayer.getChildren().add(previewLine);
                } else {
                    dragOffset[0] = localPt.getX() - st.x;
                    dragOffset[1] = localPt.getY() - st.y;
                }
                e.consume();
            });

            node.setOnMouseDragged(e -> {
                Point2D localPt = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                if (dragSourceForArrow != null && previewLine != null) {
                    previewLine.setEndX(localPt.getX());
                    previewLine.setEndY(localPt.getY());
                } else {
                    st.x = Math.max(RX + 10, localPt.getX() - dragOffset[0]);
                    st.y = Math.max(RY + 10, localPt.getY() - dragOffset[1]);
                    node.setLayoutX(st.x - RX);
                    node.setLayoutY(st.y - RY);
                    redrawArrowsOnly();
                }
                e.consume();
            });

            node.setOnMouseReleased(e -> {
                if (dragSourceForArrow != null) {
                    Point2D localPt = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                    MutableState target = findStateAt(localPt.getX(), localPt.getY());
                    if (target != null) {
                        connectStates(dragSourceForArrow, target);
                    }
                    arrowLayer.getChildren().remove(previewLine);
                    previewLine = null;
                    dragSourceForArrow = null;
                    redraw();
                }
                e.consume();
            });

            return node;
        }

        private void drawArrow(MutableState src, MutableState dst, String labelText, Runnable onDeleteTransition) {
            if (src == dst) {
                Arc loop = new Arc(src.x, src.y - RY, 24, 24, -30, 240);
                loop.setFill(null);
                loop.setStroke(Color.web("#455a64"));
                loop.setStrokeWidth(1.6);
                Label lbl = new Label(labelText);
                lbl.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-font-size: 10px; -fx-padding: 1 3;");
                lbl.setLayoutX(src.x - 18);
                lbl.setLayoutY(src.y - RY - 36);
                lbl.setTooltip(new Tooltip("Click to remove transition"));
                lbl.setOnMouseClicked(_ -> onDeleteTransition.run());
                arrowLayer.getChildren().addAll(loop, lbl);
                return;
            }
            double dx = dst.x - src.x;
            double dy = dst.y - src.y;
            double angle = Math.atan2(dy, dx);
            double perpOffset = 8.0;
            double ox = -Math.sin(angle) * perpOffset;
            double oy = Math.cos(angle) * perpOffset;
            double startX = src.x + ox + RX * Math.cos(angle);
            double startY = src.y + oy + RY * Math.sin(angle);
            double endX = dst.x + ox - RX * Math.cos(angle);
            double endY = dst.y + oy - RY * Math.sin(angle);

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(Color.web("#455a64"));
            line.setStrokeWidth(1.6);

            double arrowLen = 10.0;
            double arrowAngle = Math.toRadians(25);
            Polygon head = new Polygon(endX, endY, endX - arrowLen * Math.cos(angle - arrowAngle),
                    endY - arrowLen * Math.sin(angle - arrowAngle), endX - arrowLen * Math.cos(angle + arrowAngle),
                    endY - arrowLen * Math.sin(angle + arrowAngle));
            head.setFill(Color.web("#455a64"));

            Label lbl = new Label(labelText);
            lbl.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.88); -fx-font-size: 10px; -fx-padding: 1 4; -fx-border-color: #cfd8dc;");
            lbl.setLayoutX((startX + endX) / 2.0 - 16);
            lbl.setLayoutY((startY + endY) / 2.0 - 10);
            lbl.setTooltip(new Tooltip("Click to remove this transition"));
            lbl.setOnMouseClicked(_ -> onDeleteTransition.run());
            arrowLayer.getChildren().addAll(line, head, lbl);
        }

        private void connectStates(MutableState src, MutableState dst) {
            switch (src.type) {
            case "PSEUDO_RANDOM", "DIE_ROLLING" -> {
                TextInputDialog dialog = new TextInputDialog("1-5");
                dialog.initOwner(getDialogPane().getScene().getWindow());
                dialog.setTitle("Range Transition");
                dialog.setHeaderText("Transition from '" + src.name + "' to '" + dst.name + "'");
                dialog.setContentText("Roll range (min-max):");
                dialog.showAndWait().ifPresent(range -> {
                    String entry = range.trim() + "->" + dst.name;
                    src.rangesText = (src.rangesText == null || src.rangesText.isBlank()) ? entry
                            : src.rangesText + "; " + entry;
                    setStatus("Added transition " + entry, false);
                });
            }
            case "NOTE_BASED" -> {
                TextInputDialog dialog = new TextInputDialog("60, 64, 67");
                dialog.initOwner(getDialogPane().getScene().getWindow());
                dialog.setTitle("Note-Based Transition");
                dialog.setHeaderText("Transition from '" + src.name + "' to '" + dst.name + "'");
                dialog.setContentText("MIDI note numbers (comma-separated):");
                dialog.showAndWait().ifPresent(notes -> {
                    String entry = "[" + notes.trim() + "]->" + dst.name;
                    src.rangesText = (src.rangesText == null || src.rangesText.isBlank()) ? entry
                            : src.rangesText + "; " + entry;
                    setStatus("Added transition " + entry, false);
                });
            }
            case "CONDITIONAL" -> {
                if (src.targetState == null || src.targetState.isBlank()) {
                    src.targetState = dst.name;
                    setStatus("Set 'true' branch of '" + src.name + "' -> '" + dst.name + "'", false);
                } else {
                    String trueTarget = src.targetState.split(",", 2)[0].trim();
                    src.targetState = trueTarget + "," + dst.name;
                    setStatus("Set 'false' branch of '" + src.name + "' -> '" + dst.name + "'", false);
                }
            }
            case "NEVER_TRANSITION" ->
                setStatus("'" + src.name + "' is NEVER_TRANSITION and cannot have outgoing arrows.", true);
            default -> {
                src.targetState = dst.name;
                setStatus("Connected '" + src.name + "' -> '" + dst.name + "'", false);
            }
            }
        }

        private void removeRangeTransition(MutableState src, RangeTransition toRemove) {
            List<RangeTransition> remaining = new ArrayList<>(MutableState.parseRanges(src.rangesText));
            remaining.remove(toRemove);
            src.rangesText = remaining.stream().map(rt -> rt.min() + "-" + rt.max() + "->" + rt.target())
                    .collect(Collectors.joining("; "));
        }

        private void removeNoteTransition(MutableState src, NoteTransition toRemove) {
            List<NoteTransition> remaining = new ArrayList<>(MutableState.parseNoteTransitions(src.rangesText));
            remaining.remove(toRemove);
            src.rangesText = remaining.stream().map(nt -> nt.notes() + "->" + nt.target())
                    .collect(Collectors.joining("; "));
        }

        private void promptNewStateAt(double x, double y) {
            TextInputDialog dialog = new TextInputDialog(nextUniqueStateName());
            dialog.initOwner(getDialogPane().getScene().getWindow());
            dialog.setTitle("Add State");
            dialog.setHeaderText("Create a new state at (" + (int) x + ", " + (int) y + ")");
            dialog.setContentText("State name:");
            dialog.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) {
                    return;
                }
                if (stateRows.stream().anyMatch(s -> trimmed.equals(s.name))) {
                    setStatus("State '" + trimmed + "' already exists.", true);
                    return;
                }
                MutableState st = new MutableState(trimmed, "ALWAYS_TRANSITION", "16", "",
                        List.of(new ActionDef("PLAY_CHORD", Map.of())), "");
                st.x = x;
                st.y = y;
                stateRows.add(st);
                selectedState = st;
                refreshStartingStateChoices();
                if (startingStateCombo.getValue() == null) {
                    startingStateCombo.setValue(trimmed);
                }
                redraw();
            });
        }

        private void autoLayoutStates(boolean force) {
            int count = stateRows.size();
            if (count == 0) {
                return;
            }
            double centerX = 430;
            double centerY = 260;
            double radiusX = Math.max(180, count * 24.0);
            double radiusY = Math.max(140, count * 16.0);
            for (int i = 0; i < count; i++) {
                MutableState st = stateRows.get(i);
                if (force || st.x < 0 || st.y < 0) {
                    double theta = (2 * Math.PI * i / count) - (Math.PI / 2);
                    st.x = centerX + radiusX * Math.cos(theta);
                    st.y = centerY + radiusY * Math.sin(theta);
                }
            }
        }

        private MutableState findStateAt(double x, double y) {
            for (int i = stateRows.size() - 1; i >= 0; i--) {
                MutableState st = stateRows.get(i);
                double dx = (x - st.x) / RX;
                double dy = (y - st.y) / RY;
                if (dx * dx + dy * dy <= 1.0) {
                    return st;
                }
            }
            return null;
        }

        private void updateInspector() {
            inspectorBar.getChildren().clear();
            if (selectedState == null || !stateRows.contains(selectedState)) {
                inspectorBar.getChildren()
                        .add(new Label("Click a state oval to edit its type, delay/roll, or actions."));
                return;
            }
            MutableState st = selectedState;
            TextField nameField = new TextField(st.name);
            nameField.setPrefWidth(115);
            nameField.setOnAction(_ -> {
                String oldName = st.name;
                String newName = nameField.getText().trim();
                if (!newName.isEmpty() && !newName.equals(oldName)) {
                    renameStateReferences(oldName, newName);
                    refreshStartingStateChoices();
                    redraw();
                }
            });

            ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(SUPPORTED_STATE_TYPES));
            typeBox.setValue(st.type);
            typeBox.setOnAction(_ -> {
                st.type = typeBox.getValue();
                redraw();
            });

            TextField numField = new TextField(st.numberOrPlaceholder);
            numField.setPromptText("ticks/roll/$prop");
            numField.setPrefWidth(95);
            numField.textProperty().addListener((_, _, nv) -> st.numberOrPlaceholder = nv);

            Button editActionsBtn = new Button("Actions (" + st.actions.size() + ")...");
            editActionsBtn.setTooltip(new Tooltip(serializeActionsSummary(st.actions)));
            editActionsBtn.setOnAction(_ -> openActionsEditor(st, () -> {
                editActionsBtn.setText("Actions (" + st.actions.size() + ")...");
                editActionsBtn.setTooltip(new Tooltip(serializeActionsSummary(st.actions)));
            }));

            Button setStartBtn = new Button("Set as Start");
            setStartBtn.setOnAction(_ -> {
                startingStateCombo.setValue(st.name);
                redraw();
            });

            Button delStateBtn = new Button("Delete State");
            delStateBtn.setOnAction(_ -> {
                stateRows.remove(st);
                selectedState = null;
                refreshStartingStateChoices();
                redraw();
            });

            inspectorBar.getChildren().addAll(new Label("State:"), nameField, typeBox, new Label("Param:"), numField,
                    editActionsBtn, setStartBtn, delStateBtn);
        }

        private void renameStateReferences(String oldName, String newName) {
            for (MutableState s : stateRows) {
                if (oldName.equals(s.name)) {
                    s.name = newName;
                }
                if ("CONDITIONAL".equals(s.type) && s.targetState != null && !s.targetState.isBlank()) {
                    s.targetState = Arrays.stream(s.targetState.split(",", 2)).map(String::trim)
                            .map(t -> oldName.equals(t) ? newName : t).collect(Collectors.joining(","));
                } else if (oldName.equals(s.targetState)) {
                    s.targetState = newName;
                }
                if (s.rangesText != null && !s.rangesText.isBlank()) {
                    s.rangesText = Arrays.stream(s.rangesText.split(";")).map(String::trim)
                            .filter(part -> !part.isEmpty()).map(part -> {
                                String[] arrow = part.split("->", 2);
                                if (arrow.length == 2 && oldName.equals(arrow[1].trim())) {
                                    return arrow[0].trim() + "->" + newName;
                                }
                                return part;
                            }).collect(Collectors.joining("; "));
                }
            }
            if (oldName.equals(startingStateCombo.getValue())) {
                startingStateCombo.setValue(newName);
            }
        }

        private String abbreviateType(String type) {
            return switch (type) {
            case "ALWAYS_TRANSITION" -> "Always";
            case "DELAYED_TRANSITION" -> "Delayed";
            case "PSEUDO_RANDOM" -> "Random";
            case "DIE_ROLLING" -> "Dice";
            case "NOTE_BASED" -> "Notes";
            case "NEVER_TRANSITION" -> "Terminal";
            case "COUNTDOWN" -> "Countdown";
            default -> type;
            };
        }
    }

    // --- Mutable Row Models & ListCells ---
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
        String name, type, numberOrPlaceholder, targetState, rangesText;
        List<ActionDef> actions;
        double x = -1;
        double y = -1;

        MutableState(String name, String type, String numberOrPlaceholder, String targetState, List<ActionDef> actions,
                String rangesText) {
            this.name = name;
            this.type = type;
            this.numberOrPlaceholder = numberOrPlaceholder;
            this.targetState = targetState;
            this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
            this.rangesText = rangesText;
        }

        StateDef toStateDef() {
            Object numericArg = numberOrPlaceholder.startsWith("$") ? numberOrPlaceholder
                    : (numberOrPlaceholder.isBlank() ? 0 : Integer.parseInt(numberOrPlaceholder.trim()));
            List<ActionDef> immutableActions = List.copyOf(actions);
            Double savedX = (x >= 0) ? Math.round(x * 10.0) / 10.0 : null;
            Double savedY = (y >= 0) ? Math.round(y * 10.0) / 10.0 : null;

            return switch (type) {
            case "ALWAYS_TRANSITION" ->
                new StateDef.AlwaysTransition(immutableActions, targetState.trim(), savedX, savedY);
            case "DELAYED_TRANSITION" -> new StateDef.DelayedTransition(numericArg, targetState.trim(), savedX, savedY);
            case "COUNTDOWN" -> new StateDef.Countdown(numericArg, targetState.trim(), savedX, savedY);
            case "PSEUDO_RANDOM" ->
                new StateDef.PseudoRandom(numericArg, immutableActions, parseRanges(rangesText), savedX, savedY);
            case "NEVER_TRANSITION" -> new StateDef.NeverTransition(immutableActions, savedX, savedY);
            case "DIE_ROLLING" -> new StateDef.DieRolling(numberOrPlaceholder.trim(), immutableActions,
                    parseRanges(rangesText), savedX, savedY);
            case "NOTE_BASED" ->
                new StateDef.NoteBased(immutableActions, parseNoteTransitions(rangesText), savedX, savedY);
            case "CONDITIONAL" -> {
                List<String> condParts = List.of(numberOrPlaceholder.split(":", 2));
                String condName = condParts.getFirst().trim();
                Object thresh = 0;
                if (condParts.size() > 1 && !condParts.get(1).trim().isEmpty()) {
                    String rawThresh = condParts.get(1).trim();
                    thresh = rawThresh.startsWith("$") ? rawThresh : Integer.parseInt(rawThresh);
                }
                List<String> targets = List.of(targetState.split(",", 2));
                String onTrue = targets.getFirst().trim();
                String onFalse = (targets.size() > 1 && !targets.get(1).trim().isEmpty()) ? targets.get(1).trim()
                        : null;
                yield new StateDef.Conditional(condName, thresh, immutableActions, onTrue, onFalse, savedX, savedY);
            }
            default -> throw new IllegalArgumentException("Unsupported state type: " + type);
            };
        }

        private static List<RangeTransition> parseRanges(String text) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            List<RangeTransition> ranges = new ArrayList<>();
            for (String part : text.split(";")) {
                String[] arrow = part.trim().split("->");
                if (arrow.length == 2) {
                    String[] bounds = arrow[0].trim().split("-");
                    if (bounds.length == 2) {
                        ranges.add(new RangeTransition(Integer.parseInt(bounds[0].trim()),
                                Integer.parseInt(bounds[1].trim()), arrow[1].trim()));
                    }
                }
            }
            return ranges;
        }

        private static List<NoteTransition> parseNoteTransitions(String text) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            List<NoteTransition> list = new ArrayList<>();
            for (String part : text.split(";")) {
                List<String> arrow = List.of(part.trim().split("->"));
                if (arrow.size() == 2) {
                    String noteSpec = arrow.getFirst().trim().replace("[", "").replace("]", "");
                    Set<Integer> notes = Arrays.stream(noteSpec.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                            .map(Integer::parseInt).collect(Collectors.toCollection(LinkedHashSet::new));
                    list.add(new NoteTransition(notes, arrow.get(1).trim()));
                }
            }
            return list;
        }
    }

    @FunctionalInterface
    private interface ActionsEditorLauncher {
        void open(MutableState state, Runnable onUpdated);
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
            typeBox.setPrefWidth(115);
            typeBox.setValue(item.type);
            typeBox.setOnAction(_ -> item.type = typeBox.getValue());
            TextField keyField = field(item.key, "key", 110, v -> item.key = v);
            TextField labelField = field(item.label, "Label:", 130, v -> item.label = v);
            TextField defField = field(item.defaultVal, "default", 70, v -> item.defaultVal = v);
            TextField minField = field(item.min, "min", 55, v -> item.min = v);
            TextField maxField = field(item.max, "max", 55, v -> item.max = v);
            TextField stepField = field(item.step, "step", 55, v -> item.step = v);
            TextField listField = field(item.listValues, "a,b,c (STRING_LIST)", 140, v -> item.listValues = v);
            Button removeBtn = new Button("X");
            removeBtn.setOnAction(_ -> onRemove.accept(item));
            setGraphic(new HBox(6, typeBox, keyField, labelField, defField, minField, maxField, stepField, listField,
                    removeBtn));
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
        private final ActionsEditorLauncher onEditActions;

        StateCell(Consumer<MutableState> onRemove, Runnable onNameChanged, ActionsEditorLauncher onEditActions) {
            this.onRemove = onRemove;
            this.onNameChanged = onNameChanged;
            this.onEditActions = onEditActions;
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

            ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(SUPPORTED_STATE_TYPES));
            typeBox.setValue(item.type);
            typeBox.setPrefWidth(165);
            typeBox.setOnAction(_ -> item.type = typeBox.getValue());

            TextField numField = new TextField(item.numberOrPlaceholder);
            numField.setPromptText("ticks / maxRoll / $prop");
            numField.setPrefWidth(135);
            numField.textProperty().addListener((_, _, newV) -> item.numberOrPlaceholder = newV);

            TextField targetField = new TextField(item.targetState);
            targetField.setPromptText("next / wrapped state");
            targetField.setPrefWidth(130);
            targetField.textProperty().addListener((_, _, newV) -> item.targetState = newV);

            Label actionsLabel = new Label(serializeActionsSummary(item.actions));
            actionsLabel.setPrefWidth(130);
            actionsLabel.setTooltip(new Tooltip(serializeActionsSummary(item.actions)));

            Button editActionsBtn = new Button("Edit...");
            editActionsBtn.setPrefWidth(54);
            editActionsBtn.setOnAction(_ -> onEditActions.open(item, () -> {
                String summary = serializeActionsSummary(item.actions);
                actionsLabel.setText(summary);
                actionsLabel.setTooltip(new Tooltip(summary));
            }));

            HBox actionsBox = new HBox(4, actionsLabel, editActionsBtn);
            actionsBox.setAlignment(Pos.CENTER_LEFT);
            actionsBox.setPrefWidth(190);

            TextField rangesField = new TextField(item.rangesText);
            rangesField.setPromptText("Ranges (1-3->stateA; 4-6->stateB)");
            rangesField.setPrefWidth(190);
            rangesField.textProperty().addListener((_, _, newV) -> item.rangesText = newV);

            Button removeBtn = new Button("X");
            removeBtn.setOnAction(_ -> {
                onRemove.accept(item);
                onNameChanged.run();
            });

            setGraphic(new HBox(6, nameField, typeBox, numField, targetField, actionsBox, rangesField, removeBtn));
        }
    }
}