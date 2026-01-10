package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.*;
import org.roach.margia.timing.TimingSource;
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
    private ToolPanel toolPanel;

    /**
     * @param title     window title
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     * @param musicians the musicians to display
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()} returns
     *                           true
     */
    public MargiaWindow(String title, TimingSource timing, Transport transport, List<Musician> musicians)
            throws HeadlessException {
        super();
        this.algorithmTitle = title;
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
        agentPanel = new AgentPanel(musicians);
        agentPanel.addVetoableChangeListener(optionPanel);
        transportPanel.addTempoListener(agentPanel);
        getContentPane().add(agentPanel, BorderLayout.CENTER);
        getContentPane().add(optionPanel, BorderLayout.EAST);
        toolPanel = new ToolPanel(agentPanel);
        getContentPane().add(toolPanel, BorderLayout.WEST);
        pack();
        optionPanel.setVisible(false);
        toolPanel.setVisible(false);
        SwingUtilities.invokeLater(() -> agentPanel.initMusicians());
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
        openMenuItem.addActionListener(this::openSettingsFromFile);

        var saveMenuItem = new JMenuItem("Save", createImageIcon("/icons/save.png", "a floppy disk"));
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(this::saveSettingsToFile);

        var saveAsMenuItem = new JMenuItem("Save As...", createImageIcon("/icons/save.png", "a floppy disk"));
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.addActionListener(e -> {
            Options.getInstance().setFilename(null);
            saveSettingsToFile(e);
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
        var showToolPaneMenuItem = new JCheckBoxMenuItem("Show Tools",
                createImageIcon("/icons/tools.png", "an icon of a wrench and screwdriver"));
        showToolPaneMenuItem.setMnemonic(KeyEvent.VK_T);
        showToolPaneMenuItem.addActionListener(_ -> toolPanel.setVisible(showToolPaneMenuItem.isSelected()));
        editMenu.add(showOptionPaneMenuItem);
        editMenu.add(showToolPaneMenuItem);
        var selectAll = new JMenuItem("Select All");
        selectAll.setMnemonic(KeyEvent.VK_S);
        selectAll.addActionListener(_ -> agentPanel.selectAll());
        editMenu.add(selectAll);
        var deselectAll = new JMenuItem("Deselect All");
        deselectAll.setMnemonic(KeyEvent.VK_D);
        deselectAll.addActionListener(_ -> agentPanel.deselectAll());
        editMenu.add(deselectAll);
        var muteSelected = new JMenuItem("Mute selected",
                createImageIcon("/icons/mute.png", "a speaker that is muted"));
        muteSelected.setMnemonic(KeyEvent.VK_M);
        muteSelected.addActionListener(_ -> agentPanel.muteSelected());
        editMenu.add(muteSelected);
        var unmuteSelected = new JMenuItem("Unmute selected",
                createImageIcon("/icons/unmute.png", "a speaker that is unmuted"));
        unmuteSelected.setMnemonic(KeyEvent.VK_U);
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
        var deleteSelected = new JMenuItem("Remove (delete) selected", createImageIcon("/icons/delete.png", "an large capital X"));
        deleteSelected.setMnemonic(KeyEvent.VK_R);
        deleteSelected.addActionListener(_ -> agentPanel.deleteSelected());
        editMenu.add(deleteSelected);
        menubar.add(editMenu);
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

    private void openSettingsFromFile(ActionEvent e) {
        var newSaveLocation = getFilePathFromUser(this, "Select file", FileAction.LOAD);
        if (newSaveLocation == null)
            return;
        Options.getInstance().setFilename(newSaveLocation);
        try (var is = Files.newInputStream(Options.getInstance().getFilename())) {
            Options.getInstance().load(is);
            optionPanel.updateOptions();
            updateTitle();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(this, e1.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettingsToFile(ActionEvent e) {
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
                options.store(os, "MARGIA");
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
