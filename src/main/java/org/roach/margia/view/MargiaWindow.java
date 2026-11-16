package org.roach.margia.view;

import static org.roach.margia.view.Icons.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;

import org.roach.margia.command.CommandCaretaker;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.TimingSource;
import org.roach.margia.model.Key;
import org.roach.margia.storage.Options;
import org.roach.margia.util.DieRoller;
import org.roach.margia.util.MargiaFileUtils;
import org.roach.margia.view.AgentPanel.EditMode;
import org.roach.margia.view.AgentPanel.FanDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The main program window
 */
@SuppressWarnings({ "java:S1948" })
public class MargiaWindow extends Stage {
    private static final String MARGIA_EXTENSION = ".margia";
    private static final String ABOUT_MESSAGE = """
            <html>
            <h1>MARGIA</h1>

            <h3>The <u>M</u>usic <u>a</u>nd <u>R</u>hythm <u>G</u>enerating <u>I</u>ntelligent <u>A</u>gents</h3>

            <h3>By Stevie Roach</h3>
            <a href="mailto:roachls@yahoo.com">roachls@yahoo.com</a>
            <br>
            <a href="https://github.com">https://github.com</a>
            </html>
            """;
    private static final String KEYBOARD_SHORTCUTS = """
            <html>
            <table>
            <tr><td>Ctrl+C</td><td>copy selected musician</td></tr>
            <tr><td>Ctrl+Shift+C</td><td>copy settings of one musician</td></tr>
            <tr><td>Ctrl+V</td><td>paste copied musicians</td></tr>
            <tr><td>Ctrl+Shift+V</td><td>paste copied settings into selected musicians</td></tr>
            <tr><td>Ctrl+S</td><td>save</td></tr>
            <tr><td>Ctrl+O</td><td>open</td></tr>
            <tr><td>Ctrl+A</td><td>select all musicians</td></tr>
            <tr><td>Ctrl+M</td><td>Toggle muting of selected musicians</td></tr>
            <tr><td>Ctrl+L</td><td>Toggle whether selected musicians are listening to peers</td></tr>
            <tr><td>Del</td><td>Delete selected musicians</td></tr>
            <tr><td>Esc</td><td>Deselect all musicians</td></tr>
            <tr><td>F11</td><td>Toggle full-screen</td></tr>
            <tr><td>Ctrl+Spacebar</td><td>Play/pause</td></tr>
            </table>
            </html>
            """;
    private static final Logger LOGGER = LoggerFactory.getLogger(MargiaWindow.class);

    @FXML
    private BorderPane rootLayout;
    @FXML
    private VBox topContainer;
    @FXML
    private MenuBar menubar;
    @FXML
    private ToolBar toolbar;
    @FXML
    private AgentPanel agentPanel;

    @FXML
    private Menu openRecents;
    @FXML
    private Menu helpMenu;

    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem saveAsMenuItem;
    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem copyMenuItem;
    @FXML
    private MenuItem pasteMenuItem;
    @FXML
    private MenuItem copyParamsMenuItem;
    @FXML
    private MenuItem pasteParamsMenuItem;
    @FXML
    private MenuItem selectAllMenuItem;
    @FXML
    private MenuItem deselectAllMenuItem;
    @FXML
    private MenuItem selectConnectedMenuItem;
    @FXML
    private MenuItem muteMenuItem;
    @FXML
    private MenuItem unmuteMenuItem;
    @FXML
    private MenuItem lockMenuItem;
    @FXML
    private MenuItem unlockMenuItem;
    @FXML
    private MenuItem lockAllMenuItem;
    @FXML
    private MenuItem unlockAllMenuItem;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private MenuItem connectMenuItem;
    @FXML
    private MenuItem disconnectMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private MenuItem keyboardShortcutMenuItem;

