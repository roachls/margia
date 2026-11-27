package org.roach.margia.view;

import java.util.*;
import java.util.stream.Collectors;

import org.roach.margia.controller.rules.DataDrivenMusicianRule;
import org.roach.margia.controller.rules.DataDrivenMusicianRule.ActionParamSpec;
import org.roach.margia.controller.rules.storage.RuleDefinition.ActionDef;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Modal dialog for managing the ordered list of {@link ActionDef}s on a state
 * and editing each action's typed parameters through labeled form fields.
 */
public class StateActionsDialog extends Dialog<List<ActionDef>> {

    private static final class MutableAction {
        final String type;
        final Map<String, String> params = new LinkedHashMap<>();

        MutableAction(String type, Map<String, Object> initialParams) {
            this.type = type;
            // Seed with defaults from spec first, then overlay existing values
            for (ActionParamSpec spec : DataDrivenMusicianRule.getActionParamSpecs(type)) {
                params.put(spec.name(), spec.defaultValue());
            }
            if (initialParams != null) {
                initialParams.forEach((k, v) -> params.put(k, v != null ? String.valueOf(v) : ""));
            }
        }

        String paramsSummary() {
            String nonBlank = params.entrySet().stream().filter(e -> e.getValue() != null && !e.getValue().isBlank())
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
            return nonBlank.isEmpty() ? "(none)" : nonBlank;
        }

