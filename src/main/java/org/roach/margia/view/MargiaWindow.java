package org.roach.margia.view;

import static org.roach.margia.view.Icons.*;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.nio.file.Path;

import javax.swing.event.ChangeListener;

import org.roach.margia.command.CommandCaretaker;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.TimingSource;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.util.DieRoller;
import org.roach.margia.util.MargiaFileUtils;
import org.roach.margia.view.AgentPanel.EditMode;
import org.roach.margia.view.AgentPanel.FanDirection;
import org.roach.margia.view.ChangeEmitter.ChangeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The main program window
 */
@SuppressWarnings({ "java:S1948" })
public class MargiaWindow extends Stage implements ChangeListener {
    private static final String MARGIA_EXTENSION = ".margia";
    private static final String ABOUT_MESSAGE = """
            <html>
            <h1>MARGIA</h1>

            <h3>The <u>M</u>usic <u>a</u>nd <u>R</u>hythm <u>G</u>enerating <u>I</u>ntelligent <u>A</u>gents</h3>

            <h3>By Stevie Roach</h3>
            <a href="mailto:roachls@yahoo.com">roachls@yahoo.com</a>
            <br>
            <a href="https://github.com/roachls/margia">https://github.com/roachls/margia</a>
            </html>
            """;
    private OptionsWindow optionsWindow;
    private AgentPanel agentPanel;
    private boolean fullScreen;
    private static final Logger LOGGER = LoggerFactory.getLogger(MargiaWindow.class);
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
    private MenuBar menubar;
    private ToolBar toolbar;
    private final TransportPanel transportPanel;
    private Menu openRecents;
    private final BorderPane rootLayout;

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
     *                           true
     */
    public MargiaWindow(TimingSource timing, Transport transport) {
        rootLayout = new BorderPane();

        // Top region: MenuBar stacked above ToolBar
        setupMenu();
        toolbar = createToolPalette();
        var topContainer = new VBox(menubar, toolbar);
        rootLayout.setTop(topContainer);

        agentPanel = new AgentPanel();
        agentPanel.setMinSize(0, 0);
        rootLayout.setCenter(agentPanel);

        transportPanel = new TransportPanel(timing, transport);
        rootLayout.setBottom(transportPanel);

        // Initialize OptionsWindow synchronously and link its owner to MargiaWindow
        optionsWindow = new OptionsWindow(this);

        // Wire bidirectional listeners before initMusicians() runs
        agentPanel.addPropertyChangeListener(AgentPanel.SELECTED_AGENT_PROPERTY, optionsWindow);
        agentPanel.addPropertyChangeListener(AgentPanel.AGENT_ADDED_PROPERTY, optionsWindow);
        agentPanel.addPropertyChangeListener(AgentPanel.AGENT_REMOVED_PROPERTY, optionsWindow);
        optionsWindow.addPropertyChangeListener(agentPanel);

        updateTitle();
        registerListeners();

        var scene = new Scene(rootLayout, 1000, 750);
        // Replaces KeyboardFocusManager.addKeyEventDispatcher
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, this::handleGlobalKeyReleased);
        setScene(scene);

