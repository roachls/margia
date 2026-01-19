package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;

import javax.imageio.ImageIO;
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
    private static final int TOOLBAR_ICON_SIZE = 24;
    private static final int MENU_ICON_SIZE = 18;
    private static final String UNLOCK_ICON_DESCRIPTION = "an open lock";
    private static final String LOCK_ICON_DESCRIPTION = "a closed lock";
    private static final String CONNECT_ICON_DESCRIPTION = "two dots with a line between them";
    private static final String CONNECT_ICON = "/icons/connect.png";
    private static final String UNLOCK_ICON = "/icons/unlock.png";
    private static final String LOCK_ICON = "/icons/lock.png";
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
        var openMenuItem = new JMenuItem("Open", createImageIcon("/icons/open.png", "an open folder", 18));
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.addActionListener(_ -> openSettingsFromFile());

        var saveMenuItem = new JMenuItem("Save", createImageIcon("/icons/save.png", "a floppy disk", 18));
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(_ -> saveSettingsToFile());

        var saveAsMenuItem = new JMenuItem("Save As... (" + CONTROL_TEXT + "+S)",
                createImageIcon("/icons/save.png", "a floppy disk", 18));
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
        var showOptionPaneMenuItem = new JCheckBoxMenuItem("Show Options",
                createImageIcon("/icons/options.png", "an Options icon", 18));
        showOptionPaneMenuItem.setMnemonic(KeyEvent.VK_O);
        showOptionPaneMenuItem.addActionListener(_ -> optionPanel.setVisible(showOptionPaneMenuItem.isSelected()));
        editMenu.add(showOptionPaneMenuItem);

        var copyMenuItem = new JMenuItem("Copy (" + CONTROL_TEXT + "+C)",
                createImageIcon("/icons/copy.png", "two clipboards", 18));
        copyMenuItem.setMnemonic(KeyEvent.VK_C);
        copyMenuItem.addActionListener(_ -> agentPanel.copySelectedComponents());
        editMenu.add(copyMenuItem);
        var pasteMenuItem = new JMenuItem("Paste (" + CONTROL_TEXT + "+V)",
                createImageIcon("/icons/paste.png", "a clipboard", 18));
        pasteMenuItem.setMnemonic(KeyEvent.VK_P);
        pasteMenuItem.addActionListener(_ -> agentPanel.paste());
        editMenu.add(pasteMenuItem);

        setupModesMenu(editMenu);

        var selectAll = new JMenuItem("Select All (" + CONTROL_TEXT + "+A)");
        selectAll.setMnemonic(KeyEvent.VK_S);
        selectAll.addActionListener(_ -> agentPanel.selectAll());
        editMenu.add(selectAll);
        var deselectAll = new JMenuItem("Deselect All (" + KeyEvent.getKeyText(KeyEvent.VK_ESCAPE) + ")");
        deselectAll.setMnemonic(KeyEvent.VK_D);
        deselectAll.addActionListener(_ -> agentPanel.deselectAll());
        editMenu.add(deselectAll);
        var muteSelected = new JMenuItem("Mute selected",
                createImageIcon("/icons/mute.png", "a speaker that is muted", 18));
        muteSelected.setMnemonic(KeyEvent.VK_U);
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        editMenu.add(muteSelected);
        var unmuteSelected = new JMenuItem("Unmute selected",
                createImageIcon("/icons/unmute.png", "a speaker that is unmuted", 18));
        unmuteSelected.setMnemonic(KeyEvent.VK_E);
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        editMenu.add(unmuteSelected);
        var lockSelected = new JMenuItem("Lock selected", createImageIcon(LOCK_ICON, LOCK_ICON_DESCRIPTION, 18));
        lockSelected.setMnemonic(KeyEvent.VK_L);
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        editMenu.add(lockSelected);
        var unlockSelected = new JMenuItem("Unlock selected",
                createImageIcon(UNLOCK_ICON, UNLOCK_ICON_DESCRIPTION, 18));
        unlockSelected.setMnemonic(KeyEvent.VK_N);
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        editMenu.add(unlockSelected);
        var lockAll = new JMenuItem("Lock all", createImageIcon("/icons/lock_all.png", LOCK_ICON_DESCRIPTION, 18));
        lockAll.addActionListener(_ -> agentPanel.lockAll());
        editMenu.add(lockAll);
        var unlockAll = new JMenuItem("Unlock all",
                createImageIcon("/icons/unlock_all.png", UNLOCK_ICON_DESCRIPTION, MENU_ICON_SIZE));
        unlockAll.addActionListener(_ -> agentPanel.unlockAll());
        editMenu.add(unlockAll);
        var deleteSelected = new JMenuItem("Remove selected (" + KeyEvent.getKeyText(KeyEvent.VK_DELETE) + ")",
                createImageIcon("/icons/delete.png", "an large capital X", MENU_ICON_SIZE));
        deleteSelected.setMnemonic(KeyEvent.VK_R);
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        editMenu.add(deleteSelected);
        var connectSelected = new JMenuItem("Connect selected",
                createImageIcon(CONNECT_ICON, CONNECT_ICON_DESCRIPTION, MENU_ICON_SIZE));
        connectSelected.setMnemonic(KeyEvent.VK_T);
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        editMenu.add(connectSelected);
        var disconnectSelected = new JMenuItem("Disconnect selected",
                createImageIcon("/icons/disconnect.png", "two dots with a broken line between them", MENU_ICON_SIZE));
        disconnectSelected.setMnemonic(KeyEvent.VK_I);
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());
        editMenu.add(disconnectSelected);
        menubar.add(editMenu);
    }

    private JToolBar createToolPalette() {
        var toolbar = new JToolBar("Palette");
        toolbar.setFloatable(true);
        var copyBtn = new JButton(createImageIcon("/icons/copy.png", "two clipboards", TOOLBAR_ICON_SIZE));
        copyBtn.setToolTipText("Copy selected");
        copyBtn.setBorder(BorderFactory.createEmptyBorder());
        copyBtn.addActionListener(_ -> agentPanel.copySelectedComponents());

        var pasteBtn = new JButton(createImageIcon("/icons/paste.png", "a clipboard", TOOLBAR_ICON_SIZE));
        pasteBtn.setToolTipText("Paste");
        pasteBtn.setBorder(BorderFactory.createEmptyBorder());
        pasteBtn.addActionListener(_ -> agentPanel.paste());

//        setupModesMenu(editMenu);
//
        var muteSelected = new JButton(
                createImageIcon("/icons/mute.png", "a speaker that is muted", TOOLBAR_ICON_SIZE));
        muteSelected.setBorder(BorderFactory.createEmptyBorder());
        muteSelected.setToolTipText("Mute selected");
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        var unmuteSelected = new JButton(
                createImageIcon("/icons/unmute.png", "a speaker that is unmuted", TOOLBAR_ICON_SIZE));
        unmuteSelected.setBorder(BorderFactory.createEmptyBorder());
        unmuteSelected.setToolTipText("Unmute selected");
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        var lockSelected = new JButton(createImageIcon(LOCK_ICON, LOCK_ICON_DESCRIPTION, TOOLBAR_ICON_SIZE));
        lockSelected.setBorder(BorderFactory.createEmptyBorder());
        lockSelected.setToolTipText("Lock selected");
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        var unlockSelected = new JButton(createImageIcon(UNLOCK_ICON, UNLOCK_ICON_DESCRIPTION, TOOLBAR_ICON_SIZE));
        unlockSelected.setBorder(BorderFactory.createEmptyBorder());
        unlockSelected.setToolTipText("Unlock selected");
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        var lockAll = new JButton(createImageIcon("/icons/lock_all.png", LOCK_ICON_DESCRIPTION, TOOLBAR_ICON_SIZE));
        lockAll.setBorder(BorderFactory.createEmptyBorder());
        lockAll.setToolTipText("Lock all");
        lockAll.addActionListener(_ -> agentPanel.lockAll());
        var unlockAll = new JButton(
                createImageIcon("/icons/unlock_all.png", UNLOCK_ICON_DESCRIPTION, TOOLBAR_ICON_SIZE));
        unlockAll.setBorder(BorderFactory.createEmptyBorder());
        unlockAll.setToolTipText("Unlock all");
        unlockAll.addActionListener(_ -> agentPanel.unlockAll());
        var deleteSelected = new JButton(createImageIcon("/icons/delete.png", "an large capital X", TOOLBAR_ICON_SIZE));
        deleteSelected.setBorder(BorderFactory.createEmptyBorder());
        deleteSelected.setToolTipText("Delete selected");
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        var connectSelected = new JButton(createImageIcon(CONNECT_ICON, CONNECT_ICON_DESCRIPTION, TOOLBAR_ICON_SIZE));
        connectSelected.setBorder(BorderFactory.createEmptyBorder());
        connectSelected.setToolTipText("Connect selected");
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        var disconnectSelected = new JButton(createImageIcon("/icons/disconnect.png",
                "two dots with a broken line between them", TOOLBAR_ICON_SIZE));
        disconnectSelected.setBorder(BorderFactory.createEmptyBorder());
        disconnectSelected.setToolTipText("Disconnect selected");
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());

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
        toolbar.add(Box.createHorizontalGlue());
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

    private void setupModesMenu(JMenu editMenu) {
        var modesMenu = new JMenu("Mode");
        modesMenu.setMnemonic(KeyEvent.VK_M);
        var selectMode = new JRadioButtonMenuItem("Select Musician(s)",
                createImageIcon("/icons/select.png", "a hand with the index finger pointing", MENU_ICON_SIZE));
        selectMode.setMnemonic(KeyEvent.VK_S);
        selectMode.setName(EditMode.SELECT.name());
        var addMode = new JRadioButtonMenuItem("Add Musician(s)",
                createImageIcon("/icons/add.png", "an outline of a person with a plus symbol", MENU_ICON_SIZE));
        addMode.setMnemonic(KeyEvent.VK_A);
        addMode.setName(EditMode.ADD.name());
        var moveMode = new JRadioButtonMenuItem("Move Musician",
                createImageIcon("/icons/move.png", "a four-way arrow icon", MENU_ICON_SIZE));
        moveMode.setMnemonic(KeyEvent.VK_V);
        moveMode.setName(EditMode.MOVE.name());
        var connectMode = new JRadioButtonMenuItem("Connect/Disconnect two musicians",
                createImageIcon(CONNECT_ICON, CONNECT_ICON_DESCRIPTION, MENU_ICON_SIZE));
        connectMode.setMnemonic(KeyEvent.VK_C);
        connectMode.setName(EditMode.CONNECT.name());

        selectMode.setSelected(true);

        var group = new ButtonGroup();
        group.add(selectMode);
        modesMenu.add(selectMode);
        group.add(addMode);
        modesMenu.add(addMode);
        group.add(moveMode);
        modesMenu.add(moveMode);
        group.add(connectMode);
        modesMenu.add(connectMode);

        var modeChangeListener = new ModeChangeListener();
        selectMode.addChangeListener(modeChangeListener);
        addMode.addChangeListener(modeChangeListener);
        moveMode.addChangeListener(modeChangeListener);
        connectMode.addChangeListener(modeChangeListener);

        editMenu.add(modesMenu);
    }

    private class ModeChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof JRadioButtonMenuItem jtb && jtb.isSelected()) {
                var name = jtb.getName();
                var mode = EditMode.valueOf(name);
                agentPanel.setEditMode(mode);
            }
        }
    }

    private void setupHelpMenu(JMenuBar menubar) {
        var helpMenu = new JMenu("Help");
        helpMenu.setIcon(createImageIcon("/icons/help.png", "a question mark", MENU_ICON_SIZE));
        helpMenu.setMnemonic('H');
        var aboutMenuItem = new JMenuItem("About",
                createImageIcon("/icons/about.png", "an circle with the letter i for information", MENU_ICON_SIZE));
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

    private ImageIcon createImageIcon(String path, String description, int scale) {
        try (var resource = getClass().getResourceAsStream(path)) {
            var image = ImageIO.read(resource);
            var resizedImage = image.getScaledInstance(scale, scale, Image.SCALE_SMOOTH);
            return new ImageIcon(resizedImage, description);
        } catch (IOException e) {
            LOGGER.atError().withThrowable(e).log("Unable to load image file {}", path);
            return null;
        }
    }
}