    @FXML
    private Button selectAllBtn;
    @FXML
    private Button deselectAllBtn;
    @FXML
    private Button copyBtn;
    @FXML
    private Button pasteBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button muteBtn;
    @FXML
    private Button unmuteBtn;
    @FXML
    private Button listeningBtn;
    @FXML
    private Button notListeningBtn;
    @FXML
    private Button lockBtn;
    @FXML
    private Button unlockBtn;
    @FXML
    private Button lockAllBtn;
    @FXML
    private Button unlockAllBtn;
    @FXML
    private Button connectBtn;
    @FXML
    private Button disconnectBtn;
    @FXML
    private Button selectConnectedBtn;

    @FXML
    private ToggleButton selectModeBtn;
    @FXML
    private ToggleButton addModeBtn;
    @FXML
    private ToggleButton moveModeBtn;
    @FXML
    private ToggleButton connectModeBtn;
    @FXML
    private ToggleGroup modeGroup;
    @FXML
    private ToggleButton showUiOptions;

    @FXML
    private HBox addPanel;
    @FXML
    private Button addBtn;
    @FXML
    private Button addDropdownBtn;
    @FXML
    private ContextMenu addModeMenu;
    @FXML
    private MenuItem addMultipleMenuItem;
    @FXML
    private MenuItem addCircleMenuItem;
    @FXML
    private MenuItem addGridMenuItem;
    @FXML
    private MenuItem addFanMenuItem;

    private AddMode currentAddMode = AddMode.MULTIPLE;
    private OptionsWindow optionsWindow;
    private final TransportPanel transportPanel;
    private boolean fullScreen;

    /**
     * Constructs a {@link MargiaWindow}
     * 
     * @param timing    the timing source
     * @param transport the transport to use
     */
    public MargiaWindow(TimingSource timing, Transport transport) {
        var loader = new FXMLLoader(getClass().getResource("MargiaWindow.fxml"));
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load MargiaWindow.fxml", e);
            throw new IllegalStateException(e);
        }

        // Initialize TransportPanel with constructor dependencies and place at bottom
        transportPanel = new TransportPanel(timing, transport);
        rootLayout.setBottom(transportPanel);

        // Assign icons, tooltips, and key accelerators
        assignIconsAndTooltips();

        // Wire bidirectional listeners before initMusicians() runs
        agentPanel.addPropertyChangeListener(AgentPanel.SELECTED_AGENT_PROPERTY, optionsWindow);
        agentPanel.addPropertyChangeListener(AgentPanel.AGENT_ADDED_PROPERTY, optionsWindow);
        agentPanel.addPropertyChangeListener(AgentPanel.AGENT_REMOVED_PROPERTY, optionsWindow);
        optionsWindow.addPropertyChangeListener(agentPanel);

        updateTitle();
        updateRecentsMenu();
        registerListeners();

