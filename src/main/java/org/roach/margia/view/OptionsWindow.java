package org.roach.margia.view;

import java.beans.*;
import java.io.IOException;
import java.util.*;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

class OptionsWindow extends Stage implements PropertyChangeListener {
    private static final String HEIGHT_PROPERTY = "_height";
    private static final String WIDTH_PROPERTY = "_width";
    private static final String Y_PROPERTY = "_y";
    private static final String X_PROPERTY = "_x";
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";

    static final String MUSICIAN = "Musician ";

    // Injected directly from OptionsWindow.fxml
    @FXML
    private BorderPane borderPane;
    @FXML
    private UiOptionsPanel uiPanel;
    @FXML
    private MiscPanel miscPanel;
    @FXML
    private MidiPanel midiPanel;
    @FXML
    private MusicPanel globalMusicPanel;

    @FXML
    private TreeView<Object> tree;
    @FXML
    private TreeItem<Object> root;
    @FXML
    private TreeItem<Object> musiciansNode;
    @FXML
    private StackPane warningPanel;

    private final Map<Integer, TreeItem<Object>> musicianNodes = new HashMap<>();

    public static final String OPTIONS_AGENTS_SELECTED_PROPERTY = "options agents selected";
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    OptionsWindow(Window owner) {
        setTitle("Options");
        if (owner != null) {
            initOwner(owner);
        }

        setupPersistenceListeners();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionsWindow.fxml"));
        loader.setController(this);

        try {
            BorderPane loadedRoot = loader.load();
            setScene(new Scene(loadedRoot));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OptionsWindow.fxml", e);
        }

        restoreWindowBounds();

        if (Persistence.getInstance().getBoolean(OPTIONS_WINDOW_NAME + "_visible", false)) {
            show();
        }

        updateOptions();
        syncExistingMusicians();
    }

    @FXML
    private void initialize() {
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
                    borderPane.setCenter(warningPanel);
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
    }

    private void setupPersistenceListeners() {
        xProperty().addListener((_, _, newVal) -> saveBounds(X_PROPERTY, newVal));
        yProperty().addListener((_, _, newVal) -> saveBounds(Y_PROPERTY, newVal));
        widthProperty().addListener((_, _, newVal) -> saveBounds(WIDTH_PROPERTY, newVal));
        heightProperty().addListener((_, _, newVal) -> saveBounds(HEIGHT_PROPERTY, newVal));
    }

    private void saveBounds(String propertySuffix, Number value) {
        if (isShowing()) {
            Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + propertySuffix,
                    Integer.toString(value.intValue()));
        }
    }

    private void restoreWindowBounds() {
        var persistence = Persistence.getInstance();
        setX(persistence.getInt(OPTIONS_WINDOW_NAME + X_PROPERTY, 100));
        setY(persistence.getInt(OPTIONS_WINDOW_NAME + Y_PROPERTY, 100));
        setWidth(persistence.getInt(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY, 600));
        setHeight(persistence.getInt(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY, 400));
    }

    void syncExistingMusicians() {
        for (var musician : MusicianList.getInstance().getMusicians().values()) {
            var id = musician.getId();
            if (musicianNodes.containsKey(id)) {
                continue;
            }
            var compOptions = Options.getInstance().getUiOptions().getMusicianComponents().computeIfAbsent(id,
                    _ -> new MusicianComponentOptions());
            var newUiPanel = new MusicianUiPanel(id, compOptions);
            var newMusicianPanel = new MusicianMusicalOptionsPanel(musician.getOptions());
            var newRulePanel = new MusicianRuleOptionsPanel(id, musician.getOptions().getRuleOptions());
            var newOptionsPanel = new MusicianOptionsPanel(newUiPanel, newMusicianPanel, newRulePanel);

            var newNode = new TreeItem<Object>(newOptionsPanel);
            musiciansNode.getChildren().add(newNode);
            musicianNodes.put(id, newNode);
        }
        musiciansNode.setExpanded(true);
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
        Platform.runLater(() -> {
            if (AgentPanel.AGENT_ADDED_PROPERTY.equals(e.getPropertyName())) {
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

    static Label createLabelFor(String propertyName, Node target) {
        var spinnerLabel = new Label(propertyName.replace('_', ' '));
        spinnerLabel.setLabelFor(target);
        return spinnerLabel;
    }

    static Spinner<Integer> createSpinner(String propertyName, int defValue, int min, int max, int step) {
        var spinner = new Spinner<Integer>(min, max, defValue, step);
        spinner.setId(propertyName);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    static Spinner<Long> createSpinner(String propertyName, long defValue, long min, long max, long step) {
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
