package org.roach.margia.view;

import static org.roach.margia.view.Icons.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.TimingSource;
import org.roach.margia.model.UiOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.view.AgentPanel.EditMode;
import org.roach.margia.view.AgentPanel.FanDirection;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * The main program window
 */
@SuppressWarnings({ "java:S1948" })
public class MargiaWindow extends JFrame implements ChangeListener {
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
    private MusicianOptionWindow musicianOptionsWindow;
    private OptionsWindow optionsWindow;
    private AgentPanel agentPanel;
    private static final Logger LOGGER = LogManager.getLogger(MargiaWindow.class);
    // OS-specific control key (Ctrl for Windows, Option for Mac)
    private static final String CONTROL_TEXT = InputEvent.getModifiersExText(InputEvent.CTRL_DOWN_MASK);
    private static final String SHIFT_TEXT = InputEvent.getModifiersExText(InputEvent.SHIFT_DOWN_MASK);
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
            <tr><td>Del</td><td>delete selected musicians</td></tr>
            <tr><td>Esc</td><td>Deselect all musicians</td></tr>
            </table>
            </html>
            """;

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
     *                           true
     */
    public MargiaWindow(TimingSource timing, Transport transport) throws HeadlessException {
        setUndecorated(true);
        getContentPane().setLayout(new BorderLayout());

        setupMenu();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var transportPanel = new TransportPanel(timing, transport);
        getContentPane().add(transportPanel, BorderLayout.SOUTH);
        musicianOptionsWindow = new MusicianOptionWindow();
        optionsWindow = new OptionsWindow();
        updateTitle();
        Options.getInstance().getUiOptions().addChangeListener(UiOptions.SHOW_NUMBERS_PROPERTY,
                MusicianComponent.SHOW_NUMBERS_LISTENER);
        Options.getInstance().addChangeListener(Options.DIRTY_PROPERTY, this);
        agentPanel = new AgentPanel();
        agentPanel.setBounds(0, 0, 1000, 1000);
        agentPanel.addVetoableChangeListener(musicianOptionsWindow);
        getContentPane().add(agentPanel, BorderLayout.CENTER);

        var palette1 = createToolPalette();
        add(palette1, BorderLayout.PAGE_START);

        pack();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new AgentPanelKeyListener());
        SwingUtilities.invokeLater(() -> agentPanel.initMusicians());
        
        transport.addPropertyListener(Transport.TICK_PROPERTY, agentPanel);
    }

    private class AgentPanelKeyListener implements KeyEventDispatcher {

        @Override
        @SuppressWarnings("java:S3776")
        public boolean dispatchKeyEvent(KeyEvent e) {
            boolean complete = false;
            switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                agentPanel.deselectAll();
                complete = true;
                break;
            case KeyEvent.VK_DELETE:
                agentPanel.deleteSelected();
                complete = true;
                break;
            case KeyEvent.VK_A:
                if (e.isControlDown()) {
                    agentPanel.selectAll();
                    complete = true;
                }
                break;
            case KeyEvent.VK_C:
                if (e.isControlDown()) {
                    if (e.isShiftDown()) {
                        agentPanel.copySelectedComponentOptions();
                        complete = true;
                    } else {
                        agentPanel.copySelectedComponents();
                        complete = true;
                    }
                }
                break;
            case KeyEvent.VK_V:
                if (e.isControlDown()) {
                    if (e.isShiftDown()) {
                        agentPanel.pasteSettings();
                        complete = true;
                    } else {
                        agentPanel.paste();
                        complete = true;
                    }
                }
                break;
            case KeyEvent.VK_S:
                if (e.isControlDown()) {
                    saveSettingsToFile();
                    complete = true;
                }
                break;
            case KeyEvent.VK_O:
                if (e.isControlDown()) {
                    openSettingsFromFile();
                    complete = true;
                }
                break;
            default:
                break;
            }
            return complete;
        }

    }

    private void setupMenu() {
        var menubar = new JMenuBar();

        setupFileMenu(menubar);
        setupEditMenu(menubar);
        setupMidiMenu(menubar);
        setupHelpMenu(menubar);
        setJMenuBar(menubar);
    }

    private void setupFileMenu(JMenuBar menubar) {
        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        var openMenuItem = new JMenuItem("Open", getToolbarIcon(OPEN));
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.addActionListener(_ -> openSettingsFromFile());

        var saveMenuItem = new JMenuItem("Save", getToolbarIcon(SAVE));
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(_ -> saveSettingsToFile());

        var saveAsMenuItem = new JMenuItem("Save As... (" + CONTROL_TEXT + "+S)", getToolbarIcon(SAVE));
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.addActionListener(_ -> {
            Options.getInstance().setFilename(null);
            saveSettingsToFile();
        });

        var exitMenuItem = new JMenuItem("Exit", getToolbarIcon(EXIT));
        exitMenuItem.setMnemonic(KeyEvent.VK_X);
        exitMenuItem.addActionListener(_ -> {
            if (Options.getInstance().isDirty()) {
                var answer = JOptionPane.showOptionDialog(MargiaWindow.this,
                        "Save " + Options.getInstance().getFilename() + " before exiting?", "Save before exiting?",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new Object[] { "Save", "Don't Save", "Cancel" }, "Save");
                if (answer == 0) {
                    saveSettingsToFile();
                } else if (answer == 1) {
                    return;
                }
            }
            System.exit(0);
        });

        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        menubar.add(fileMenu);
    }

    private void setupEditMenu(JMenuBar menubar) {
        var editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        var copyMenuItem = new JMenuItem("Copy (" + CONTROL_TEXT + "+C)", getToolbarIcon(COPY));
        copyMenuItem.setMnemonic(KeyEvent.VK_C);
        copyMenuItem.addActionListener(_ -> agentPanel.copySelectedComponents());
        var pasteMenuItem = new JMenuItem("Paste (" + CONTROL_TEXT + "+V)", getToolbarIcon(PASTE));
        pasteMenuItem.setMnemonic(KeyEvent.VK_P);
        pasteMenuItem.addActionListener(_ -> agentPanel.paste());
        var copyParamsMenuItem = new JMenuItem("Copy settings (" + CONTROL_TEXT + "+" + SHIFT_TEXT + "+C)",
                getToolbarIcon(COPY));
        copyParamsMenuItem.addActionListener(_ -> agentPanel.copySelectedComponentOptions());
        var pasteParamsMenuItem = new JMenuItem("Paste settings (" + CONTROL_TEXT + "+" + SHIFT_TEXT + "+V)",
                getToolbarIcon(PASTE));
        pasteParamsMenuItem.addActionListener(_ -> agentPanel.pasteSettings());

        var selectAll = new JMenuItem("Select All (" + CONTROL_TEXT + "+A)", getToolbarIcon(SELECT_ALL));
        selectAll.setMnemonic(KeyEvent.VK_S);
        selectAll.addActionListener(_ -> agentPanel.selectAll());
        var deselectAll = new JMenuItem("Deselect All (" + KeyEvent.getKeyText(KeyEvent.VK_ESCAPE) + ")",
                getToolbarIcon(DESELECT_ALL));
        deselectAll.setMnemonic(KeyEvent.VK_D);
        deselectAll.addActionListener(_ -> agentPanel.deselectAll());
        var selectConnected = new JMenuItem("Select connected", getMenuIcon(SELECT_CONNECTED));
        selectConnected.setMnemonic(KeyEvent.VK_D);
        selectConnected.addActionListener(_ -> agentPanel.selectConnected());
        var muteSelected = new JMenuItem("Mute selected", getToolbarIcon(MUTE));
        muteSelected.setMnemonic(KeyEvent.VK_U);
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        var unmuteSelected = new JMenuItem("Unmute selected", getToolbarIcon(UNMUTE));
        unmuteSelected.setMnemonic(KeyEvent.VK_E);
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        var lockSelected = new JMenuItem("Lock selected", getToolbarIcon(LOCK));
        lockSelected.setMnemonic(KeyEvent.VK_L);
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        var unlockSelected = new JMenuItem("Unlock selected", getToolbarIcon(UNLOCK));
        unlockSelected.setMnemonic(KeyEvent.VK_N);
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        var lockAll = new JMenuItem("Lock all", getToolbarIcon(LOCK_ALL));
        lockAll.addActionListener(_ -> agentPanel.lockAll());
        var unlockAll = new JMenuItem("Unlock all", getToolbarIcon(UNLOCK_ALL));
        unlockAll.addActionListener(_ -> agentPanel.unlockAll());
        var deleteSelected = new JMenuItem("Remove selected (" + KeyEvent.getKeyText(KeyEvent.VK_DELETE) + ")",
                getToolbarIcon(DELETE));
        deleteSelected.setMnemonic(KeyEvent.VK_R);
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        var connectSelected = new JMenuItem("Connect selected", getToolbarIcon(CONNECT));
        connectSelected.setMnemonic(KeyEvent.VK_T);
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        var disconnectSelected = new JMenuItem("Disconnect selected", getToolbarIcon(DISCONNECT));
        disconnectSelected.setMnemonic(KeyEvent.VK_I);
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());

        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.add(copyParamsMenuItem);
        editMenu.add(pasteParamsMenuItem);
        editMenu.add(selectAll);
        editMenu.add(deselectAll);
        editMenu.add(selectConnected);
        editMenu.add(muteSelected);
        editMenu.add(unmuteSelected);
        editMenu.add(lockSelected);
        editMenu.add(unlockSelected);
        editMenu.add(lockAll);
        editMenu.add(unlockAll);
        editMenu.add(deleteSelected);
        editMenu.add(connectSelected);
        editMenu.add(disconnectSelected);
        menubar.add(editMenu);
    }

    private static void setupMidiMenu(JMenuBar menubar) {
        var midiMenu = new JMenu("Midi");
        midiMenu.setMnemonic(KeyEvent.VK_M);
        
        var rescanMidi = new JMenuItem("Rescan MIDI devices");
        rescanMidi.addActionListener(_ -> {
            MidiController.getInstance().scanForMidiOutputDevices();
            MidiController.getInstance().scanForMidiInputDevices();
        });
        
        midiMenu.add(rescanMidi);
        
        menubar.add(midiMenu);
    }

    private JToolBar createToolPalette() {
        var toolbar = new JToolBar("Palette");
        toolbar.setFloatable(true);
        var copyBtn = new JButton(getMenuIcon(COPY));
        copyBtn.setToolTipText("Copy selected");
        copyBtn.addActionListener(_ -> agentPanel.copySelectedComponents());

        var pasteBtn = new JButton(getMenuIcon(PASTE));
        pasteBtn.setToolTipText("Paste");
        pasteBtn.addActionListener(_ -> agentPanel.paste());

        var muteSelected = new JButton(getMenuIcon(MUTE));
        muteSelected.setToolTipText("Mute selected");
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        var unmuteSelected = new JButton(getMenuIcon(UNMUTE));
        unmuteSelected.setToolTipText("Unmute selected");
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        var lockSelected = new JButton(getMenuIcon(LOCK));
        lockSelected.setToolTipText("Lock selected");
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        var unlockSelected = new JButton(getMenuIcon(UNLOCK));
        unlockSelected.setToolTipText("Unlock selected");
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        var lockAll = new JButton(getMenuIcon(LOCK_ALL));
        lockAll.setToolTipText("Lock all");
        lockAll.addActionListener(_ -> agentPanel.lockAll());
        var unlockAll = new JButton(getMenuIcon(UNLOCK_ALL));
        unlockAll.setToolTipText("Unlock all");
        unlockAll.addActionListener(_ -> agentPanel.unlockAll());
        var deleteSelected = new JButton(getMenuIcon(DELETE));
        deleteSelected.setToolTipText("Delete selected");
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        var connectSelected = new JButton(getMenuIcon(CONNECT));
        connectSelected.setToolTipText("Connect selected");
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        var disconnectSelected = new JButton(getMenuIcon(DISCONNECT));
        disconnectSelected.setToolTipText("Disconnect selected");
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());

        var selectAll = new JButton(getMenuIcon(SELECT_ALL));
        selectAll.addActionListener(_ -> agentPanel.selectAll());
        var deselectAll = new JButton(getMenuIcon(DESELECT_ALL));
        deselectAll.addActionListener(_ -> agentPanel.deselectAll());
        var selectConnected = new JButton(getMenuIcon(SELECT_CONNECTED));
        selectConnected.addActionListener(_ -> agentPanel.selectConnected());

        var showUiOptions = new JToggleButton("Options");
        showUiOptions.addActionListener(_ -> optionsWindow.setVisible(showUiOptions.isSelected()));
        optionsWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                showUiOptions.setSelected(false);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                showUiOptions.setSelected(false);
            }
        });

        var showMusicianOptions = new JToggleButton("Musician");
        showMusicianOptions.addActionListener(_ -> musicianOptionsWindow.setVisible(true));
        musicianOptionsWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                showMusicianOptions.setSelected(false);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                showMusicianOptions.setSelected(false);
            }
        });

        toolbar.add(selectAll);
        toolbar.add(deselectAll);
        toolbar.addSeparator();
        toolbar.add(copyBtn);
        toolbar.add(pasteBtn);
        toolbar.add(deleteSelected);
        toolbar.addSeparator();
        toolbar.add(muteSelected);
        toolbar.add(unmuteSelected);
        toolbar.addSeparator();
        toolbar.add(lockSelected);
        toolbar.add(unlockSelected);
        toolbar.add(lockAll);
        toolbar.add(unlockAll);
        toolbar.addSeparator();
        toolbar.add(connectSelected);
        toolbar.add(disconnectSelected);
        toolbar.add(selectConnected);
        setupModesToolbar(toolbar);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Show: "));
        toolbar.add(showUiOptions);
        toolbar.add(showMusicianOptions);
        toolbar.addSeparator();
        toolbar.add(new AddPanel());
        return toolbar;
    }

    private class AddPanel extends JPanel {
        private AddMode mode = AddMode.MULTIPLE;
        private static final DefaultComboBoxModel<FanDirection> fanDirectionModel = new DefaultComboBoxModel<>(
                FanDirection.values());

        AddPanel() {
            super(new FlowLayout(FlowLayout.LEFT, 0, 10));
            var addBtn = new JButton(getToolbarIcon(ADD));
            setOpaque(false);
            addBtn.addActionListener(_ -> {
                switch (mode) {
                case CIRCLE: {
                    var panel = new JPanel(new GridLayout(1, 2));
                    panel.add(new JLabel("Number to add"));
                    var spinner = new JSpinner(new SpinnerNumberModel(3, 3, 1000, 1));
                    panel.add(spinner);
                    var result = JOptionPane.showConfirmDialog(null, panel, "Add Musician Circle",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        var numToAdd = (int) spinner.getValue();
                        agentPanel.addCircle(numToAdd);
                    }
                }
                    break;
                case GRID: {
                    var panel = new JPanel(new GridLayout(2, 2));
                    panel.add(new JLabel("Rows"));
                    var rowSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
                    panel.add(rowSpinner);
                    panel.add(new JLabel("Columns"));
                    var colSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
                    panel.add(colSpinner);
                    var result = JOptionPane.showConfirmDialog(null, panel, "Add Musician Grid",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        agentPanel.addGrid((int) rowSpinner.getValue(), (int) colSpinner.getValue());
                    }
                }
                    break;
                case FAN: {
                    var panel = new JPanel(new GridLayout(1, 2));
                    panel.add(new JLabel("Levels"));
                    var levelSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 10, 1));
                    panel.add(levelSpinner);
                    var directionBox = new JComboBox<FanDirection>(fanDirectionModel);
                    var directionLabel = new JLabel("Direction");
                    directionLabel.setLabelFor(directionBox);
                    panel.add(directionLabel);
                    panel.add(directionBox);
                    var result = JOptionPane.showConfirmDialog(null, panel, "Add Musician Fan",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        agentPanel.addFan((int) levelSpinner.getValue(), (FanDirection) directionBox.getSelectedItem());
                    }
                }
                    break;
                case MULTIPLE: {
                    var panel = new JPanel(new GridLayout(1, 2));
                    panel.add(new JLabel("Number to add"));
                    var spinner = new JSpinner(new SpinnerNumberModel(2, 1, 1000, 1));
                    panel.add(spinner);
                    var result = JOptionPane.showConfirmDialog(null, panel, "Add N musicians",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        var numToAdd = (int) spinner.getValue();
                        agentPanel.addNMusicians(numToAdd);
                    }
                }
                    break;
                default:
                    break;
                }
            });
            var modeMenu = new JPopupMenu();
            var singleMode = new JMenuItem(getMenuIcon(ADD));
            singleMode.setName(AddMode.MULTIPLE.toString());
            singleMode.setToolTipText("Add N musicians");
            var circleMode = new JMenuItem(getMenuIcon(ADD_CIRCLE));
            circleMode.setName(AddMode.CIRCLE.toString());
            circleMode.setToolTipText("Add musician circle");
            var gridMode = new JMenuItem(getMenuIcon(ADD_GRID));
            gridMode.setName(AddMode.GRID.toString());
            gridMode.setToolTipText("Add musician grid");
            var fanMode = new JMenuItem(getMenuIcon(ADD_FAN));
            fanMode.setName(AddMode.FAN.toString());
            fanMode.setToolTipText("Add musician fan");
            modeMenu.add(singleMode);
            modeMenu.add(circleMode);
            modeMenu.add(gridMode);
            modeMenu.add(fanMode);

            var modeSelectionListener = (ActionListener) (e -> {
                if (e.getSource() instanceof JMenuItem jmi) {
                    var name = jmi.getName();
                    mode = AddMode.valueOf(name);
                    addBtn.setIcon(jmi.getIcon());
                }
            });

            singleMode.addActionListener(modeSelectionListener);
            circleMode.addActionListener(modeSelectionListener);
            gridMode.addActionListener(modeSelectionListener);
            fanMode.addActionListener(modeSelectionListener);

            var modeBtn = new JButton("▼");
            modeBtn.setMargin(new Insets(0, 0, 0, 0));
            modeBtn.setPreferredSize(new Dimension(30, 40));
            modeBtn.addActionListener(_ -> modeMenu.show(modeBtn, 0, 0));

            add(new JLabel("Add: "));
            add(addBtn);
            add(modeBtn);
        }

        enum AddMode {
            MULTIPLE, CIRCLE, GRID, FAN;
        }
    }

    private void setupModesToolbar(JToolBar toolbar) {
        var selectMode = new JToggleButton(getMenuIcon(SELECT));
        selectMode.setToolTipText("Selection mode");
        selectMode.setName(EditMode.SELECT.name());
        var addMode = new JToggleButton(getMenuIcon(ADD));
        addMode.setToolTipText("Add Musician(s)");
        addMode.setName(EditMode.ADD.name());
        var moveMode = new JToggleButton(getMenuIcon(MOVE));
        moveMode.setToolTipText("Move Musician mode");
        moveMode.setName(EditMode.MOVE.name());
        var connectMode = new JToggleButton(getMenuIcon(CONNECT));
        connectMode.setToolTipText("Connect/Disconnect two musicians");
        connectMode.setName(EditMode.CONNECT.name());

        selectMode.setSelected(true);

        toolbar.addSeparator();
        toolbar.add(new JLabel("Mode:"));
        var group = new ButtonGroup();
        group.add(selectMode);
        toolbar.add(selectMode);
        group.add(addMode);
        toolbar.add(addMode);
        group.add(moveMode);
        toolbar.add(moveMode);
        group.add(connectMode);
        toolbar.add(connectMode);

        var modeChangeListener = new ModeChangeListener();
        selectMode.addItemListener(modeChangeListener);
        addMode.addItemListener(modeChangeListener);
        moveMode.addItemListener(modeChangeListener);
        connectMode.addItemListener(modeChangeListener);
    }

    private class ModeChangeListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getSource() instanceof JToggleButton jtb && jtb.isSelected()) {
                var name = jtb.getName();
                var mode = EditMode.valueOf(name);
                agentPanel.setEditMode(mode);
            }
        }
    }

    private void setupHelpMenu(JMenuBar menubar) {
        var helpMenu = new JMenu("Help");
        helpMenu.setIcon(getToolbarIcon(HELP));
        helpMenu.setMnemonic('H');
        var aboutMenuItem = new JMenuItem("About", getToolbarIcon(ABOUT));
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, ABOUT_MESSAGE));
        var keyboardShortcutMenuItem = new JMenuItem("Keyboard Shortcuts", getToolbarIcon(KEYBOARD));
        keyboardShortcutMenuItem.setMnemonic(KeyEvent.VK_K);
        keyboardShortcutMenuItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, KEYBOARD_SHORTCUTS));
        helpMenu.add(aboutMenuItem);
        helpMenu.add(keyboardShortcutMenuItem);

        menubar.add(Box.createHorizontalGlue());
        menubar.add(helpMenu);
    }

    private void openSettingsFromFile() {
        var newSaveLocation = getFilePathFromUser(this, "Select file", FileAction.LOAD);
        if (newSaveLocation == null)
            return;
        Options.getInstance().setFilename(newSaveLocation);
        try (var is = Files.newInputStream(Options.getInstance().getFilename())) {
            Options.getInstance().load(is);
            Options.getInstance().setSaveDir(newSaveLocation.getParent());
            optionsWindow.updateOptions();
            updateTitle();
            SwingUtilities.invokeLater(() -> {
                agentPanel.reset();
                agentPanel.init();
                agentPanel.initMusicians();
            });
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(this, e1.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
        } catch (BackingStoreException e) {
            LOGGER.atError().log("Error writing save directory to preferences: {}", e.getMessage());
        }
    }

    private void saveSettingsToFile() {
        var options = Options.getInstance();
        if (Options.getInstance().getFilename() == null) {
            Options.getInstance().setFilename(getFilePathFromUser(this, "Select Save location", FileAction.SAVE));
        }
        if (Options.getInstance().getFilename() == null)
            return;
        try {
            var filenameStr = Options.getInstance().getFilename().toString();
            if (filenameStr.lastIndexOf('.') == -1)
                Options.getInstance()
                        .setFilename(Options.getInstance().getFilename().resolveSibling(filenameStr + ".margia"));
            try (var os = Files.newOutputStream(Options.getInstance().getFilename())) {
                options.store(os);
            }
            updateTitle();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(this, e1.getMessage(), "Error saving file", JOptionPane.ERROR_MESSAGE);
        }
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
    public Path getFilePathFromUser(JFrame parent, String dialogTitle, FileAction action) {
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setCurrentDirectory(Options.getInstance().getSaveDir().toFile());
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (action == FileAction.LOAD)
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MARGIA files", "margia"));

        int returnValue;
        if (action == FileAction.SAVE) {
            returnValue = fileChooser.showSaveDialog(parent);
        } else {
            returnValue = fileChooser.showOpenDialog(parent);
        }

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.getSelectedFile();
            try {
                Options.getInstance().setSaveDir(selectedFile.getParentFile().toPath());
            } catch (BackingStoreException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error saving preferences",
                        JOptionPane.WARNING_MESSAGE);
            }
            return selectedFile.toPath();
        }
        // If the user cancels or an error occurs
        return null;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String key, Object value) && key.equals(Options.DIRTY_PROPERTY)
                && (boolean) value) {
            updateTitle();
        }
    }

    private void updateTitle() {
        var pathStr = Options.getInstance().getFilename() == null ? ""
                : ("- " + Options.getInstance().getFilename().toString());
        var windowTitle = String.format("MARGIA %s%s", pathStr, Options.getInstance().isDirty() ? " *" : "");
        setTitle(windowTitle);
    }

}
