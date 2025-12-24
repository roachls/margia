package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.Transport;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * The main program window
 */
public class MargiaWindow extends JFrame implements ChangeListener {
    private static final String ABOUT_MESSAGE = """
            <html>
            <h1>MARGIA</h1>

            <h3>The <u>M</u>usic <u>a</u>nd <u>R</u>hythm <u>G</u>enerating <u>I</u>ntelligent <u>A</u>gents</h3>

            <h3>By Stevie Roach</h3>
            <a href="mailto:roachls@yahoo.com">roachls@yahoo.com</a>
            """;
    private OptionPanel optionPanel;
    private AgentPanel agentPanel;
    private Path saveDir;
    private Path filename;
    private String title;
    private Preferences preferences;

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
        this.title = title;
        preferences = Preferences.userNodeForPackage(getClass());
        this.saveDir = Path.of(preferences.get("saveDir", System.getProperty("user.home")));
        this.getContentPane().setLayout(new BorderLayout());
        setupMenu();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var transportPanel = new TransportPanel(timing, transport);
        getContentPane().add(transportPanel, BorderLayout.SOUTH);
        optionPanel = new OptionPanel();
        updateTitle();
        optionPanel.addChangeListener(MusicianComponent.SHOW_NUMBERS_PROPERTY, MusicianComponent.SHOW_NUMBERS_LISTENER);
        optionPanel.addChangeListener(OptionPanel.DIRTY_PROPERTY, this);
        agentPanel = new AgentPanel(musicians, optionPanel);
        transportPanel.addTempoListener(agentPanel);
        getContentPane().add(agentPanel, BorderLayout.CENTER);
        getContentPane().add(optionPanel, BorderLayout.EAST);
        pack();
        optionPanel.setVisible(false);
        SwingUtilities.invokeLater(() -> agentPanel.initMusicians());
    }

    private void setupMenu() {
        var menubar = new JMenuBar();

        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        var openMenuItem = new JMenuItem("Open");
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.addActionListener(this::openSettingsFromFile);

        var saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.addActionListener(this::saveSettingsToFile);

        var saveAsMenuItem = new JMenuItem("Save As...");
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.addActionListener(e -> {
            this.filename = null;
            saveSettingsToFile(e);
        });

        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        menubar.add(fileMenu);

        // setup global actions
        JComponent component = getRootPane();

        KeyStroke ctrlSKeyStroke = KeyStroke.getKeyStroke("control s");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlSKeyStroke, "save");
        component.getActionMap().put("save", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettingsToFile(e);
            }

        });

        KeyStroke ctrlOKeyStroke = KeyStroke.getKeyStroke("control o");
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlOKeyStroke, "open");
        component.getActionMap().put("open", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openSettingsFromFile(e);
            }

        });

        var editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        var showOptionPaneMenuItem = new JCheckBoxMenuItem("Show Options");
        showOptionPaneMenuItem.setMnemonic(KeyEvent.VK_O);
        showOptionPaneMenuItem.addActionListener(_ -> optionPanel.setVisible(showOptionPaneMenuItem.isSelected()));
        editMenu.add(showOptionPaneMenuItem);
        menubar.add(editMenu);

        var helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        var aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, ABOUT_MESSAGE));
        helpMenu.add(aboutMenuItem);

        menubar.add(Box.createHorizontalGlue());
        menubar.add(helpMenu);
        this.setJMenuBar(menubar);
    }

    private void openSettingsFromFile(ActionEvent e) {
        var newSaveLocation = getFilePathFromUser(this, "Select file", FileAction.LOAD);
        if (newSaveLocation == null)
            return;
        this.filename = newSaveLocation;
        var opts = new Properties();
        try {
            opts.load(Files.newInputStream(filename));
            optionPanel.setOptions(opts);
            optionPanel.setDirty(false);
            updateTitle();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(this, e1.getMessage(), "Error loading file", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettingsToFile(ActionEvent e) {
        var options = optionPanel.getOptions();
        System.out.println("saving options:" + options);
        if (this.filename == null) {
            this.filename = getFilePathFromUser(this, "Select Save location", FileAction.SAVE);
        }
        if (this.filename == null)
            return;
        try {
            var filenameStr = filename.toString();
            if (filenameStr.lastIndexOf('.') == -1)
                filename = filename.resolveSibling(filenameStr + ".margia");
            options.store(Files.newOutputStream(this.filename), "MARGIA");
            optionPanel.setDirty(false);
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setCurrentDirectory(saveDir.toFile());
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
            File selectedFile = fileChooser.getSelectedFile();
            saveDir = selectedFile.getParentFile().toPath();
            preferences.put("saveDir", saveDir.toString());
            try {
                preferences.flush();
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
        if (e.getSource() instanceof ChangeSource cs && cs.key().equals(OptionPanel.DIRTY_PROPERTY)) {
            updateTitle();
        }
    }

    private void updateTitle() {
        var pathStr = filename == null ? "" : "- " + filename.toString();
        var windowTitle = String.format("MARGIA %s%s%s", title, pathStr, optionPanel.isDirty() ? " *" : "");
        setTitle(windowTitle);
    }

}