        var scene = new Scene(rootLayout, 1000, 750);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleGlobalKeyReleased);
        setScene(scene);

        setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });

        // Safe to call directly now since listeners are already registered
        agentPanel.init();
        agentPanel.initMusicians();
        transport.addPropertyListener(Transport.TICK_PROPERTY, agentPanel);
    }

    @FXML
    private void initialize() {
        // Configure mode toggle buttons
        selectModeBtn.setUserData(EditMode.SELECT);
        addModeBtn.setUserData(EditMode.ADD);
        moveModeBtn.setUserData(EditMode.MOVE);
        connectModeBtn.setUserData(EditMode.CONNECT);
        selectModeBtn.setSelected(true);

        modeGroup.selectedToggleProperty().addListener((_, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            if (newToggle.getUserData() instanceof EditMode mode) {
                agentPanel.setEditMode(mode);
            }
        });

        // Initialize OptionsWindow and sync visibility with toolbar toggle button
        optionsWindow = new OptionsWindow(this);
        showUiOptions.setSelected(optionsWindow.isShowing());
        optionsWindow.showingProperty().addListener((_, _, isShowing) -> showUiOptions.setSelected(isShowing));
    }

    private void assignIconsAndTooltips() {
        // Menu item icons
        openMenuItem.setGraphic(getToolbarIcon(OPEN));
        saveMenuItem.setGraphic(getToolbarIcon(SAVE));
        saveAsMenuItem.setGraphic(getToolbarIcon(SAVE));
        exitMenuItem.setGraphic(getToolbarIcon(EXIT));

        copyMenuItem.setGraphic(getToolbarIcon(COPY));
        pasteMenuItem.setGraphic(getToolbarIcon(PASTE));
        copyParamsMenuItem.setGraphic(getToolbarIcon(COPY));
        pasteParamsMenuItem.setGraphic(getToolbarIcon(PASTE));
        selectAllMenuItem.setGraphic(getToolbarIcon(SELECT_ALL));
        deselectAllMenuItem.setGraphic(getToolbarIcon(DESELECT_ALL));
        selectConnectedMenuItem.setGraphic(getMenuIcon(SELECT_CONNECTED));
        muteMenuItem.setGraphic(getToolbarIcon(MUTE));
        unmuteMenuItem.setGraphic(getToolbarIcon(UNMUTE));
        lockMenuItem.setGraphic(getToolbarIcon(LOCK));
        unlockMenuItem.setGraphic(getToolbarIcon(UNLOCK));
        lockAllMenuItem.setGraphic(getToolbarIcon(LOCK_ALL));
        unlockAllMenuItem.setGraphic(getToolbarIcon(UNLOCK_ALL));
        deleteMenuItem.setGraphic(getToolbarIcon(DELETE));
        connectMenuItem.setGraphic(getToolbarIcon(CONNECT));
        disconnectMenuItem.setGraphic(getToolbarIcon(DISCONNECT));

        helpMenu.setGraphic(getToolbarIcon(HELP));
        aboutMenuItem.setGraphic(getToolbarIcon(ABOUT));
        keyboardShortcutMenuItem.setGraphic(getToolbarIcon(KEYBOARD));
        // Menu item accelerators
        saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        copyMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        pasteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        copyParamsMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        pasteParamsMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        selectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        deselectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.ESCAPE));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        // Toolbar button icons and tooltips
        configureButton(selectAllBtn, SELECT_ALL, "Select all");
        configureButton(deselectAllBtn, DESELECT_ALL, "Deselect all");
        configureButton(copyBtn, COPY, "Copy selected");
        configureButton(pasteBtn, PASTE, "Paste");
        configureButton(deleteBtn, DELETE, "Delete selected");
        configureButton(muteBtn, MUTE, "Mute selected");
        configureButton(unmuteBtn, UNMUTE, "Unmute selected");
        configureButton(listeningBtn, LISTENING, "Set selected agents listening");
        configureButton(notListeningBtn, NOT_LISTENING, "Set selected agents not listening");
        configureButton(lockBtn, LOCK, "Lock selected");
        configureButton(unlockBtn, UNLOCK, "Unlock selected");
        configureButton(lockAllBtn, LOCK_ALL, "Lock all");
        configureButton(unlockAllBtn, UNLOCK_ALL, "Unlock all");
        configureButton(connectBtn, CONNECT, "Connect selected");
        configureButton(disconnectBtn, DISCONNECT, "Disconnect selected");
        configureButton(selectConnectedBtn, SELECT_CONNECTED, "Select connected");
        // Mode toggle button icons and tooltips
        selectModeBtn.setGraphic(getMenuIcon(SELECT));
        selectModeBtn.setTooltip(new Tooltip("Selection mode"));
        addModeBtn.setGraphic(getMenuIcon(ADD));
        addModeBtn.setTooltip(new Tooltip("Add Musician(s)"));
        moveModeBtn.setGraphic(getMenuIcon(MOVE));
        moveModeBtn.setTooltip(new Tooltip("Move Musician mode"));
        connectModeBtn.setGraphic(getMenuIcon(CONNECT));
        connectModeBtn.setTooltip(new Tooltip("Connect/Disconnect two musicians"));
        // AddPanel default icon and mode menu items
        addBtn.setGraphic(getToolbarIcon(ADD));
        configureAddModeItem(addMultipleMenuItem, ADD, "Add N musicians");
        configureAddModeItem(addCircleMenuItem, ADD_CIRCLE, "Add musician circle");
        configureAddModeItem(addGridMenuItem, ADD_GRID, "Add musician grid");
        configureAddModeItem(addFanMenuItem, ADD_FAN, "Add musician fan");
    }

    private static void configureButton(Button button, String iconKey, String tooltipText) {
        button.setGraphic(getMenuIcon(iconKey));
        button.setTooltip(new Tooltip(tooltipText));
    }

    private static void configureAddModeItem(MenuItem item, String iconKey, String tooltipText) {
        var graphicLabel = new Label(null, getMenuIcon(iconKey));
        Tooltip.install(graphicLabel, new Tooltip(tooltipText));
        item.setGraphic(graphicLabel);
    }

    private void registerListeners() {
        Options.getInstance().getUiOptions().showNumbersProperty().addListener(MusicianComponent.SHOW_NUMBERS_LISTENER);
        Options.getInstance().getMusicOptions().tempoProperty().addListener(MusicianComponent.TEMPO_LISTENER);
        Options.getInstance().dirtyProperty().addListener((_, _, _) -> Platform.runLater(this::updateTitle));
    }

    private void updateTitle() {
        var title = "MARGIA";
        if (Options.getInstance().getFilename() != null) {
            title += " - " + Options.getInstance().getFilename().getFileName().toString();
        }
        if (Options.getInstance().isDirty()) {
            title += " *";
        }
        setTitle(title);
    }

    private void updateRecentsMenu() {
        openRecents.getItems().clear();
        var recentFiles = MargiaFileUtils.getRecentsFromPersistence();
        for (var i = 0; i < recentFiles.size(); i++) {
            final int index = i;
            var file = new MenuItem("%d: %s".formatted(i + 1, recentFiles.get(i)));
            file.setId(recentFiles.get(i));
            file.setOnAction(_ -> openSettingsFromFile(Paths.get(recentFiles.get(index))));
            openRecents.getItems().add(file);
        }
    }

    // --- FXML Action Handlers ---
    @FXML
    private void onOpenClicked() {
        openSettingsFromFileUserPrompt();
    }

    @FXML
    private void onSaveClicked() {
        saveSettingsToFile();
    }

    @FXML
    private void onSaveAsClicked() {
        Options.getInstance().setFilename(null);
        saveSettingsToFile();
    }

    @FXML
    private void onExitClicked() {
        handleExitRequest();
    }

    @FXML
    private void onCopyClicked() {
        agentPanel.copySelectedComponents();
    }

    @FXML
    private void onPasteClicked() {
        agentPanel.paste();
    }

    @FXML
    private void onCopyParamsClicked() {
        agentPanel.copySelectedComponentOptions();
    }

    @FXML
    private void onPasteParamsClicked() {
        agentPanel.pasteSettings();
    }

    @FXML
    private void onSelectAllClicked() {
        agentPanel.selectAll();
    }

    @FXML
    private void onDeselectAllClicked() {
        agentPanel.deselectAll();
    }

    @FXML
    private void onSelectConnectedClicked() {
        agentPanel.selectConnected();
    }

    @FXML
    private void onMuteClicked() {
        agentPanel.new MuteSelectedCommand().execute();
    }

    @FXML
    private void onUnmuteClicked() {
        agentPanel.new UnmuteSelectedCommand().execute();
    }

    @FXML
    private void onLockClicked() {
        agentPanel.new LockSelectedCommand().execute();
    }

    @FXML
    private void onUnlockClicked() {
        agentPanel.new UnlockSelectedCommand().execute();
    }

    @FXML
    private void onLockAllClicked() {
        agentPanel.new LockAllCommand().execute();
    }

    @FXML
    private void onUnlockAllClicked() {
        agentPanel.new UnlockAllCommand().execute();
    }

    @FXML
    private void onDeleteClicked() {
        agentPanel.new DeleteSelectedCommand().execute();
    }

    @FXML
    private void onConnectClicked() {
        agentPanel.new ConnectSelectedCommand().execute();
    }

    @FXML
    private void onDisconnectClicked() {
        agentPanel.new DisconnectSelectedCommand().execute();
    }

    @FXML
    private void onSetListeningClicked() {
        agentPanel.new SetSelectedListeningCommand().execute();
    }

    @FXML
    private void onSetNotListeningClicked() {
        agentPanel.new SetSelectedNotListeningCommand().execute();
    }

    @FXML
    private void onToggleOptionsClicked() {
        if (optionsWindow != null) {
            optionsWindow.setVisible(showUiOptions.isSelected());
        }
    }

    @SuppressWarnings("static-method")
    @FXML
    private void onRescanMidiClicked() {
        MidiController.getInstance().scanForMidiOutputDevices();
        MidiController.getInstance().scanForMidiInputDevices();
    }

    @SuppressWarnings("static-method")
    @FXML
    private void onMidiPanicClicked() {
        MidiController.getInstance().allNotesOff();
    }

    @FXML
    private void onAboutClicked() {
        showInfoAlert("About", ABOUT_MESSAGE);
    }

    @FXML
    private void onKeyboardShortcutsClicked() {
        showInfoAlert("Keyboard Shortcuts", KEYBOARD_SHORTCUTS);
    }

    // --- AddPanel Handlers & Helpers ---
    @FXML
    private void onShowAddModeMenu() {
        addModeMenu.show(addDropdownBtn, Side.BOTTOM, 0, 0);
    }

    @FXML
    private void onSelectMultipleMode() {
        currentAddMode = AddMode.MULTIPLE;
        addBtn.setGraphic(getToolbarIcon(ADD));
    }

    @FXML
    private void onSelectCircleMode() {
        currentAddMode = AddMode.CIRCLE;
        addBtn.setGraphic(getToolbarIcon(ADD_CIRCLE));
    }

    @FXML
    private void onSelectGridMode() {
        currentAddMode = AddMode.GRID;
        addBtn.setGraphic(getToolbarIcon(ADD_GRID));
    }

    @FXML
    private void onSelectFanMode() {
        currentAddMode = AddMode.FAN;
        addBtn.setGraphic(getToolbarIcon(ADD_FAN));
    }

    @FXML
    private void onAddClicked() {
        switch (currentAddMode) {
        case CIRCLE -> {
            var grid = createDialogGrid();
            var spinner = createIntDialogSpinner(3, 3, 1000, 1);
            grid.add(new Label("Number to add"), 0, 0);
            grid.add(spinner, 1, 0);
            if (showCustomInputDialog("Add Musician Circle", grid)) {
                agentPanel.addCircle((Integer) spinner.getValue());
            }
        }
        case GRID -> {
            var grid = createDialogGrid();
            var rowSpinner = createIntDialogSpinner(1, 1, 100, 1);
            var colSpinner = createIntDialogSpinner(1, 1, 100, 1);
            grid.add(new Label("Rows"), 0, 0);
            grid.add(rowSpinner, 1, 0);
            grid.add(new Label("Columns"), 0, 1);
            grid.add(colSpinner, 1, 1);
            if (showCustomInputDialog("Add Musician Grid", grid)) {
                agentPanel.new AddGridCommand((Integer) rowSpinner.getValue(), (Integer) colSpinner.getValue())
                        .execute();
            }
        }
        case FAN -> {
            var grid = createDialogGrid();
            var levelSpinner = createIntDialogSpinner(2, 2, 10, 1);
            var directionBox = new ComboBox<FanDirection>();
            directionBox.getItems().addAll(FanDirection.values());
            directionBox.getSelectionModel().selectFirst();
            var directionLabel = new Label("Direction");
            directionLabel.setLabelFor(directionBox);
            grid.add(new Label("Levels"), 0, 0);
            grid.add(levelSpinner, 1, 0);
            grid.add(directionLabel, 0, 1);
            grid.add(directionBox, 1, 1);
            if (showCustomInputDialog("Add Musician Fan", grid)) {
                agentPanel.new AddFanCommand((Integer) levelSpinner.getValue(), directionBox.getValue()).execute();
            }
        }
        case MULTIPLE -> {
            var grid = createDialogGrid();
            var spinner = createIntDialogSpinner(2, 1, 1000, 1);
            grid.add(new Label("Number to add"), 0, 0);
            grid.add(spinner, 1, 0);
            if (showCustomInputDialog("Add N musicians", grid)) {
                agentPanel.new AddNMusiciancCommand((Integer) spinner.getValue()).execute();
            }
        }
        default -> throw new IllegalArgumentException("Don't know how to create a " + currentAddMode);
        }
    }

    private static GridPane createDialogGrid() {
        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        return grid;
    }

    private static Spinner<Integer> createIntDialogSpinner(int initial, int min, int max, int step) {
        var spinner = new Spinner<Integer>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setPrefWidth(100);
        return spinner;
    }

    private boolean showCustomInputDialog(String title, Node content) {
        var dialog = new Dialog<ButtonType>();
        dialog.initOwner(this);
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        var result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // --- Keyboard Shortcuts & Lifecycle ---
    private void handleGlobalKeyReleased(KeyEvent e) {
        if (e.getTarget() instanceof TextInputControl) {
            return;
        }
        boolean complete = false;
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();
        switch (e.getCode()) {
        case ESCAPE -> {
            agentPanel.deselectAll();
            complete = true;
        }
        case DELETE -> {
            agentPanel.new DeleteSelectedCommand().execute();
            complete = true;
        }
        case A -> {
            if (ctrl) {
                agentPanel.selectAll();
                complete = true;
            }
        }
        case C -> {
            if (ctrl) {
                if (shift) {
                    agentPanel.copySelectedComponentOptions();
                } else {
                    agentPanel.copySelectedComponents();
                }
                complete = true;
            }
        }
        case M -> {
            if (ctrl) {
                agentPanel.new ToggleMuteSelectedCommand().execute();
                complete = true;
            }
        }
        case V -> {
            if (ctrl) {
                if (shift) {
                    agentPanel.pasteSettings();
                } else {
                    agentPanel.paste();
                }
                complete = true;
            }
        }
        case S -> {
            if (ctrl) {
                saveSettingsToFile();
                complete = true;
            }
        }
        case O -> {
            if (ctrl) {
                openSettingsFromFileUserPrompt();
                complete = true;
            }
        }
        case F11 -> {
            if (fullScreen) {
                rootLayout.setTop(topContainer);
                toolbar.setVisible(true);
                toolbar.setManaged(true);
            } else {
                rootLayout.setTop(null);
                toolbar.setVisible(false);
                toolbar.setManaged(false);
            }
            fullScreen = !fullScreen;
            setFullScreen(fullScreen);
            complete = true;
        }
        case SPACE -> {
            if (ctrl) {
                transportPanel.toggleStartStop();
                complete = true;
            }
        }
        case L -> {
            if (ctrl) {
                agentPanel.new ToggleSelectedListeningCommand().execute();
                complete = true;
            }
        }
        case Z -> {
            if (ctrl) {
                CommandCaretaker.undo();
                complete = true;
            }
        }
        default -> {
            // do nothing
        }
        }
        if (complete) {
            e.consume();
        }
    }

    private void handleExitRequest() {
        if (Options.getInstance().isDirty()) {
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(this);
            alert.setTitle("Save before exiting?");
            alert.setHeaderText(null);
            alert.setContentText("Save " + Options.getInstance().getFilename() + " before exiting?");
            var saveBtn = new ButtonType("Save", ButtonBar.ButtonData.YES);
            var dontSaveBtn = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
            var cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);
            var result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancelBtn) {
                return;
            }
            if (result.get() == saveBtn) {
                saveSettingsToFile();
            }
        }
        Platform.exit();
        System.exit(0);
    }

    private void showInfoAlert(String title, String content) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(this);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(this);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- File Open & Save Operations ---
    private void openSettingsFromFileUserPrompt() {
        var newSaveLocation = getFilePathFromUser(this, "Select file", FileAction.LOAD);
        if (newSaveLocation == null) {
            return;
        }
        openSettingsFromFile(newSaveLocation);
    }

    private void openSettingsFromFile(Path fileLocation) {
        Options.getInstance().setFilename(fileLocation);
        try (var is = Files.newInputStream(Options.getInstance().getFilename())) {
            Options.getInstance().load(is);
            Options.getInstance().setSaveDir(fileLocation.getParent());
            registerListeners();
            optionsWindow.updateOptions();
            Key.initFromOptions();
            DieRoller.initFromOptions();
            Transport.instance().initFromOptions();
            transportPanel.initFromOptions();
            updateTitle();
            agentPanel.reset();
            agentPanel.init();
            agentPanel.initMusicians();
            optionsWindow.syncExistingMusicians();
            MargiaFileUtils.saveRecentsToPersistence(fileLocation.toString());
            updateRecentsMenu();
        } catch (IOException e1) {
            showErrorAlert("Error loading file", e1.getMessage());
        } catch (BackingStoreException e) {
            LOGGER.atError().setMessage("Error writing save directory to preferences: {}").addArgument(e::getMessage)
                    .log();
        }
    }

    private void saveSettingsToFile() {
        var options = Options.getInstance();
        boolean newFile = Options.getInstance().getFilename() == null;
        if (newFile) {
            Options.getInstance().setFilename(getFilePathFromUser(this, "Select Save location", FileAction.SAVE));
        }
        if (Options.getInstance().getFilename() == null) {
            return;
        }
        try {
            var filenameStr = Options.getInstance().getFilename().toString();
            if (filenameStr.lastIndexOf('.') == -1) {
                Options.getInstance().setFilename(
                        Options.getInstance().getFilename().resolveSibling(filenameStr + MARGIA_EXTENSION));
            }
            if (newFile && Files.exists(Options.getInstance().getFilename())) {
                var confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.initOwner(this);
                confirm.setTitle("Confirm overwrite");
                confirm.setHeaderText(null);
                confirm.setContentText("File '%s' exists, do you want to replace it?"
                        .formatted(Options.getInstance().getFilename().toString()));
                confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                var result = confirm.showAndWait();
                if (result.isEmpty() || result.get() == ButtonType.NO) {
                    Options.getInstance().setFilename(null);
                    return;
                }
            }
            try (var os = Files.newOutputStream(Options.getInstance().getFilename())) {
                options.store(os);
            }
            updateTitle();
            MargiaFileUtils.saveRecentsToPersistence(Options.getInstance().getFilename().toString());
            updateRecentsMenu();
        } catch (IOException e1) {
            showErrorAlert("Error saving file", e1.getMessage());
        }
    }

    /**
     * @param parent      parent window
     * @param dialogTitle title of dialog
     * @param action      action to perform
     * @return a {@link Path} that the user selected, or {@code null} if the user
     *         cancelled
     */
    public Path getFilePathFromUser(Window parent, String dialogTitle, FileAction action) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle(dialogTitle);
        var saveDir = Options.getInstance().getSaveDir();
        if (saveDir != null && Files.isDirectory(saveDir)) {
            fileChooser.setInitialDirectory(saveDir.toFile());
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MARGIA files", "*.margia"));
        File selectedFile;
        if (action == FileAction.SAVE) {
            selectedFile = fileChooser.showSaveDialog(parent);
        } else {
            selectedFile = fileChooser.showOpenDialog(parent);
        }
        if (selectedFile != null) {
            String selectedFilename = selectedFile.getAbsolutePath();
            if (!selectedFilename.toLowerCase().endsWith(MARGIA_EXTENSION)) {
                selectedFilename += MARGIA_EXTENSION;
                selectedFile = new File(selectedFilename);
            }
            try {
                Options.getInstance().setSaveDir(selectedFile.getParentFile().toPath());
            } catch (BackingStoreException e) {
                var alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(this);
                alert.setTitle("Error saving preferences");
                alert.setHeaderText(null);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
            return selectedFile.toPath();
        }
        // If the user cancels
        return null;
    }

    enum FileAction {
        @SuppressWarnings("hiding")
        SAVE, LOAD;
    }

    enum AddMode {
        MULTIPLE, CIRCLE, GRID, FAN;
    }
}