        ActionDef toActionDef() {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String raw = entry.getValue() != null ? entry.getValue().trim() : "";
                if (raw.isEmpty()) {
                    continue; // Omit blank optional parameters (like musicianId)
                }
                if (raw.startsWith("$")) {
                    converted.put(entry.getKey(), raw);
                } else {
                    try {
                        converted.put(entry.getKey(), Integer.parseInt(raw));
                    } catch (NumberFormatException e) {
                        converted.put(entry.getKey(), raw);
                    }
                }
            }
            return new ActionDef(type, converted);
        }
    }

    private final ObservableList<MutableAction> actionItems = FXCollections.observableArrayList();
    private final TableView<MutableAction> table = new TableView<>(actionItems);
    private final GridPane paramEditorGrid = new GridPane();
    private final Label paramHeaderLabel = new Label("Select an action above to configure its parameters.");
    private final List<String> availablePropertyKeys;

    /**
     * @param owner                 the owning window
     * @param stateName             name of the state being configured
     * @param existingActions       existing actions for the state
     * @param availablePropertyKeys keys for available properties
     */
    public StateActionsDialog(Window owner, String stateName, List<ActionDef> existingActions,
            List<String> availablePropertyKeys) {
        this.availablePropertyKeys = availablePropertyKeys != null ? availablePropertyKeys : List.of();
        initOwner(owner);
        setTitle("Configure Actions — State '" + stateName + "'");
        setResizable(true);

        if (existingActions != null) {
            existingActions.forEach(a -> actionItems.add(new MutableAction(a.type(), a.params())));
        }

        ComboBox<String> actionPicker = new ComboBox<>(
                FXCollections.observableArrayList(DataDrivenMusicianRule.supportedActionNames()));
        actionPicker.setPromptText("Choose action to add...");
        actionPicker.setPrefWidth(260);
        if (!actionPicker.getItems().isEmpty()) {
            actionPicker.getSelectionModel().selectFirst();
        }

        Button addBtn = new Button("+ Add Action");
        addBtn.setStyle("-fx-font-weight: bold;");
        addBtn.setOnAction(_ -> {
            String chosen = actionPicker.getValue();
            if (chosen != null && !chosen.isBlank()) {
                MutableAction added = new MutableAction(chosen, Map.of());
                actionItems.add(added);
                table.getSelectionModel().select(added);
            }
        });

        Button moveUpBtn = new Button("Move Up");
        moveUpBtn.setOnAction(_ -> moveSelected(-1));

        Button moveDownBtn = new Button("Move Down");
        moveDownBtn.setOnAction(_ -> moveSelected(1));

        HBox topControls = new HBox(8, new Label("Action:"), actionPicker, addBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL), moveUpBtn, moveDownBtn);
        topControls.setAlignment(Pos.CENTER_LEFT);

        configureTable();

        paramHeaderLabel.setStyle("-fx-font-weight: bold;");
        paramEditorGrid.setHgap(10);
        paramEditorGrid.setVgap(8);
        paramEditorGrid.setPadding(new Insets(8));
        paramEditorGrid.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #dddddd; -fx-border-radius: 4;");

        VBox paramBox = new VBox(6, paramHeaderLabel, paramEditorGrid);

        VBox root = new VBox(10, topControls, table, paramBox);
        root.setPadding(new Insets(12));
        root.setPrefSize(680, 460);
        VBox.setVgrow(table, Priority.ALWAYS);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (!actionItems.isEmpty()) {
            table.getSelectionModel().selectFirst();
        } else {
            rebuildParameterForm(null);
        }

        setResultConverter(
                btn -> btn == ButtonType.OK ? actionItems.stream().map(MutableAction::toActionDef).toList() : null);
    }

    private void configureTable() {
        TableColumn<MutableAction, String> orderCol = new TableColumn<>("#");
        orderCol.setPrefWidth(36);
        orderCol.setCellValueFactory(
                cell -> new SimpleStringProperty(String.valueOf(actionItems.indexOf(cell.getValue()) + 1)));

        TableColumn<MutableAction, String> typeCol = new TableColumn<>("Action Type");
        typeCol.setPrefWidth(240);
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type));

        TableColumn<MutableAction, String> paramsCol = new TableColumn<>("Configured Parameters");
        paramsCol.setPrefWidth(290);
        paramsCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().paramsSummary()));

        TableColumn<MutableAction, Void> removeCol = new TableColumn<>("");
        removeCol.setPrefWidth(75);
        removeCol.setCellFactory(_ -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.setOnAction(_ -> {
                    MutableAction item = getTableView().getItems().get(getIndex());
                    actionItems.remove(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        table.getColumns().setAll(List.of(orderCol, typeCol, paramsCol, removeCol));
        table.setPlaceholder(
                new Label("No actions assigned to this state yet. Pick an action above and click '+ Add Action'."));

        table.getSelectionModel().selectedItemProperty()
                .addListener((_, _, selected) -> rebuildParameterForm(selected));
    }

    private void rebuildParameterForm(MutableAction selected) {
        paramEditorGrid.getChildren().clear();
        if (selected == null) {
            paramHeaderLabel.setText("Parameters: (select an action row above)");
            return;
        }

        List<ActionParamSpec> specs = DataDrivenMusicianRule.getActionParamSpecs(selected.type);
        paramHeaderLabel.setText("Parameters for " + selected.type + ":");

        if (specs.isEmpty()) {
            paramEditorGrid.add(new Label("This action takes no parameters."), 0, 0);
            return;
        }

        int row = 0;
        for (ActionParamSpec spec : specs) {
            Label nameLbl = new Label(spec.name() + ":");
            nameLbl.setStyle("-fx-font-weight: bold;");

            TextField valueField = new TextField(selected.params.getOrDefault(spec.name(), spec.defaultValue()));
            valueField.setPrefWidth(150);
            valueField.textProperty().addListener((_, _, newVal) -> {
                selected.params.put(spec.name(), newVal);
                table.refresh();
            });

            Label descLbl = new Label(spec.description());
            descLbl.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

            paramEditorGrid.addRow(row++, nameLbl, valueField, descLbl);
        }

        if (!availablePropertyKeys.isEmpty()) {
            String propRefs = availablePropertyKeys.stream().filter(k -> k != null && !k.isBlank()).map(k -> "$" + k)
                    .collect(Collectors.joining(", "));
            if (!propRefs.isEmpty()) {
                Label hint = new Label("Available rule property placeholders: " + propRefs);
                hint.setStyle("-fx-text-fill: #1565c0; -fx-font-size: 11px;");
                paramEditorGrid.add(hint, 0, row, 3, 1);
            }
        }
    }

    private void moveSelected(int direction) {
        int idx = table.getSelectionModel().getSelectedIndex();
        int targetIdx = idx + direction;
        if (idx < 0 || targetIdx < 0 || targetIdx >= actionItems.size()) {
            return;
        }
        MutableAction moved = actionItems.remove(idx);
        actionItems.add(targetIdx, moved);
        table.getSelectionModel().select(targetIdx);
        table.refresh();
    }
}