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
    private AgentPanel agentPanel;
    private String algorithmTitle;
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
        this.getContentPane().setLayout(new BorderLayout());
        setupMenu();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var transportPanel = new TransportPanel(timing, transport);
        getContentPane().add(transportPanel, BorderLayout.SOUTH);
        optionPanel = new OptionPanel();
        updateTitle();
        Options.getInstance().addChangeListener(MusicianComponent.SHOW_NUMBERS_PROPERTY,
                MusicianComponent.SHOW_NUMBERS_LISTENER);
        Options.getInstance().addChangeListener(Options.DIRTY_PROPERTY, this);
        agentPanel = new AgentPanel();
        agentPanel.addVetoableChangeListener(optionPanel);
        transportPanel.addTempoListener(agentPanel);
        getContentPane().add(agentPanel, BorderLayout.CENTER);
        getContentPane().add(optionPanel, BorderLayout.EAST);
        pack();
        optionPanel.setVisible(false);
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
        setupHelpMenu(menubar);
        this.setJMenuBar(menubar);
    }

    private void setupFileMenu(JMenuBar menubar) {
        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        var openMenuItem = new JMenuItem("Open", createImageIcon("/icons/open.png", "an open folder"));
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.addActionListener(_ -> openSettingsFromFile());

        var saveMenuItem = new JMenuItem("Save", createImageIcon("/icons/save.png", "a floppy disk"));
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(_ -> saveSettingsToFile());

        var saveAsMenuItem = new JMenuItem("Save As... (" + CONTROL_TEXT + "+S)",
                createImageIcon("/icons/save.png", "a floppy disk"));
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
                createImageIcon("/icons/options.png", "an Options icon"));
        showOptionPaneMenuItem.setMnemonic(KeyEvent.VK_O);
        showOptionPaneMenuItem.addActionListener(_ -> optionPanel.setVisible(showOptionPaneMenuItem.isSelected()));
        editMenu.add(showOptionPaneMenuItem);

        var copyMenuItem = new JMenuItem("Copy (" + CONTROL_TEXT + "+C)",
                createImageIcon("/icons/copy.png", "two clipboards"));
        copyMenuItem.setMnemonic(KeyEvent.VK_C);
        copyMenuItem.addActionListener(_ -> agentPanel.copySelectedComponents());
        editMenu.add(copyMenuItem);
        var pasteMenuItem = new JMenuItem("Paste (" + CONTROL_TEXT + "+V)",
                createImageIcon("/icons/paste.png", "a clipboard"));
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
                createImageIcon("/icons/mute.png", "a speaker that is muted"));
        muteSelected.setMnemonic(KeyEvent.VK_U);
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        editMenu.add(muteSelected);
        var unmuteSelected = new JMenuItem("Unmute selected",
                createImageIcon("/icons/unmute.png", "a speaker that is unmuted"));
        unmuteSelected.setMnemonic(KeyEvent.VK_E);
        unmuteSelected.addActionListener(_ -> agentPanel.unmuteSelected());
        editMenu.add(unmuteSelected);
        var lockSelected = new JMenuItem("Lock selected", createImageIcon("/icons/lock.png", "a closed lock"));
        lockSelected.setMnemonic(KeyEvent.VK_L);
        lockSelected.addActionListener(_ -> agentPanel.lockSelected());
        editMenu.add(lockSelected);
        var unlockSelected = new JMenuItem("Unlock selected", createImageIcon("/icons/unlock.png", "an open lock"));
        unlockSelected.setMnemonic(KeyEvent.VK_N);
        unlockSelected.addActionListener(_ -> agentPanel.unlockSelected());
        editMenu.add(unlockSelected);
        var deleteSelected = new JMenuItem("Remove selected (" + KeyEvent.getKeyText(KeyEvent.VK_DELETE) + ")",
                createImageIcon("/icons/delete.png", "an large capital X"));
        deleteSelected.setMnemonic(KeyEvent.VK_R);
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        editMenu.add(deleteSelected);
        var connectSelected = new JMenuItem("Connect selected",
                createImageIcon("/icons/connect.png", "two dots with a line between them"));
        connectSelected.setMnemonic(KeyEvent.VK_T);
        connectSelected.addActionListener(_ -> agentPanel.connectSelected());
        editMenu.add(connectSelected);
        var disconnectSelected = new JMenuItem("Disconnect selected",
                createImageIcon("/icons/disconnect.png", "two dots with a broken line between them"));
        disconnectSelected.setMnemonic(KeyEvent.VK_I);
        disconnectSelected.addActionListener(_ -> agentPanel.disconnectSelected());
        editMenu.add(disconnectSelected);
        menubar.add(editMenu);
    }

    private void setupModesMenu(JMenu editMenu) {
        var modesMenu = new JMenu("Mode");
        modesMenu.setMnemonic(KeyEvent.VK_M);
        var selectMode = new JRadioButtonMenuItem("Select Musician(s)",
                createImageIcon("/icons/select.png", "a hand with the index finger pointing"));
        selectMode.setMnemonic(KeyEvent.VK_S);
        selectMode.setName(EditMode.SELECT.name());
        var addMode = new JRadioButtonMenuItem("Add Musician(s)",
                createImageIcon("/icons/add.png", "an outline of a person with a plus symbol"));
        addMode.setMnemonic(KeyEvent.VK_A);
        addMode.setName(EditMode.ADD.name());
        var moveMode = new JRadioButtonMenuItem("Move Musician",
                createImageIcon("/icons/move.png", "a four-way arrow icon"));
        moveMode.setMnemonic(KeyEvent.VK_V);
        moveMode.setName(EditMode.MOVE.name());
        var connectMode = new JRadioButtonMenuItem("Connect/Disconnect two musicians",
                createImageIcon("/icons/connect.png", "two dots with a line between them"));
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
        helpMenu.setIcon(createImageIcon("/icons/help.png", "a question mark"));
        helpMenu.setMnemonic('H');
        var aboutMenuItem = new JMenuItem("About",
                createImageIcon("/icons/about.png", "an circle with the letter i for information"));
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
            optionPanel.updateOptions();
            updateTitle();
            SwingUtilities.invokeLater(() -> {
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
        if (e.getSource() instanceof ChangeSource cs && cs.key().equals(Options.DIRTY_PROPERTY)) {
            updateTitle();
        }
    }

    private void updateTitle() {
        var pathStr = Options.getInstance().getFilename() == null ? ""
                : ("- " + Options.getInstance().getFilename().toString());
        var windowTitle = String.format("MARGIA %s%s%s", algorithmTitle, pathStr,
                Options.getInstance().isDirty() ? " *" : "");
        setTitle(windowTitle);
    }

    private ImageIcon createImageIcon(String path, String description) {
        try (var resource = getClass().getResourceAsStream(path)) {
            var image = ImageIO.read(resource);
            var resizedImage = image.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            return new ImageIcon(resizedImage, description);
        } catch (IOException e) {
            LOGGER.atError().withThrowable(e).log("Unable to load image file {}", path);
            return null;
        }
    }
}
