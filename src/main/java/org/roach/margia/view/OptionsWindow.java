package org.roach.margia.view;

import java.beans.*;
import java.util.*;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

class OptionsWindow extends Stage implements PropertyChangeListener {
    private static final String HEIGHT_PROPERTY = "_height";
    private static final String WIDTH_PROPERTY = "_width";
    private static final String Y_PROPERTY = "_y";
    private static final String X_PROPERTY = "_x";
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";

    private static final String MUSICIAN = "Musician ";

    // Note: These sub-panels will eventually become JavaFX Nodes/Panes.
    // While migrating, if they are still JPanels, wrap them in a SwingNode!
    private UiOptionsPanel uiPanel;
    private MiscPanel miscPanel;
    private MidiPanel midiPanel;
    private MusicPanel globalMusicPanel;

    private TreeView<Object> tree;
    private TreeItem<Object> root;
    private TreeItem<Object> musiciansNode;
    private final Map<Integer, TreeItem<Object>> musicianNodes = new HashMap<>();

    public static final String OPTIONS_AGENTS_SELECTED_PROPERTY = "options agents selected";

    // Replaces JDialog's built-in Swing PropertyChangeSupport
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    OptionsWindow() {
        setTitle("Options");

        // Save window position and size via JavaFX Property Listeners
        xProperty().addListener((_, _, newVal) -> {
            if (isShowing()) {
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + X_PROPERTY,
                        Integer.toString(newVal.intValue()));
            }
        });
        yProperty().addListener((_, _, newVal) -> {
            if (isShowing()) {
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + Y_PROPERTY,
                        Integer.toString(newVal.intValue()));
            }
        });
        widthProperty().addListener((_, _, newVal) -> {
            if (isShowing()) {
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY,
                        Integer.toString(newVal.intValue()));
            }
        });
        heightProperty().addListener((_, _, newVal) -> {
            if (isShowing()) {
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY,
                        Integer.toString(newVal.intValue()));
            }
        });

        BorderPane mainLayout = createUi();
        Scene scene = new Scene(mainLayout);
        setScene(scene);

        var x = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + X_PROPERTY, 100);
        var y = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + Y_PROPERTY, 100);
        var w = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY, 600);
        var h = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY, 400);

        setX(x);
        setY(y);
        setWidth(w);
        setHeight(h);

        if (Persistence.getInstance().getBoolean(OPTIONS_WINDOW_NAME + "_visible", false)) {
            show();
        }

        updateOptions();
    }

    @SuppressWarnings("unchecked")
    private BorderPane createUi() {
        var borderPane = new BorderPane();

        root = new TreeItem<>("root");
        uiPanel = new UiOptionsPanel();
        var uiOptions = new TreeItem<Object>(uiPanel);
        midiPanel = new MidiPanel();
        var midiOptions = new TreeItem<Object>(midiPanel);
        globalMusicPanel = new MusicPanel();
        var musicOptions = new TreeItem<Object>(globalMusicPanel);
        miscPanel = new MiscPanel();
        var miscOptions = new TreeItem<Object>(miscPanel);
        musiciansNode = new TreeItem<>("Musicians");
        musiciansNode.setExpanded(true);

        root.getChildren().addAll(uiOptions, midiOptions, miscOptions, musicOptions, musiciansNode);

        tree = new TreeView<>(root);
     // Inside OptionsWindow.createUi(), right after creating `tree = new TreeView<>(root);`
        tree.setShowRoot(false);
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Force TreeView to render item.toString() as text instead of embedding the Node itself
        tree.setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        });
        tree.setPrefWidth(180);
        borderPane.setLeft(tree);

        tree.setShowRoot(false);
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Set initial center view
        borderPane.setCenter(uiPanel);

        // Listen for multi-selection changes in the JavaFX TreeView
        tree.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Object>>) _ -> {
            var selectedItems = tree.getSelectionModel().getSelectedItems();
            int selectedCount = selectedItems.size();

            if (selectedCount == 1) {
                var userObject = selectedItems.get(0).getValue();
                if (userObject instanceof Node node) {
                    borderPane.setCenter(node);
                }
            } else if (selectedCount > 1) {
                var firstObjectType = selectedItems.get(0).getValue().getClass();
                boolean areAllSameType = selectedItems.stream().map(TreeItem::getValue).map(Object::getClass)
                        .allMatch(firstObjectType::equals);

                if (!areAllSameType) {
                    borderPane.setCenter(new WarningPanel());
                } else {
                    setupMultipleSelection(selectedItems, borderPane);
                }
            }

            if (selectedCount > 0) {
                var musicianIdList = new ArrayList<Integer>();
                for (var item : selectedItems) {
                    var valueStr = item.getValue().toString();
                    if (valueStr != null && valueStr.startsWith(MUSICIAN)) {
                        var musicianId = Integer.parseInt(valueStr.substring(MUSICIAN.length()));
                        musicianIdList.add(musicianId);
                    }
                }
                pcs.firePropertyChange(OPTIONS_AGENTS_SELECTED_PROPERTY, Collections.emptyList(), musicianIdList);
            }
        });

        // TreeView in JavaFX has built-in scrolling, no JScrollPane needed
        tree.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        borderPane.setLeft(tree);

        return borderPane;
    }

    // Expose PropertyChangeListener registration so external Swing controllers
    // still work
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        // Ensure UI updates run on the JavaFX thread, even if triggered by a Swing EDT
        // event
        Platform.runLater(() -> {
            if (AgentPanel.AGENT_ADDED_PROPERTY.equals(e.getPropertyName())) {
                System.out.println("************* " + AgentPanel.AGENT_ADDED_PROPERTY);
                var mc = (MusicianComponent) e.getNewValue();
                var id = mc.getMusician().getId();
                var newUiPanel = new MusicianUiPanel(id, mc.getOptions());
                var newMusicianPanel = new MusicianMusicalOptionsPanel(mc.getMusician().getOptions());
                var newRulePanel = new MusicianRuleOptionsPanel(id, mc.getMusician().getOptions().getRuleOptions());
                var newOptionsPanel = new MusicianOptionsPanel(newUiPanel, newMusicianPanel, newRulePanel);

                var newNode = new TreeItem<Object>(newOptionsPanel);

                // JavaFX TreeItem uses a standard ObservableList; no TreeModel or repaint()
                // needed
                musiciansNode.getChildren().add(newNode);
                musiciansNode.setExpanded(true);
                musicianNodes.put(id, newNode);
            } else if (AgentPanel.AGENT_REMOVED_PROPERTY.equals(e.getPropertyName())) {
                var mc = (MusicianComponent) e.getNewValue();
                var id = mc.getMusician().getId();
                var nodeToRemove = musicianNodes.remove(id);

                if (nodeToRemove != null) {
                    musiciansNode.getChildren().remove(nodeToRemove);
                }

            } else if (AgentPanel.SELECTED_AGENT_PROPERTY.equals(e.getPropertyName())) {
                var selectionModel = tree.getSelectionModel();
                selectionModel.clearSelection();

                var selectedMusicians = (List<Musician>) e.getNewValue();
                var itemsToSelect = selectedMusicians.stream().map(Musician::getId).map(musicianNodes::get)
                        .filter(Objects::nonNull).toList();

                if (!itemsToSelect.isEmpty()) {
                    // Ensure the parent "Musicians" node is expanded so row indices resolve
                    // properly
                    musiciansNode.setExpanded(true);

                    for (var item : itemsToSelect) {
                        selectionModel.select(item);
                    }

                    int firstRowIndex = tree.getRow(itemsToSelect.get(0));
                    if (firstRowIndex >= 0) {
                        tree.scrollTo(firstRowIndex);
                    }
                }
            }
        });
    }

    private void setupMultipleSelection(List<TreeItem<Object>> selectedItems, BorderPane borderPane) {
        if (selectedItems.isEmpty()) {
            return;
        }
        var userObjects = selectedItems.stream().map(TreeItem::getValue).toList();

        if (userObjects.get(0) instanceof MusicianOptionsPanel) {
            var mups = userObjects.stream().map(MusicianOptionsPanel.class::cast).toList();
            setupMultipleMusicianUi(mups, borderPane);
        }
    }

    private void setupMultipleMusicianUi(List<MusicianOptionsPanel> mups, BorderPane borderPane) {
        var multipleEditor = new MusicianOptionsPanel(mups);
        borderPane.setCenter(multipleEditor);
    }

    // Replaces Swing's setVisible(boolean)
    public void setVisible(boolean visible) {
        if (visible) {
            show();
        } else {
            hide();
        }
        Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_visible", Boolean.toString(isShowing()));
    }

    private static class WarningPanel extends StackPane {
        WarningPanel() {
            var warning = new Label("All selected objects must be of same type");
            warning.setStyle("-fx-font-weight: bold;");
            getChildren().add(warning);
        }
    }

    private class UiOptionsPanel extends VBox {
        private final Spinner<Double> gravity;
        private final Spinner<Double> windSpeed;
        private final Spinner<Integer> edgeLength;
        private final CheckBox showIcons;
        private final CheckBox animateBackground;

        UiOptionsPanel() {
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            // Replaces BorderFactory.createTitledBorder("UI Options")
            var titleLabel = new Label("UI Options");
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            // Configure column widths (replaces LEFT_COLUMN_WEIGHT and RIGHT_COLUMN_WEIGHT)
            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            // Spinners & Listeners
            gravity = createSpinner("Gravity", Options.getInstance().getUiOptions().getGravity(), -20.0, 20.0, 0.1);
            gravity.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    Options.getInstance().getUiOptions().setGravity(newVal);
                }
            });
            var gravityLabel = createLabelFor("Gravitational Constant", gravity);

            windSpeed = createSpinner("Wind speed (clockwise)", Options.getInstance().getUiOptions().getWindSpeed(),
                    -200.0, 200.0, 0.1);
            windSpeed.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    Options.getInstance().getUiOptions().setWindSpeed(newVal);
                }
            });
            var windSpeedLabel = createLabelFor("WindSpeed", windSpeed);

            edgeLength = createSpinner(UiOptions.EDGE_LENGTH_PROPERTY,
                    Options.getInstance().getUiOptions().getEdgeLength(), 20, 300, 1);
            edgeLength.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    Options.getInstance().getUiOptions().setEdgeLength(newVal);
                }
            });
            var edgeLengthLabel = createLabelFor("Edge length", edgeLength);

            // CheckBoxes & Listeners
            showIcons = new CheckBox("Show icons");
            showIcons.setTooltip(new Tooltip("Show/hide additional information on musicians"));
            showIcons.setSelected(Options.getInstance().getUiOptions().isShowNumbers());
            showIcons.selectedProperty()
                    .addListener((_, _, newVal) -> Options.getInstance().getUiOptions().setShowNumbers(newVal));

            animateBackground = new CheckBox("Animate background");
            animateBackground.setTooltip(new Tooltip("Animate background"));
            animateBackground.setSelected(Options.getInstance().getUiOptions().isAnimateBackground());
            animateBackground.selectedProperty()
                    .addListener((_, _, newVal) -> Options.getInstance().getUiOptions().setAnimateBackground(newVal));

            // Populate GridPane (note: CheckBoxes align with your original c.gridx = 1
            // placement)
            int row = 0;
            grid.add(gravityLabel, 0, row);
            grid.add(gravity, 1, row++);

            grid.add(windSpeedLabel, 0, row);
            grid.add(windSpeed, 1, row++);

            grid.add(edgeLengthLabel, 0, row);
            grid.add(edgeLength, 1, row++);

            grid.add(showIcons, 1, row++);
            grid.add(animateBackground, 1, row++);

            getChildren().addAll(titleLabel, grid);
        }

        @Override
        public String toString() {
            return "UI";
        }
    }

    private class MiscPanel extends VBox {
        private final Spinner<Long> randomSeedSpinner;

        MiscPanel() {
            setId("Misc Options");
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            // Replaces BorderFactory.createTitledBorder("Miscellaneous Options")
            var titleLabel = new Label("Miscellaneous Options");
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            randomSeedSpinner = createSpinner("Random seed", Options.getInstance().getRandomSeed(), -Long.MAX_VALUE,
                    Long.MAX_VALUE, 1L);

            // Replaces JFormattedTextField.setColumns(15)
            randomSeedSpinner.getEditor().setPrefColumnCount(15);

            randomSeedSpinner.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    Options.getInstance().setRandomSeed(newVal);
                }
            });

            var randomSeedLabel = createLabelFor("Random seed", randomSeedSpinner);

            grid.add(randomSeedLabel, 0, 0);
            grid.add(randomSeedSpinner, 1, 0);

            getChildren().addAll(titleLabel, grid);
        }

        @Override
        public String toString() {
            return "Miscellaneous";
        }
    }

    private class MidiPanel extends VBox {
        private final CheckBox external;
        private final CheckBox sendMidiTimecode;
        private final CheckBox sendPanMessage;
        private final Spinner<Integer> panController;
        private final CheckBox panWithRelativeLocations;
        private final CheckBox sendVerticalPanMessage;
        private final Spinner<Integer> verticalPanController;
        private final CheckBox verticalPanWithRelativeLocations;

        MidiPanel() {
            setId("MIDI Options");
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            var titleLabel = new Label("MIDI Options");
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            var midiOpts = Options.getInstance().getMidiOptions();

            // Controls & Model Listeners
            external = new CheckBox("Use External MIDI");
            external.setSelected(midiOpts.isUsingExternalMidi());
            external.selectedProperty().addListener((_, _, newVal) -> midiOpts.setUsingExternalMidi(newVal));

            sendMidiTimecode = new CheckBox("Send MIDI Timecode");
            sendMidiTimecode.selectedProperty().addListener((_, _, newVal) -> midiOpts.setSendingMidiTimecode(newVal));

            sendPanMessage = new CheckBox("Send stereo panning");
            sendPanMessage.selectedProperty().addListener((_, _, newVal) -> midiOpts.setSendPanMessage(newVal));

            panController = createSpinner("Pan controller", MidiOptions.DEFAULT_PAN_CONTROLLER, 0, 127, 1);
            panController.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    midiOpts.setPanController(newVal);
                }
            });
            var panControllerLabel = createLabelFor("Pan controller", panController);

            panWithRelativeLocations = new CheckBox("Pan with relative locations");
            panWithRelativeLocations.setTooltip(new Tooltip(
                    "If set, stereo panning will be relative to the location of the left-most and right-most components; otherwise it will be relative to the screen"));
            panWithRelativeLocations.selectedProperty()
                    .addListener((_, _, newVal) -> midiOpts.setPanWithRelativeLocations(newVal));

            sendVerticalPanMessage = new CheckBox("Send vertical panning");
            sendVerticalPanMessage.selectedProperty()
                    .addListener((_, _, newVal) -> midiOpts.setSendVerticalPanMessage(newVal));

            verticalPanController = createSpinner("Vertical pan controller",
                    MidiOptions.DEFAULT_VERTICAL_PAN_CONTROLLER, 0, 127, 1);
            verticalPanController.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    midiOpts.setVerticalPanController(newVal);
                }
            });
            var verticalPanControllerLabel = createLabelFor("Vertical pan controller", verticalPanController);

            verticalPanWithRelativeLocations = new CheckBox(
                    "Vertical pan with relative locations");
            verticalPanWithRelativeLocations.setTooltip(new Tooltip(
                    "If set, vertical panning will be relative to the location of the top-most and bottom-most components; otherwise it will be relative to the screen"));
            verticalPanWithRelativeLocations.selectedProperty()
                    .addListener((_, _, newVal) -> midiOpts.setVerticalPanWithRelativeLocations(newVal));

            // Declarative Disable Bindings (replaces manual setEnabled cascades)
            var externalNotSelected = external.selectedProperty().not();
            var panDisabled = externalNotSelected.or(sendPanMessage.selectedProperty().not());
            var verticalPanDisabled = externalNotSelected.or(sendVerticalPanMessage.selectedProperty().not());

            sendMidiTimecode.disableProperty().bind(externalNotSelected);
            sendPanMessage.disableProperty().bind(externalNotSelected);
            sendVerticalPanMessage.disableProperty().bind(externalNotSelected);

            panController.disableProperty().bind(panDisabled);
            panControllerLabel.disableProperty().bind(panDisabled);
            panWithRelativeLocations.disableProperty().bind(panDisabled);

            verticalPanController.disableProperty().bind(verticalPanDisabled);
            verticalPanControllerLabel.disableProperty().bind(verticalPanDisabled);
            verticalPanWithRelativeLocations.disableProperty().bind(verticalPanDisabled);

            // Layout Grid (full-width checkboxes span 2 columns so long text isn't clipped
            // to 20%)
            int row = 0;
            grid.add(external, 0, row++, 2, 1);
            grid.add(sendMidiTimecode, 0, row++, 2, 1);
            grid.add(sendPanMessage, 0, row++, 2, 1);

            grid.add(panControllerLabel, 0, row);
            grid.add(panController, 1, row++);

            grid.add(panWithRelativeLocations, 0, row++, 2, 1);
            grid.add(sendVerticalPanMessage, 0, row++, 2, 1);

            grid.add(verticalPanControllerLabel, 0, row);
            grid.add(verticalPanController, 1, row++);

            grid.add(verticalPanWithRelativeLocations, 0, row++, 2, 1);

            getChildren().addAll(titleLabel, grid);
        }

        @Override
        public String toString() {
            return "MIDI";
        }
    }

    private class MusicPanel extends VBox {
        private static final String GLOBAL_MUSIC_OPTIONS = "Global Music Options";
        private final Spinner<Integer> maxQueueSize;

        MusicPanel() {
            setId(GLOBAL_MUSIC_OPTIONS);
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            // Replaces BorderFactory.createTitledBorder(GLOBAL_MUSIC_OPTIONS)
            var titleLabel = new Label(GLOBAL_MUSIC_OPTIONS);
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            maxQueueSize = createSpinner("Max Queue Size", MusicOptions.DEFAULT_MAX_QUEUE_SIZE, 1, 120, 1);
            maxQueueSize.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    Options.getInstance().getMusicOptions().setMaxQueueSize(newVal);
                }
            });
            var maxQueueSizeLabel = createLabelFor("Max Queue Size", maxQueueSize);

            grid.add(maxQueueSizeLabel, 0, 0);
            grid.add(maxQueueSize, 1, 0);

            getChildren().addAll(titleLabel, grid);
        }

        @Override
        public String toString() {
            return GLOBAL_MUSIC_OPTIONS;
        }
    }

    private class MusicianOptionsPanel extends BorderPane {
        private final int id;
        private final MusicianUiPanel musicianUiPanel;
        private final MusicianMusicalOptionsPanel musicPanel;
        private final MusicianRuleOptionsPanel rulePanel;

        MusicianOptionsPanel(MusicianUiPanel uiPanel, MusicianMusicalOptionsPanel musicPanel,
                MusicianRuleOptionsPanel rulePanel) {
            this.id = musicPanel.id;
            this.musicianUiPanel = uiPanel;
            this.musicPanel = musicPanel;
            this.rulePanel = rulePanel;
            initTabs();
        }

        public MusicianOptionsPanel(List<MusicianOptionsPanel> mups) {
            this.id = -1;
            this.musicianUiPanel = new MusicianUiPanel(mups.stream().map(m -> m.musicianUiPanel).toList());
            this.musicPanel = new MusicianMusicalOptionsPanel(mups.stream().map(m -> m.musicPanel).toList());
            this.rulePanel = new MusicianRuleOptionsPanel(mups.stream().map(m -> m.rulePanel).toList());
            initTabs();
        }

        private void initTabs() {
            var tabbedPane = new TabPane();
            tabbedPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            var uiTab = new Tab("UI", musicianUiPanel);
            var musicTab = new Tab("Music", musicPanel);
            var ruleTab = new Tab("Rules", rulePanel);

            tabbedPane.getTabs().addAll(uiTab, musicTab, ruleTab);
            setCenter(tabbedPane);
        }

        @Override
        public String toString() {
            return MUSICIAN + id;
        }
    }

    private static class MusicianUiPanel extends VBox {
        private final MusicianComponentOptions options;
        private final int id;
        private final Spinner<Integer> radius;

        MusicianUiPanel(int id, MusicianComponentOptions options) {
            this.id = id;
            this.options = options;
            setId("UI");

            var grid = initContainer(MUSICIAN + id + " UI Options");

            radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY, options.getRadius(), 1, 50, 1);
            radius.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    options.setRadius(newVal);
                }
            });
            var radiusLabel = createLabelFor("Radius", radius);

            grid.add(radiusLabel, 0, 0);
            grid.add(radius, 1, 0);
        }

        // Called for multiple edits
        MusicianUiPanel(List<MusicianUiPanel> others) {
            this.id = -1; // not used
            this.options = new MusicianComponentOptions();
            setId("UI");

            var idList = others.stream().map(p -> p.id).toList();
            var optionsList = others.stream().map(p -> p.options).toList();
            var firstRadius = optionsList.get(0).getRadius();
            var allRadiiSame = optionsList.stream().map(MusicianComponentOptions::getRadius)
                    .allMatch(r -> firstRadius == r);

            if (allRadiiSame) {
                options.setRadius(firstRadius);
            }

            var grid = initContainer(MUSICIAN + idList + " UI Options");

            radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY, options.getRadius(), 1, 50, 1);
            if (!allRadiiSame) {
                // Highlight mismatched values across selected musicians
                radius.setStyle("-fx-base: #ffcccc;");
                radius.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
            }

            radius.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    // Clear highlight once the user sets a unified value
                    radius.setStyle("");
                    radius.getEditor().setStyle("");
                    others.forEach(o -> o.radius.getValueFactory().setValue(newVal));
                }
            });

            var radiusLabel = createLabelFor("Radius", radius);
            grid.add(radiusLabel, 0, 0);
            grid.add(radius, 1, 0);
        }

        private GridPane initContainer(String title) {
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            var titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            getChildren().addAll(titleLabel, grid);
            return grid;
        }

        @Override
        public String toString() {
            return "UI";
        }
    }

    private class MusicianMusicalOptionsPanel extends VBox {
        private static final String LOW_NOTE = "Low note";
        private static final String HIGH_NOTE = "High note";
        private final int id;
        private final MusicianOptions options;
        private final ComboBox<String> key;
        private final Spinner<Integer> rangeLow;
        private final Spinner<Integer> rangeHi;
        private Spinner<Integer> channel;
        private ComboBox<String> device;

        @Override
        public String toString() {
            return "Music";
        }

        MusicianMusicalOptionsPanel(MusicianOptions options) {
            this.id = options.getId();
            this.options = options;
            setId("Musical");

            var grid = initContainer(MUSICIAN + id + " musical options");

            // Key ComboBox (editable)
            key = new ComboBox<>();
            key.getItems().add("");
            key.getItems().addAll(Key.BUILTIN_KEYS.keySet());
            key.setEditable(true);
            key.setMaxWidth(Double.MAX_VALUE);
            key.setValue(options.getKeyName());
            key.valueProperty().addListener((_, _, newVal) -> options.setKeyName(newVal));
            var keyLabel = createLabelFor("Key", key);

            // Note Range & MIDI Channel Spinners
            rangeLow = createSpinner(LOW_NOTE, options.getRange().low(), 0, 127, 1);
            rangeLow.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    options.setRange(options.getRange().withLow(newVal));
                }
            });
            var rangeLowLabel = createLabelFor(LOW_NOTE, rangeLow);

            rangeHi = createSpinner(HIGH_NOTE, options.getRange().high(), 0, 127, 1);
            rangeHi.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    options.setRange(options.getRange().withHigh(newVal));
                }
            });
            var rangeHiLabel = createLabelFor(HIGH_NOTE, rangeHi);

            channel = createSpinner("MIDI channel", options.getChannel() + 1, 1, 16, 1);
            channel.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    options.setChannel(newVal - 1);
                }
            });
            var channelLabel = createLabelFor("MIDI Channel", channel);

            // MIDI Device ComboBox
            var availableDevices = new TreeSet<String>();
            availableDevices.add("");
            availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
            device = new ComboBox<>();
            device.getItems().addAll(availableDevices);
            device.setMaxWidth(Double.MAX_VALUE);
            device.setValue(options.getDeviceName());
            device.valueProperty().addListener((_, _, newVal) -> options.setDeviceName(newVal));
            var deviceLabel = createLabelFor("MIDI Device", device);

            // Populate GridPane
            int row = 0;
            grid.add(keyLabel, 0, row);
            grid.add(key, 1, row++);

            grid.add(rangeLowLabel, 0, row);
            grid.add(rangeLow, 1, row++);

            grid.add(rangeHiLabel, 0, row);
            grid.add(rangeHi, 1, row++);

            grid.add(channelLabel, 0, row);
            grid.add(channel, 1, row++);

            grid.add(deviceLabel, 0, row);
            grid.add(device, 1, row++);
        }

        private GridPane initContainer(String title) {
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            var titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            getChildren().addAll(titleLabel, grid);
            return grid;
        }

        MusicianMusicalOptionsPanel(List<MusicianMusicalOptionsPanel> others) {
            this.id = -1; // unused
            this.options = new MusicianOptions();
            setId("Musical");

            var idList = others.stream().map(p -> p.id).toList();
            var optionsList = others.stream().map(p -> p.options).toList();

            var firstKeyName = optionsList.get(0).getKeyName();
            var allKeysSame = optionsList.stream().map(MusicianOptions::getKeyName).allMatch(firstKeyName::equals);
            options.setKeyName(allKeysSame ? firstKeyName : "");

            var firstLow = optionsList.get(0).getRange().low();
            var allLowsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::low)
                    .allMatch(l -> firstLow == l);
            if (allLowsSame) {
                options.setRange(options.getRange().withLow(firstLow));
            }

            var firstHigh = optionsList.get(0).getRange().high();
            var allHighsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::high)
                    .allMatch(h -> firstHigh == h);
            if (allHighsSame) {
                options.setRange(options.getRange().withHigh(firstHigh));
            }

            var firstChannel = optionsList.get(0).getChannel();
            var allChannelsSame = optionsList.stream().map(MusicianOptions::getChannel)
                    .allMatch(c -> firstChannel == c);
            if (allChannelsSame) {
                options.setChannel(firstChannel);
            }

            var firstDeviceName = optionsList.get(0).getDeviceName();
            var allDevicesNamesSame = optionsList.stream().map(MusicianOptions::getDeviceName)
                    .allMatch(firstDeviceName::equals);
            options.setDeviceName(allDevicesNamesSame ? firstDeviceName : "");

            var grid = initContainer(MUSICIAN + idList + " musical options");

            // Key ComboBox
            key = new ComboBox<>();
            key.getItems().add("");
            key.getItems().addAll(Key.BUILTIN_KEYS.keySet());
            key.setEditable(true);
            key.setMaxWidth(Double.MAX_VALUE);
            if (!allKeysSame) {
                key.setStyle("-fx-base: #ffcccc;");
                key.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
            }
            key.setValue(options.getKeyName());
            key.valueProperty().addListener((_, _, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    return;
                }
                options.setKeyName(newVal);
                key.setStyle("");
                key.getEditor().setStyle("");
                others.forEach(o -> o.key.setValue(newVal));
            });
            var keyLabel = createLabelFor("Key", key);

            // Low Note Spinner
            rangeLow = createSpinner(LOW_NOTE, options.getRange().low(), 0, 127, 1);
            if (!allLowsSame) {
                rangeLow.setStyle("-fx-base: #ffcccc;");
                rangeLow.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
            }
            rangeLow.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    rangeLow.setStyle("");
                    rangeLow.getEditor().setStyle("");
                    options.setRange(options.getRange().withLow(newVal));
                    others.forEach(o -> o.rangeLow.getValueFactory().setValue(newVal));
                }
            });
            var rangeLowLabel = createLabelFor(LOW_NOTE, rangeLow);

            // High Note Spinner
            rangeHi = createSpinner(HIGH_NOTE, options.getRange().high(), 0, 127, 1);
            if (!allHighsSame) {
                rangeHi.setStyle("-fx-base: #ffcccc;");
                rangeHi.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
            }
            rangeHi.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    rangeHi.setStyle("");
                    rangeHi.getEditor().setStyle("");
                    options.setRange(options.getRange().withHigh(newVal));
                    others.forEach(o -> o.rangeHi.getValueFactory().setValue(newVal));
                }
            });
            var rangeHiLabel = createLabelFor(HIGH_NOTE, rangeHi);

            // MIDI Channel Spinner
            channel = createSpinner("MIDI channel", options.getChannel() + 1, 1, 16, 1);
            if (!allChannelsSame) {
                channel.setStyle("-fx-base: #ffcccc;");
                channel.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
            }
            channel.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    channel.setStyle("");
                    channel.getEditor().setStyle("");
                    options.setChannel(newVal - 1);
                    others.forEach(o -> o.channel.getValueFactory().setValue(newVal));
                }
            });
            var channelLabel = createLabelFor("MIDI Channel", channel);

            // MIDI Device ComboBox
            var availableDevices = new TreeSet<String>();
            availableDevices.add("");
            availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
            device = new ComboBox<>();
            device.getItems().addAll(availableDevices);
            device.setMaxWidth(Double.MAX_VALUE);
            if (!allDevicesNamesSame) {
                device.setStyle("-fx-base: #ffcccc;");
            }
            device.setValue(options.getDeviceName());
            device.valueProperty().addListener((_, _, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    return;
                }
                device.setStyle("");
                options.setDeviceName(newVal);
                others.forEach(o -> o.device.setValue(newVal));
            });
            var deviceLabel = createLabelFor("MIDI Device", device);

            // Populate GridPane
            int row = 0;
            grid.add(keyLabel, 0, row);
            grid.add(key, 1, row++);

            grid.add(rangeLowLabel, 0, row);
            grid.add(rangeLow, 1, row++);

            grid.add(rangeHiLabel, 0, row);
            grid.add(rangeHi, 1, row++);

            grid.add(channelLabel, 0, row);
            grid.add(channel, 1, row++);

            grid.add(deviceLabel, 0, row);
            grid.add(device, 1, row++);
        }
    }

    private class MusicianRuleOptionsPanel extends VBox {
        private final Map<String, MusicianRule> availableRules;
        private final List<String> ruleNames;
        private final int id;
        private final RuleOptions options;
        private final ComboBox<String> rule;

        @Override
        public String toString() {
            return "Rule";
        }

        public MusicianRuleOptionsPanel(int id, RuleOptions options) {
            this.id = id;
            this.options = options;
            setId("Rule");

            var grid = initContainer(MUSICIAN + id + " rule options");

            availableRules = new HashMap<>();
            ruleNames = new ArrayList<>();
            ServiceLoader.load(MusicianRule.class).forEach(r -> {
                availableRules.put(r.getName(), r);
                ruleNames.add(r.getName());
            });

            rule = new ComboBox<>();
            rule.getItems().addAll(ruleNames);
            rule.setMaxWidth(Double.MAX_VALUE);
            rule.setValue(options.getName());
            rule.valueProperty().addListener((_, _, newVal) -> {
                if (newVal != null) {
                    options.setName(newVal);
                }
            });

            var ruleLabel = createLabelFor("Rule", rule);

            grid.add(ruleLabel, 0, 0);
            grid.add(rule, 1, 0);
        }

        private GridPane initContainer(String title) {
            setSpacing(8);
            setPadding(new Insets(10));
            setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 4;");

            var titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold;");

            var grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(6);

            var leftCol = new ColumnConstraints();
            leftCol.setPercentWidth(20);
            var rightCol = new ColumnConstraints();
            rightCol.setPercentWidth(80);
            grid.getColumnConstraints().addAll(leftCol, rightCol);

            getChildren().addAll(titleLabel, grid);
            return grid;
        }

        // Multiple selection
        MusicianRuleOptionsPanel(List<MusicianRuleOptionsPanel> others) {
            this.id = -1; // unused
            this.options = new RuleOptions();
            setId("Rule");

            availableRules = new HashMap<>();
            ruleNames = new ArrayList<>();
            ServiceLoader.load(MusicianRule.class).forEach(r -> {
                availableRules.put(r.getName(), r);
                ruleNames.add(r.getName());
            });

            var idList = others.stream().map(p -> p.id).toList();
            var grid = initContainer(MUSICIAN + idList + " rule options");

            var optionsList = others.stream().map(p -> p.options).toList();
            var firstName = optionsList.get(0).getName();
            var allNamesSame = optionsList.stream().map(RuleOptions::getName)
                    .allMatch(name -> Objects.equals(firstName, name));

            if (allNamesSame) {
                options.setName(firstName == null ? "" : firstName);
            } else {
                options.setName("");
            }

            rule = new ComboBox<>();
            rule.getItems().addAll(ruleNames);
            rule.setMaxWidth(Double.MAX_VALUE);

            if (!allNamesSame) {
                rule.setStyle("-fx-base: #ffcccc;");
            }

            rule.setValue(options.getName());
            rule.valueProperty().addListener((_, _, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    return;
                }
                rule.setStyle("");
                options.setName(newVal);
                others.forEach(o -> o.rule.setValue(newVal));
            });

            var ruleLabel = createLabelFor("Rule", rule);

            grid.add(ruleLabel, 0, 0);
            grid.add(rule, 1, 0);
        }

    }

    private static Label createLabelFor(String propertyName, Node target) {
        var spinnerLabel = new Label(propertyName.replace('_', ' '));
        spinnerLabel.setLabelFor(target);
        return spinnerLabel;
    }

    private static Spinner<Double> createSpinner(String propertyName, double defValue, double min,
            double max, double step) {
        var spinner = new Spinner<Double>(min, max, defValue, step);
        spinner.setId(propertyName);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private static Spinner<Integer> createSpinner(String propertyName, int defValue, int min,
            int max, int step) {
        var spinner = new Spinner<Integer>(min, max, defValue, step);
        spinner.setId(propertyName);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private static Spinner<Long> createSpinner(String propertyName, long defValue, long min,
            long max, long step) {
        var valueFactory = new LongSpinnerValueFactory(min, max, defValue, step);
        var spinner = new Spinner<Long>(valueFactory);
        spinner.setId(propertyName);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    void updateOptions() {
        Platform.runLater(() -> {
            var options = Options.getInstance();

            uiPanel.gravity.getValueFactory().setValue(options.getUiOptions().getGravity());
            uiPanel.showIcons.setSelected(options.getUiOptions().isShowNumbers());
            uiPanel.edgeLength.getValueFactory().setValue(options.getUiOptions().getEdgeLength());
            uiPanel.windSpeed.getValueFactory().setValue(options.getUiOptions().getWindSpeed());
            uiPanel.animateBackground.setSelected(options.getUiOptions().isAnimateBackground());

            miscPanel.randomSeedSpinner.getValueFactory().setValue(options.getRandomSeed());

            midiPanel.external.setSelected(options.getMidiOptions().isUsingExternalMidi());
            midiPanel.sendMidiTimecode.setSelected(options.getMidiOptions().isSendingMidiTimecode());
            midiPanel.sendPanMessage.setSelected(options.getMidiOptions().isSendPanMessage());
            midiPanel.panController.getValueFactory().setValue(options.getMidiOptions().getPanController());
            midiPanel.panWithRelativeLocations.setSelected(options.getMidiOptions().isPanWithRelativeLocations());
            midiPanel.sendVerticalPanMessage.setSelected(options.getMidiOptions().isSendVerticalPanMessage());
            midiPanel.verticalPanController.getValueFactory()
                    .setValue(options.getMidiOptions().getVerticalPanController());
            midiPanel.verticalPanWithRelativeLocations
                    .setSelected(options.getMidiOptions().isVerticalPanWithRelativeLocations());

            globalMusicPanel.maxQueueSize.getValueFactory().setValue(options.getMusicOptions().getMaxQueueSize());
        });
    }

    private static class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
        private final long min;
        private final long max;
        private final long amountToStepBy;

        LongSpinnerValueFactory(long min, long max, long initialValue, long amountToStepBy) {
            this.min = min;
            this.max = max;
            this.amountToStepBy = amountToStepBy;

            setConverter(new StringConverter<>() {
                @Override
                public String toString(Long value) {
                    return value == null ? "" : value.toString();
                }

                @Override
                public Long fromString(String text) {
                    if (text == null || text.isBlank()) {
                        return initialValue;
                    }
                    try {
                        return Math.clamp(Long.parseLong(text.trim()), min, max);
                    } catch (NumberFormatException e) {
                        return getValue() != null ? getValue() : initialValue;
                    }
                }
            });

            setValue(Math.clamp(initialValue, min, max));
        }

        @Override
        public void decrement(int steps) {
            long current = getValue() != null ? getValue() : min;
            setValue(Math.clamp(current - (steps * amountToStepBy), min, max));
        }

        @Override
        public void increment(int steps) {
            long current = getValue() != null ? getValue() : min;
            setValue(Math.clamp(current + (steps * amountToStepBy), min, max));
        }
    }

}
