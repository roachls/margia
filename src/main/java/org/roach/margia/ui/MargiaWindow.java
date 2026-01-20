package org.roach.margia.ui;

import static org.roach.margia.ui.Icons.*;

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
import org.roach.margia.Transport;
import org.roach.margia.storage.Options;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.AgentPanel.EditMode;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

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
    private OptionPanel optionPanel;
    private OptionsWindow uiOptionsWindow;
    private AgentPanel agentPanel;
    private static final Logger LOGGER = LogManager.getLogger(MargiaWindow.class);
    // OS-specific control key (Ctrl for Windows, Option for Mac)
    private static final String CONTROL_TEXT = InputEvent.getModifiersExText(InputEvent.CTRL_DOWN_MASK);

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
     *                           true
     */
    public MargiaWindow(TimingSource timing, Transport transport) throws HeadlessException {
        super();

        getContentPane().setLayout(new BorderLayout());
        setupMenu();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var transportPanel = new TransportPanel(timing, transport);
        getContentPane().add(transportPanel, BorderLayout.SOUTH);
        optionPanel = new OptionPanel();
        uiOptionsWindow = new OptionsWindow();
        updateTitle();
        Options.getInstance().addChangeListener(MusicianComponent.SHOW_NUMBERS_PROPERTY,
                MusicianComponent.SHOW_NUMBERS_LISTENER);
        Options.getInstance().addChangeListener(Options.DIRTY_PROPERTY, this);
        agentPanel = new AgentPanel();
        agentPanel.setBounds(0, 0, 1000, 1000);
        agentPanel.addVetoableChangeListener(optionPanel);
        transportPanel.addTempoListener(agentPanel);
        getContentPane().add(agentPanel, BorderLayout.CENTER);
        getContentPane().add(optionPanel, BorderLayout.EAST);
        optionPanel.setVisible(false);

        var palette1 = createToolPalette();
        add(palette1, BorderLayout.PAGE_START);

        pack();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new AgentPanelKeyListener());
        SwingUtilities.invokeLater(() -> agentPanel.initMusicians());
    }

    private class AgentPanelKeyListener implements KeyEventDispatcher {

        @Override
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
                    agentPanel.copySelectedComponents();
                    complete = true;
                }
                break;
            case KeyEvent.VK_V:
                if (e.isControlDown()) {
                    agentPanel.paste();
                    complete = true;
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
        setupViewMenu(menubar);
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

        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        menubar.add(fileMenu);
    }

    private void setupEditMenu(JMenuBar menubar) {
        var editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        var showOptionPaneMenuItem = new JCheckBoxMenuItem("Show Options", getToolbarIcon(OPTIONS));
        showOptionPaneMenuItem.setMnemonic(KeyEvent.VK_O);
        showOptionPaneMenuItem.addActionListener(_ -> optionPanel.setVisible(showOptionPaneMenuItem.isSelected()));
        editMenu.add(showOptionPaneMenuItem);

        var copyMenuItem = new JMenuItem("Copy (" + CONTROL_TEXT + "+C)", getToolbarIcon(COPY));
        copyMenuItem.setMnemonic(KeyEvent.VK_C);
        copyMenuItem.addActionListener(_ -> agentPanel.copySelectedComponents());
        editMenu.add(copyMenuItem);
        var pasteMenuItem = new JMenuItem("Paste (" + CONTROL_TEXT + "+V)", getToolbarIcon(PASTE));
        pasteMenuItem.setMnemonic(KeyEvent.VK_P);
        pasteMenuItem.addActionListener(_ -> agentPanel.paste());
        editMenu.add(pasteMenuItem);

        var selectAll = new JMenuItem("Select All (" + CONTROL_TEXT + "+A)", getToolbarIcon(SELECT_ALL));
        selectAll.setMnemonic(KeyEvent.VK_S);
        selectAll.addActionListener(_ -> agentPanel.selectAll());
        editMenu.add(selectAll);
        var deselectAll = new JMenuItem("Deselect All (" + KeyEvent.getKeyText(KeyEvent.VK_ESCAPE) + ")",
                getToolbarIcon(DESELECT_ALL));
        deselectAll.setMnemonic(KeyEvent.VK_D);
        deselectAll.addActionListener(_ -> agentPanel.deselectAll());
        editMenu.add(deselectAll);
        var muteSelected = new JMenuItem("Mute selected", getToolbarIcon(MUTE));
        muteSelected.setMnemonic(KeyEvent.VK_U);
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        editMenu.add(muteSelected);
        var unmuteSelected = new JMenuItem("Unmute selected", getToolbarIcon(UNMUTE));
        unmuteSelected.setMnemonic(KeyEvent.VK_E);
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        editMenu.add(unmuteSelected);
        var lockSelected = new JMenuItem("Lock selected", getToolbarIcon(LOCK));
        lockSelected.setMnemonic(KeyEvent.VK_L);
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        editMenu.add(lockSelected);
        var unlockSelected = new JMenuItem("Unlock selected", getToolbarIcon(UNLOCK));
        unlockSelected.setMnemonic(KeyEvent.VK_N);
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        editMenu.add(unlockSelected);
        var lockAll = new JMenuItem("Lock all", getToolbarIcon(LOCK_ALL));
        lockAll.addActionListener(_ -> agentPanel.lockAll());
        editMenu.add(lockAll);
        var unlockAll = new JMenuItem("Unlock all", getToolbarIcon(UNLOCK_ALL));
        unlockAll.addActionListener(_ -> agentPanel.unlockAll());
        editMenu.add(unlockAll);
        var deleteSelected = new JMenuItem("Remove selected (" + KeyEvent.getKeyText(KeyEvent.VK_DELETE) + ")",
                getToolbarIcon(DELETE));
        deleteSelected.setMnemonic(KeyEvent.VK_R);
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        editMenu.add(deleteSelected);
        var connectSelected = new JMenuItem("Connect selected", getToolbarIcon(CONNECT));
        connectSelected.setMnemonic(KeyEvent.VK_T);
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        editMenu.add(connectSelected);
        var disconnectSelected = new JMenuItem("Disconnect selected", getToolbarIcon(DISCONNECT));
        disconnectSelected.setMnemonic(KeyEvent.VK_I);
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());
        editMenu.add(disconnectSelected);
        menubar.add(editMenu);
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
        setupModesToolbar(toolbar);
        return toolbar;
    }

    private void setupViewMenu(JMenuBar menuBar) {
        var viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        var showUiOptions = new JMenuItem("UI Options");
        showUiOptions.setMnemonic(KeyEvent.VK_U);
        showUiOptions.addActionListener(_ -> uiOptionsWindow.setVisible(true));
        viewMenu.add(showUiOptions);

        menuBar.add(viewMenu);
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
        helpMenu.add(aboutMenuItem);

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
            uiOptionsWindow.updateOptions();
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