        // Replaces WindowConstants.EXIT_ON_CLOSE
        setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });

        // Safe to call directly now since listeners are already registered
        agentPanel.init();
        agentPanel.initMusicians();
        transport.addPropertyListener(Transport.TICK_PROPERTY, agentPanel);
    }

    private void registerListeners() {
        Options.getInstance().getUiOptions().addChangeListener(UiOptions.SHOW_NUMBERS_PROPERTY,
                MusicianComponent.SHOW_NUMBERS_LISTENER);
        Options.getInstance().getMusicOptions().addChangeListener(MusicOptions.TEMPO_PROPERTY,
                MusicianComponent.TEMPO_LISTENER);
        Options.getInstance().addChangeListener(Options.DIRTY_PROPERTY, this);
    }

    // Register in MargiaWindow constructor:
    // scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED,
    // this::handleGlobalKeyReleased);

    private void handleGlobalKeyReleased(KeyEvent e) {
        // Skip shortcuts if the user is actively typing inside a TextField or editable
        // Spinner/ComboBox
        if (e.getTarget() instanceof TextInputControl) {
            return;
        }

        boolean complete = false;
        // isShortcutDown() maps to Ctrl on Windows/Linux and Cmd on macOS (or use
        // e.isControlDown())
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
                // Restore MenuBar and ToolBar at the top of rootLayout
                rootLayout.setTop(new javafx.scene.layout.VBox(menubar, toolbar));
                toolbar.setVisible(true);
                toolbar.setManaged(true);
            } else {
                // Hide MenuBar and ToolBar without leaving empty whitespace
                rootLayout.setTop(null);
                toolbar.setVisible(false);
                toolbar.setManaged(false);
            }
            fullScreen = !fullScreen;
            setFullScreen(fullScreen); // Optional: also toggle native Stage full-screen mode
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

    private void setupMenu() {
        menubar = new MenuBar();

        setupFileMenu(menubar);
        setupEditMenu(menubar);
        setupMidiMenu(menubar);
        setupHelpMenu(menubar);
        // Note: menubar is added to rootLayout.setTop(...) in the MargiaWindow
        // constructor
    }

    private void setupFileMenu(MenuBar menubar) {
        var fileMenu = new Menu("_File");
        fileMenu.setMnemonicParsing(true);

        var openMenuItem = new MenuItem("_Open", getToolbarIcon(OPEN));
        openMenuItem.setMnemonicParsing(true);
        openMenuItem.setOnAction(_ -> openSettingsFromFileUserPrompt());

        var saveMenuItem = new MenuItem("_Save", getToolbarIcon(SAVE));
        saveMenuItem.setMnemonicParsing(true);
        saveMenuItem.setOnAction(_ -> saveSettingsToFile());

        var saveAsMenuItem = new MenuItem("Save _As...", getToolbarIcon(SAVE));
        saveAsMenuItem.setMnemonicParsing(true);
        saveAsMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
        saveAsMenuItem.setOnAction(_ -> {
            Options.getInstance().setFilename(null);
            saveSettingsToFile();
        });

        openRecents = new Menu("Open recent file");
        updateRecentsMenu();

        var exitMenuItem = new MenuItem("E_xit", getToolbarIcon(EXIT));
        exitMenuItem.setMnemonicParsing(true);
        exitMenuItem.setOnAction(_ -> handleExitRequest());

        fileMenu.getItems().addAll(openMenuItem, openRecents, saveMenuItem, saveAsMenuItem, new SeparatorMenuItem(),
                exitMenuItem);
        menubar.getMenus().add(fileMenu);
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
        javafx.application.Platform.exit();
        System.exit(0);
    }

    private void updateRecentsMenu() {
        openRecents.getItems().clear();
        var recentFiles = MargiaFileUtils.getRecentsFromPersistence();
        for (var i = 0; i < recentFiles.size(); i++) {
            final int index = i;
            var file = new MenuItem("%d: %s".formatted(i + 1, recentFiles.get(i)));
            file.setId(recentFiles.get(i));
            file.setOnAction(_ -> openSettingsFromFile(java.nio.file.Paths.get(recentFiles.get(index))));
            openRecents.getItems().add(file);
        }
    }

    private void setupEditMenu(MenuBar menubar) {
        var editMenu = new Menu("_Edit");
        editMenu.setMnemonicParsing(true);

        var copyMenuItem = new MenuItem("_Copy", getToolbarIcon(COPY));
        copyMenuItem.setMnemonicParsing(true);
        copyMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
        copyMenuItem.setOnAction(_ -> agentPanel.copySelectedComponents());

        var pasteMenuItem = new MenuItem("_Paste", getToolbarIcon(PASTE));
        pasteMenuItem.setMnemonicParsing(true);
        pasteMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
        pasteMenuItem.setOnAction(_ -> agentPanel.paste());

        var copyParamsMenuItem = new MenuItem("Copy settings", getToolbarIcon(COPY));
        copyParamsMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        copyParamsMenuItem.setOnAction(_ -> agentPanel.copySelectedComponentOptions());

        var pasteParamsMenuItem = new MenuItem("Paste settings", getToolbarIcon(PASTE));
        pasteParamsMenuItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN, javafx.scene.input.KeyCombination.SHIFT_DOWN));
        pasteParamsMenuItem.setOnAction(_ -> agentPanel.pasteSettings());

        var selectAll = new MenuItem("_Select All", getToolbarIcon(SELECT_ALL));
        selectAll.setMnemonicParsing(true);
        selectAll.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
        selectAll.setOnAction(_ -> agentPanel.selectAll());

        var deselectAll = new MenuItem("_Deselect All", getToolbarIcon(DESELECT_ALL));
        deselectAll.setMnemonicParsing(true);
        deselectAll.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.ESCAPE));
        deselectAll.setOnAction(_ -> agentPanel.deselectAll());

        var selectConnected = new MenuItem("Select _connected", getMenuIcon(SELECT_CONNECTED));
        selectConnected.setMnemonicParsing(true);
        selectConnected.setOnAction(_ -> agentPanel.selectConnected());

        var muteSelected = new MenuItem("M_ute selected", getToolbarIcon(MUTE));
        muteSelected.setMnemonicParsing(true);
        muteSelected.setOnAction(_ -> agentPanel.new MuteSelectedCommand().execute());

        var unmuteSelected = new MenuItem("Unmut_e selected", getToolbarIcon(UNMUTE));
        unmuteSelected.setMnemonicParsing(true);
        unmuteSelected.setOnAction(_ -> agentPanel.new UnmuteSelectedCommand().execute());

        var lockSelected = new MenuItem("_Lock selected", getToolbarIcon(LOCK));
        lockSelected.setMnemonicParsing(true);
        lockSelected.setOnAction(_ -> agentPanel.new LockSelectedCommand().execute());

        var unlockSelected = new MenuItem("U_nlock selected", getToolbarIcon(UNLOCK));
        unlockSelected.setMnemonicParsing(true);
        unlockSelected.setOnAction(_ -> agentPanel.new UnlockSelectedCommand().execute());

        var lockAll = new MenuItem("Lock all", getToolbarIcon(LOCK_ALL));
        lockAll.setOnAction(_ -> agentPanel.new LockAllCommand().execute());

        var unlockAll = new MenuItem("Unlock all", getToolbarIcon(UNLOCK_ALL));
        unlockAll.setOnAction(_ -> agentPanel.new UnlockAllCommand().execute());

        var deleteSelected = new MenuItem("_Remove selected", getToolbarIcon(DELETE));
        deleteSelected.setMnemonicParsing(true);
        deleteSelected.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DELETE));
        deleteSelected.setOnAction(_ -> agentPanel.new DeleteSelectedCommand().execute());

        var connectSelected = new MenuItem("Connec_t selected", getToolbarIcon(CONNECT));
        connectSelected.setMnemonicParsing(true);
        connectSelected.setOnAction(_ -> agentPanel.new ConnectSelectedCommand().execute());

        var disconnectSelected = new MenuItem("D_isconnect selected", getToolbarIcon(DISCONNECT));
        disconnectSelected.setMnemonicParsing(true);
        disconnectSelected.setOnAction(_ -> agentPanel.new DisconnectSelectedCommand().execute());

        editMenu.getItems().addAll(copyMenuItem, pasteMenuItem, copyParamsMenuItem, pasteParamsMenuItem, selectAll,
                deselectAll, selectConnected, muteSelected, unmuteSelected, lockSelected, unlockSelected, lockAll,
                unlockAll, deleteSelected, connectSelected, disconnectSelected);
        menubar.getMenus().add(editMenu);
    }

    private static void setupMidiMenu(MenuBar menubar) {
        var midiMenu = new Menu("_Midi");
        midiMenu.setMnemonicParsing(true);

        var rescanMidi = new MenuItem("Rescan MIDI devices");
        rescanMidi.setOnAction(_ -> {
            MidiController.getInstance().scanForMidiOutputDevices();
            MidiController.getInstance().scanForMidiInputDevices();
        });

        var midiPanic = new MenuItem("MIDI Panic (All Notes Off)");
        midiPanic.setOnAction(_ -> MidiController.getInstance().allNotesOff());

        midiMenu.getItems().addAll(rescanMidi, midiPanic);
        menubar.getMenus().add(midiMenu);
    }

    private ToolBar createToolPalette() {
        toolbar = new ToolBar();

        var copyBtn = createToolButton(COPY, "Copy selected", () -> agentPanel.copySelectedComponents());
        var pasteBtn = createToolButton(PASTE, "Paste", () -> agentPanel.paste());

        var muteSelected = createToolButton(MUTE, "Mute selected",
                () -> agentPanel.new MuteSelectedCommand().execute());
        var unmuteSelected = createToolButton(UNMUTE, "Unmute selected",
                () -> agentPanel.new UnmuteSelectedCommand().execute());
        var setSelectedListening = createToolButton(LISTENING, "Set selected agents listening",
                () -> agentPanel.new SetSelectedListeningCommand().execute());
        var setSelectedNotListening = createToolButton(NOT_LISTENING, "Set selected agents not listening",
                () -> agentPanel.new SetSelectedNotListeningCommand().execute());

        var lockSelected = createToolButton(LOCK, "Lock selected",
                () -> agentPanel.new LockSelectedCommand().execute());
        var unlockSelected = createToolButton(UNLOCK, "Unlock selected",
                () -> agentPanel.new UnlockSelectedCommand().execute());
        var lockAll = createToolButton(LOCK_ALL, "Lock all", () -> agentPanel.new LockAllCommand().execute());
        var unlockAll = createToolButton(UNLOCK_ALL, "Unlock all", () -> agentPanel.new UnlockAllCommand().execute());

        var deleteSelected = createToolButton(DELETE, "Delete selected",
                () -> agentPanel.new DeleteSelectedCommand().execute());
        var connectSelected = createToolButton(CONNECT, "Connect selected",
                () -> agentPanel.new ConnectSelectedCommand().execute());
        var disconnectSelected = createToolButton(DISCONNECT, "Disconnect selected",
                () -> agentPanel.new DisconnectSelectedCommand().execute());

        var selectAll = createToolButton(SELECT_ALL, "Select all", () -> agentPanel.selectAll());
        var deselectAll = createToolButton(DESELECT_ALL, "Deselect all", () -> agentPanel.deselectAll());
        var selectConnected = createToolButton(SELECT_CONNECTED, "Select connected",
                () -> agentPanel.selectConnected());

        // Native JavaFX ToggleButton synced directly with OptionsWindow
        var showUiOptions = new ToggleButton("Options");
        showUiOptions.setSelected(optionsWindow != null && optionsWindow.isShowing());
        showUiOptions.setOnAction(_ -> {
            if (optionsWindow != null) {
                optionsWindow.setVisible(showUiOptions.isSelected());
            }
        });

        if (optionsWindow != null) {
            optionsWindow.showingProperty().addListener((_, _, isShowing) -> showUiOptions.setSelected(isShowing));
        }

        toolbar.getItems().addAll(selectAll, deselectAll, new Separator(), copyBtn, pasteBtn, deleteSelected,
                new Separator(), muteSelected, unmuteSelected, setSelectedListening, setSelectedNotListening,
                new Separator(), lockSelected, unlockSelected, lockAll, unlockAll, new Separator(), connectSelected,
                disconnectSelected, selectConnected);

        setupModesToolbar(toolbar);

        toolbar.getItems().addAll(new Separator(), showUiOptions, new Separator(), new AddPanel());

        return toolbar;
    }

    // Helper to keep icon button creation concise and avoid focus stealing from
    // AgentPanel
    private static Button createToolButton(String iconKey, String tooltipText, Runnable action) {
        var button = new Button(null, getMenuIcon(iconKey));
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private class AddPanel extends javafx.scene.layout.HBox {
        private AddMode mode = AddMode.MULTIPLE;

        AddPanel() {
            setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            setSpacing(2);

            var addBtn = new Button(null, getToolbarIcon(ADD));
            addBtn.setFocusTraversable(false);
            addBtn.setOnAction(_ -> {
                switch (mode) {
                case CIRCLE -> {
                    var grid = createDialogGrid();
                    var spinner = createIntDialogSpinner(3, 3, 1000, 1);
                    grid.add(new Label("Number to add"), 0, 0);
                    grid.add(spinner, 1, 0);

                    if (showCustomInputDialog("Add Musician Circle", grid)) {
                        agentPanel.addCircle(spinner.getValue());
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
                        agentPanel.new AddGridCommand(rowSpinner.getValue(), colSpinner.getValue()).execute();
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
                        agentPanel.new AddFanCommand(levelSpinner.getValue(), directionBox.getValue()).execute();
                    }
                }
                case MULTIPLE -> {
                    var grid = createDialogGrid();
                    var spinner = createIntDialogSpinner(2, 1, 1000, 1);
                    grid.add(new Label("Number to add"), 0, 0);
                    grid.add(spinner, 1, 0);

                    if (showCustomInputDialog("Add N musicians", grid)) {
                        agentPanel.new AddNMusiciancCommand(spinner.getValue()).execute();
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected mode: " + mode);
                }
            });

            // Replaces JPopupMenu
            var modeMenu = new ContextMenu();
            var singleMode = createModeMenuItem(ADD, AddMode.MULTIPLE, "Add N musicians", addBtn);
            var circleMode = createModeMenuItem(ADD_CIRCLE, AddMode.CIRCLE, "Add musician circle", addBtn);
            var gridMode = createModeMenuItem(ADD_GRID, AddMode.GRID, "Add musician grid", addBtn);
            var fanMode = createModeMenuItem(ADD_FAN, AddMode.FAN, "Add musician fan", addBtn);
            modeMenu.getItems().addAll(singleMode, circleMode, gridMode, fanMode);

            var modeBtn = new Button("▼");
            modeBtn.setPadding(new javafx.geometry.Insets(0, 4, 0, 4));
            modeBtn.setFocusTraversable(false);
            modeBtn.setOnAction(_ -> modeMenu.show(modeBtn, javafx.geometry.Side.BOTTOM, 0, 0));

            getChildren().addAll(new Label("Add: "), addBtn, modeBtn);
        }

        private MenuItem createModeMenuItem(String iconKey, AddMode targetMode, String tooltipText, Button addBtn) {
            // Wrap graphic in a Label so Tooltip attaches cleanly to MenuItem
            var graphicLabel = new Label(null, getMenuIcon(iconKey));
            Tooltip.install(graphicLabel, new Tooltip(tooltipText));

            var item = new MenuItem(null, graphicLabel);
            item.setOnAction(_ -> {
                mode = targetMode;
                // Create a fresh icon Node since a JavaFX Node cannot appear in two places
                // simultaneously
                addBtn.setGraphic(getToolbarIcon(iconKey));
            });
            return item;
        }

        private javafx.scene.layout.GridPane createDialogGrid() {
            var grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(8);
            grid.setPadding(new javafx.geometry.Insets(10));
            return grid;
        }

        private Spinner<Integer> createIntDialogSpinner(int initial, int min, int max, int step) {
            var spinner = new Spinner<Integer>(min, max, initial, step);
            spinner.setEditable(true);
            spinner.setPrefWidth(100);
            return spinner;
        }

        private boolean showCustomInputDialog(String title, javafx.scene.Node content) {
            var dialog = new Dialog<ButtonType>();
            dialog.initOwner(MargiaWindow.this);
            dialog.setTitle(title);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            var result = dialog.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }

        enum AddMode {
            MULTIPLE, CIRCLE, GRID, FAN;
        }
    }

    private void setupModesToolbar(ToolBar toolbar) {
        var selectMode = createModeButton(SELECT, "Selection mode", EditMode.SELECT);
        var addMode = createModeButton(ADD, "Add Musician(s)", EditMode.ADD);
        var moveMode = createModeButton(MOVE, "Move Musician mode", EditMode.MOVE);
        var connectMode = createModeButton(CONNECT, "Connect/Disconnect two musicians", EditMode.CONNECT);

        var group = new ToggleGroup();
        selectMode.setToggleGroup(group);
        addMode.setToggleGroup(group);
        moveMode.setToggleGroup(group);
        connectMode.setToggleGroup(group);

        selectMode.setSelected(true);

        // Replaces ModeChangeListener & ItemListener
        group.selectedToggleProperty().addListener((_, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // Prevent deselecting all mode buttons if the active one is clicked again
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            if (newToggle.getUserData() instanceof EditMode mode) {
                agentPanel.setEditMode(mode);
            }
        });

        toolbar.getItems().addAll(new Separator(), new Label("Mode:"), selectMode, addMode, moveMode, connectMode);
    }

    private static ToggleButton createModeButton(String iconKey, String tooltipText, EditMode mode) {
        var button = new ToggleButton(null, getMenuIcon(iconKey));
        button.setTooltip(new Tooltip(tooltipText));
        button.setId(mode.name());
        button.setUserData(mode);
        button.setFocusTraversable(false);
        return button;
    }

    private void setupHelpMenu(MenuBar menubar) {
        var helpMenu = new Menu("_Help", getToolbarIcon(HELP));
        helpMenu.setMnemonicParsing(true);

        var aboutMenuItem = new MenuItem("_About", getToolbarIcon(ABOUT));
        aboutMenuItem.setMnemonicParsing(true);
        aboutMenuItem.setOnAction(_ -> showInfoAlert("About", ABOUT_MESSAGE));

        var keyboardShortcutMenuItem = new MenuItem("_Keyboard Shortcuts", getToolbarIcon(KEYBOARD));
        keyboardShortcutMenuItem.setMnemonicParsing(true);
        keyboardShortcutMenuItem.setOnAction(_ -> showInfoAlert("Keyboard Shortcuts", KEYBOARD_SHORTCUTS));

        helpMenu.getItems().addAll(aboutMenuItem, keyboardShortcutMenuItem);
        menubar.getMenus().add(helpMenu);
    }

    private void showInfoAlert(String title, String content) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(this);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openSettingsFromFileUserPrompt() {
        var newSaveLocation = getFilePathFromUser(this, "Select file", FileAction.LOAD);
        if (newSaveLocation == null) {
            return;
        }
        openSettingsFromFile(newSaveLocation);
    }

    private void openSettingsFromFile(java.nio.file.Path fileLocation) {
        Options.getInstance().setFilename(fileLocation);
        try (var is = java.nio.file.Files.newInputStream(Options.getInstance().getFilename())) {
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

            // Sync OptionsWindow's tree with the newly loaded musicians
            optionsWindow.syncExistingMusicians();

            MargiaFileUtils.saveRecentsToPersistence(fileLocation.toString());
            updateRecentsMenu();
        } catch (java.io.IOException e1) {
            showErrorAlert("Error loading file", e1.getMessage());
        } catch (java.util.prefs.BackingStoreException e) {
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
            // Warn user if this was a Save As and file already exists
            if (newFile && java.nio.file.Files.exists(Options.getInstance().getFilename())) {
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
            try (var os = java.nio.file.Files.newOutputStream(Options.getInstance().getFilename())) {
                options.store(os);
            }
            updateTitle();
            MargiaFileUtils.saveRecentsToPersistence(Options.getInstance().getFilename().toString());
            updateRecentsMenu();
        } catch (java.io.IOException e1) {
            showErrorAlert("Error saving file", e1.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(this);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    enum FileAction {
        @SuppressWarnings("hiding")
        SAVE, LOAD;
    }

    /**
     * Opens a JFileChooser dialog and returns the absolute path of the selected
     * file.
     * 
     * @param parent      The parent component for the dialog (can be null).
     * @param dialogTitle
     * @param action      the action being performed
     * @return The absolute file path as an {@link Path}, or null if not found or
     *         cancelled
     */
    public java.nio.file.Path getFilePathFromUser(javafx.stage.Window parent, String dialogTitle, FileAction action) {
        var fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(dialogTitle);

        var saveDir = Options.getInstance().getSaveDir();
        if (saveDir != null && java.nio.file.Files.isDirectory(saveDir)) {
            fileChooser.setInitialDirectory(saveDir.toFile());
        }

        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("MARGIA files", "*.margia"));

        java.io.File selectedFile;
        if (action == FileAction.SAVE) {
            selectedFile = fileChooser.showSaveDialog(parent);
        } else {
            selectedFile = fileChooser.showOpenDialog(parent);
        }

        if (selectedFile != null) {
            String selectedFilename = selectedFile.getAbsolutePath();
            if (!selectedFilename.toLowerCase().endsWith(MARGIA_EXTENSION)) {
                selectedFilename += MARGIA_EXTENSION;
                selectedFile = new java.io.File(selectedFilename);
            }
            try {
                Options.getInstance().setSaveDir(selectedFile.getParentFile().toPath());
            } catch (java.util.prefs.BackingStoreException e) {
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

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String key, Object value) && key.equals(Options.DIRTY_PROPERTY)
                && (boolean) value) {
            javafx.application.Platform.runLater(this::updateTitle);
        }
    }

    private void updateTitle() {
        var pathStr = Options.getInstance().getFilename() == null ? ""
                : ("- " + Options.getInstance().getFilename().toString());
        var windowTitle = String.format("MARGIA %s%s", pathStr, Options.getInstance().isDirty() ? " *" : "");
        setTitle(windowTitle);
    }

}
