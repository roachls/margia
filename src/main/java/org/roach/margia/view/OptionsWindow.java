package org.roach.margia.view;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;
import javax.swing.tree.*;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;
import org.roach.margia.storage.params.*;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

@SuppressWarnings("java:S1948")
class OptionsWindow extends JDialog implements PropertyChangeListener {
    private static final String HEIGHT_PROPERTY = "_height";
    private static final String WIDTH_PROPERTY = "_width";
    private static final String Y_PROPERTY = "_y";
    private static final String X_PROPERTY = "_x";
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";

    private static final String MUSICIAN = "Musician ";
    private UiOptionsPanel uiPanel;
    private MiscPanel miscPanel;
    private MidiPanel midiPanel;
    private JTree tree;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode musiciansNode;
    private final Map<Integer, DefaultMutableTreeNode> musicianNodes = new HashMap<>();

    OptionsWindow(JFrame parent) {
        super(parent, "Options");
        setName(OPTIONS_WINDOW_NAME);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    var loc = getLocationOnScreen();
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + X_PROPERTY, Integer.toString(loc.x));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + Y_PROPERTY, Integer.toString(loc.y));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY,
                            Integer.toString(getBounds().width));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY,
                            Integer.toString(getBounds().height));
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY,
                        Integer.toString(getBounds().width));
                Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY,
                        Integer.toString(getBounds().height));
            }
        });

        createUi();
        var x = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + X_PROPERTY, 100);
        var y = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + Y_PROPERTY, 100);
        var w = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + WIDTH_PROPERTY, 100);
        var h = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + HEIGHT_PROPERTY, 100);
        this.setLocation(x, y);
        this.setSize(w, h);
        setVisible(Persistence.getInstance().getBoolean(OptionsWindow.OPTIONS_WINDOW_NAME + "_visible", false));
    }

    private void createUi() {
        AtomicReference<JPanel> selectedPanel = new AtomicReference<>();
        setLayout(new BorderLayout());
        root = new DefaultMutableTreeNode("root");
        uiPanel = new UiOptionsPanel();
        var uiOptions = new DefaultMutableTreeNode(uiPanel);
        midiPanel = new MidiPanel();
        var midiOptions = new DefaultMutableTreeNode(midiPanel);
        miscPanel = new MiscPanel();
        var miscOptions = new DefaultMutableTreeNode(miscPanel);
        musiciansNode = new DefaultMutableTreeNode("Musicians");
        for (var musicianId : Options.getInstance().getMusicians().keySet()) {
            var mcPanel = new MusicianUiPanel(musicianId,
                    Options.getInstance().getUiOptions().getMusicianComponents().get(musicianId));
            var musicPanel = new MusicianMusicalOptionsPanel(Options.getInstance().getMusicians().get(musicianId));
            var rulePanel = new MusicianRuleOptionsPanel(musicianId,
                    Options.getInstance().getMusicians().get(musicianId).getRuleOptions());
            var optionsPanel = new MusicianOptionsPanel(mcPanel, musicPanel, rulePanel);
            var musicianNode = new DefaultMutableTreeNode(optionsPanel);
            musicianNodes.put(musicianId, musicianNode);
            musiciansNode.add(musicianNode);
        }

        root.add(uiOptions);
        root.add(midiOptions);
        root.add(miscOptions);
        root.add(musiciansNode);
        tree = new JTree(root);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        selectedPanel.set(uiPanel);
        add(uiPanel, BorderLayout.CENTER);
        tree.addTreeSelectionListener(e -> {
            remove(selectedPanel.get());
            var selectedPathCount = tree.getSelectionCount();
            final var warningPanel = new WarningPanel();
            if (selectedPathCount == 1) {
                var path = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                var userObject = path.getUserObject();
                if (userObject instanceof JPanel panel) {
                    selectedPanel.set(panel);
                }
                add(selectedPanel.get(), BorderLayout.CENTER);
            } else if (selectedPathCount > 1) {
                var selectedPaths = tree.getSelectionPaths();
                var firstObjectType = ((DefaultMutableTreeNode) selectedPaths[0].getLastPathComponent()).getUserObject()
                        .getClass();
                var areAllPathsOfSameType = Arrays.stream(selectedPaths)
                        .map(p -> (DefaultMutableTreeNode) p.getLastPathComponent()).map(p -> p.getUserObject())
                        .map(Object::getClass).allMatch(firstObjectType::equals);
                if (!areAllPathsOfSameType) {
                    selectedPanel.set(warningPanel);
                    add(warningPanel, BorderLayout.CENTER);
                } else {
                    setupMultipleSelection(selectedPaths, selectedPanel);
                }
            }
            revalidate();
            repaint();
        });
        var treePane = new JPanel(new BorderLayout());
        treePane.setBorder(BorderFactory.createEtchedBorder());
        treePane.add(new JScrollPane(tree), BorderLayout.CENTER);
        add(treePane, BorderLayout.WEST);
        tree.setRootVisible(false);

        pack();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        tree.clearSelection();
        var selectedMusicians = (List<Musician>) e.getNewValue();
        var paths = selectedMusicians.stream().map(Musician::getId)
                .map(id -> new TreePath(new Object[] { root, musiciansNode, musicianNodes.get(id) })).toList();
        tree.setSelectionPaths(paths.toArray(new TreePath[0]));
        if (!paths.isEmpty())
            tree.scrollPathToVisible(paths.get(0));
    }

    private void setupMultipleSelection(TreePath[] selectedPaths, AtomicReference<JPanel> selectedPanel) {
        if (!(selectedPaths[0].getLastPathComponent() instanceof DefaultMutableTreeNode))
            return;
        var userObjects = Arrays.stream(selectedPaths)
                .map(sp -> ((DefaultMutableTreeNode) sp.getLastPathComponent()).getUserObject()).toList();
        if (userObjects.get(0) instanceof MusicianOptionsPanel) {
            var mups = userObjects.stream().map(MusicianOptionsPanel.class::cast).toList();
            setupMultipleMusicianUi(mups, selectedPanel);
        }
    }

    private void setupMultipleMusicianUi(List<MusicianOptionsPanel> mups, AtomicReference<JPanel> selectedPanel) {
        var multipleEditor = new MusicianOptionsPanel(mups);
        selectedPanel.set(multipleEditor);
        add(multipleEditor, BorderLayout.CENTER);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_visible", Boolean.toString(this.isVisible()));
    }

    private static class WarningPanel extends JPanel {
        WarningPanel() {
            super(new BorderLayout());
            var warning = new JLabel("All selected objects must be of same type");
            warning.setHorizontalAlignment(SwingConstants.CENTER);
            var font = warning.getFont();
            var boldFont = font.deriveFont(Font.BOLD);
            warning.setFont(boldFont);
            add(warning, BorderLayout.CENTER);
        }
    }

    private class UiOptionsPanel extends JPanel {
        private final JSpinner gravity;
        private final JSpinner windSpeed;
        private final JSpinner edgeLength;
        private final JCheckBox showIcons;
        private final JCheckBox animateBackground;

        UiOptionsPanel() {
            setName("UI Options");
            setBorder(BorderFactory.createTitledBorder("UI Options"));
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            gravity = createSpinner("Gravity", Options.getInstance().getUiOptions().getGravity(), -20.0, 20.0, 0.1);
            gravity.addChangeListener(
                    _ -> Options.getInstance().getUiOptions().setGravity((double) gravity.getValue()));
            var gravityLabel = createLabelFor("Gravitational Constant", gravity);
            windSpeed = createSpinner("Wind speed (clockwise)", Options.getInstance().getUiOptions().getWindSpeed(),
                    -200.0, 200.0, 0.1);
            windSpeed.addChangeListener(
                    _ -> Options.getInstance().getUiOptions().setWindSpeed((double) windSpeed.getValue()));
            var windSpeedLabel = createLabelFor("WindSpeed", windSpeed);
            edgeLength = createSpinner(UiOptions.EDGE_LENGTH_PROPERTY,
                    Options.getInstance().getUiOptions().getEdgeLength(), 20, 300, 1);
            edgeLength.addChangeListener(
                    _ -> Options.getInstance().getUiOptions().setEdgeLength((int) edgeLength.getValue()));
            var edgeLengthLabel = createLabelFor("Edge length", edgeLength);
            showIcons = new JCheckBox("Show icons");
            showIcons.setToolTipText("Show/hide additional information on musicians");
            showIcons.setSelected(Options.getInstance().getUiOptions().isShowNumbers());
            showIcons.addActionListener(
                    _ -> Options.getInstance().getUiOptions().setShowNumbers(showIcons.isSelected()));
            animateBackground = new JCheckBox("Animate background");
            animateBackground.setToolTipText("Animate background");
            animateBackground.setSelected(Options.getInstance().getUiOptions().isAnimateBackground());
            animateBackground.addActionListener(
                    _ -> Options.getInstance().getUiOptions().setAnimateBackground(animateBackground.isSelected()));

            var row = 0;
            c.gridx = 0;
            c.gridy = row++;
            add(gravityLabel, c);
            c.gridx = 1;
            add(gravity, c);
            c.gridx = 0;
            c.gridy = row++;
            add(windSpeedLabel, c);
            c.gridx = 1;
            add(windSpeed, c);
            c.gridx = 0;
            c.gridy = row++;
            add(edgeLengthLabel, c);
            c.gridx = 1;
            add(edgeLength, c);
            c.gridy = row++;
            add(showIcons, c);
            c.gridy = row++;
            add(animateBackground, c);

            /*
             * Add a "filler" component to absorb extra vertical space This pushes all
             * previous components to the top of the container
             */
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        @Override
        public String toString() {
            return "UI";
        }
    }

    private class MiscPanel extends JPanel {
        private final JSpinner randomSeedSpinner;

        MiscPanel() {
            setBorder(BorderFactory.createTitledBorder("Miscellaneous Options"));
            setName("Misc Options");
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            randomSeedSpinner = createSpinner("Random seed", Options.getInstance().getRandomSeed(), -Long.MAX_VALUE,
                    Long.MAX_VALUE, 1L);
            JComponent editor = randomSeedSpinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor defEditor) {
                JFormattedTextField textField = defEditor.getTextField();
                textField.setColumns(15); // Set width to 3 columns
            }
            var randomSeedLabel = createLabelFor("Random seed", randomSeedSpinner);
            randomSeedSpinner.addChangeListener(
                    _ -> Options.getInstance().setRandomSeed(((Double) randomSeedSpinner.getValue()).longValue()));

            c.gridx = 0;
            c.gridy = 0;
            add(randomSeedLabel, c);
            c.gridx = 1;
            add(randomSeedSpinner, c);

            /*
             * Add a "filler" component to absorb extra vertical space This pushes all
             * previous components to the top of the container
             */
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        @Override
        public String toString() {
            return "Miscellaneous";
        }
    }

    private class MidiPanel extends JPanel {
        private final JCheckBox external;
        private final JCheckBox sendMidiTimecode;
        private final JCheckBox autoStartOnNoteOn;
        private final JCheckBox sendPanMessage;
        private final JSpinner panController;
        private final JCheckBox panWithRelativeLocations;
        private final JCheckBox sendVerticalPanMessage;
        private final JSpinner verticalPanController;
        private final JCheckBox verticalPanWithRelativeLocations;

        MidiPanel() {
            setBorder(BorderFactory.createTitledBorder("MIDI Options"));
            setName("MIDI Options");
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            external = new JCheckBox("Use External MIDI");
            external.setSelected(Options.getInstance().getMidiOptions().isUsingExternalMidi());
            sendMidiTimecode = new JCheckBox("Send MIDI Timecode");
            sendMidiTimecode.addActionListener(
                    _ -> Options.getInstance().getMidiOptions().setSendingMidiTimecode(sendMidiTimecode.isSelected()));
            sendMidiTimecode.setEnabled(external.isSelected());
            autoStartOnNoteOn = new JCheckBox("Auto-start on NOTE_ON event");
            autoStartOnNoteOn.addActionListener(
                    _ -> Options.getInstance().getMidiOptions().setAutoStartOnNoteOn(autoStartOnNoteOn.isSelected()));
            autoStartOnNoteOn.setEnabled(external.isSelected());

            sendPanMessage = new JCheckBox("Send stereo panning");
            sendPanMessage.addActionListener(
                    _ -> Options.getInstance().getMidiOptions().setSendPanMessage(sendPanMessage.isSelected()));
            sendPanMessage.setEnabled(external.isSelected());

            panController = createSpinner("Pan controller", MidiOptions.DEFAULT_PAN_CONTROLLER, 0, 127, 1);
            panController.addChangeListener(
                    _ -> Options.getInstance().getMidiOptions().setPanController((int) panController.getValue()));
            panController.setEnabled(external.isSelected());
            var panControllerLabel = createLabelFor("Pan controller", panController);

            panWithRelativeLocations = new JCheckBox("Pan with relative locations");
            panWithRelativeLocations.setToolTipText(
                    "If set, stereo panning will be relative to the location of the left-most and right-most components; otherwise it will be relative to the screen");
            panWithRelativeLocations.addActionListener(_ -> Options.getInstance().getMidiOptions()
                    .setPanWithRelativeLocations(panWithRelativeLocations.isSelected()));
            panWithRelativeLocations.setEnabled(external.isSelected());

            sendVerticalPanMessage = new JCheckBox("Send vertical panning");
            sendVerticalPanMessage.addActionListener(_ -> Options.getInstance().getMidiOptions()
                    .setSendVerticalPanMessage(sendVerticalPanMessage.isSelected()));
            sendVerticalPanMessage.setEnabled(external.isSelected());

            verticalPanController = createSpinner("Vertical pan controller",
                    MidiOptions.DEFAULT_VERTICAL_PAN_CONTROLLER, 0, 127, 1);
            verticalPanController.addChangeListener(_ -> Options.getInstance().getMidiOptions()
                    .setVerticalPanController((int) verticalPanController.getValue()));
            verticalPanController.setEnabled(external.isSelected());
            var verticalPanControllerLabel = createLabelFor("Vertical pan controller", verticalPanController);

            verticalPanWithRelativeLocations = new JCheckBox("Vertical pan with relative locations");
            verticalPanWithRelativeLocations.setToolTipText(
                    "If set, vertical panning will be relative to the location of the top-most and bottom-most components; otherwise it will be relative to the screen");
            verticalPanWithRelativeLocations.addActionListener(_ -> Options.getInstance().getMidiOptions()
                    .setVerticalPanWithRelativeLocations(verticalPanWithRelativeLocations.isSelected()));
            verticalPanWithRelativeLocations.setEnabled(external.isSelected());

            external.addActionListener(_ -> {
                Options.getInstance().getMidiOptions().setUsingExternalMidi(external.isSelected());
                sendMidiTimecode.setEnabled(external.isSelected());
                autoStartOnNoteOn.setEnabled(external.isSelected());
                sendPanMessage.setEnabled(external.isSelected());
                panController.setEnabled(external.isSelected() && sendPanMessage.isSelected());
                panWithRelativeLocations.setEnabled(external.isSelected() && sendPanMessage.isSelected());
                sendVerticalPanMessage.setEnabled(external.isSelected());
                verticalPanController.setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
                verticalPanWithRelativeLocations
                        .setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
            });

            sendPanMessage.addActionListener(_ -> {
                panControllerLabel.setEnabled(external.isSelected() && sendPanMessage.isSelected());
                panWithRelativeLocations.setEnabled(external.isSelected() && sendPanMessage.isSelected());
            });
            sendVerticalPanMessage.addActionListener(_ -> {
                verticalPanControllerLabel.setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
                verticalPanWithRelativeLocations
                        .setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
            });

            c.gridx = 0;
            c.gridy = 0;
            add(external, c);
            c.gridy++;
            add(sendMidiTimecode, c);
            c.gridy++;
            add(autoStartOnNoteOn, c);
            c.gridy++;
            add(sendPanMessage, c);
            c.gridx = 0;
            c.gridy++;
            add(panControllerLabel, c);
            c.gridx = 1;
            add(panController, c);
            c.gridx = 0;
            c.gridy++;
            add(panWithRelativeLocations, c);
            c.gridy++;
            add(sendVerticalPanMessage, c);
            c.gridx = 0;
            c.gridy++;
            add(verticalPanControllerLabel, c);
            c.gridx = 1;
            add(verticalPanController, c);
            c.gridx = 0;
            c.gridy++;
            add(verticalPanWithRelativeLocations, c);

            /*
             * Add a "filler" component to absorb extra vertical space This pushes all
             * previous components to the top of the container
             */
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        @Override
        public String toString() {
            return "MIDI";
        }
    }

    private class MusicianOptionsPanel extends JPanel {
        private final int id;
        private final MusicianUiPanel musicianUiPanel;
        private final MusicianMusicalOptionsPanel musicPanel;
        private final MusicianRuleOptionsPanel rulePanel;

        MusicianOptionsPanel(MusicianUiPanel uiPanel, MusicianMusicalOptionsPanel musicPanel,
                MusicianRuleOptionsPanel rulePanel) {
            super(new BorderLayout());
            this.id = musicPanel.id;
            this.musicianUiPanel = uiPanel;
            this.musicPanel = musicPanel;
            this.rulePanel = rulePanel;
            var tabbedPane = new JTabbedPane();
            tabbedPane.add(uiPanel);
            tabbedPane.add(musicPanel);
            tabbedPane.add(rulePanel);
            add(tabbedPane, BorderLayout.CENTER);
        }

        public MusicianOptionsPanel(List<MusicianOptionsPanel> mups) {
            super(new BorderLayout());
            this.id = -1;
            this.musicianUiPanel = new MusicianUiPanel(mups.stream().map(m -> m.musicianUiPanel).toList());
            this.musicPanel = new MusicianMusicalOptionsPanel(mups.stream().map(m -> m.musicPanel).toList());
            this.rulePanel = new MusicianRuleOptionsPanel(mups.stream().map(m -> m.rulePanel).toList());
            var tabbedPane = new JTabbedPane();
            tabbedPane.add(musicianUiPanel);
            tabbedPane.add(musicPanel);
            tabbedPane.add(rulePanel);
            add(tabbedPane, BorderLayout.CENTER);
        }

        @Override
        public String toString() {
            return MUSICIAN + id;
        }
    }

    private static class MusicianUiPanel extends JPanel {
        private final MusicianComponentOptions options;
        private final int id;
        private final JSpinner radius;

        MusicianUiPanel(int id, MusicianComponentOptions options) {
            this.id = id;
            this.options = options;
            setName("UI");
            setBorder(BorderFactory.createTitledBorder(MUSICIAN + id + " UI Options"));
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY, options.getRadius(), 1, 50, 1);
            radius.addChangeListener(_ -> options.setRadius((int) radius.getValue()));
            var radiusLabel = createLabelFor("Radius", radius);
            c.gridx = 0;
            c.gridy = 0;
            add(radiusLabel, c);
            c.gridx = 1;
            add(radius, c);

            // Add a "filler" component to absorb extra vertical space
            // This pushes all previous components to the top of the container
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        // called for multiple edits
        MusicianUiPanel(List<MusicianUiPanel> others) {
            this.id = -1; // not used
            this.options = new MusicianComponentOptions();
            setName("UI");
            var idList = others.stream().map(p -> p.id).toList();
            var optionsList = others.stream().map(p -> p.options).toList();
            var firstRadius = optionsList.get(0).getRadius();
            var allRadiiSame = optionsList.stream().map(MusicianComponentOptions::getRadius)
                    .allMatch(r -> firstRadius == r);
            if (allRadiiSame)
                options.setRadius(firstRadius);

            setBorder(BorderFactory.createTitledBorder(MUSICIAN + idList + " UI Options"));
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY, options.getRadius(), 1, 50, 1);
            var normalRadiusBackground = radius.getBackground();
            if (!allRadiiSame) {
                radius.setBackground(Color.red);
            }
            radius.addChangeListener(_ -> {
                radius.setBackground(normalRadiusBackground);
                others.forEach(o -> o.radius.setValue(radius.getValue()));
            });
            var radiusLabel = createLabelFor("Radius", radius);
            c.gridx = 0;
            c.gridy = 0;
            add(radiusLabel, c);
            c.gridx = 1;
            add(radius, c);

            // Add a "filler" component to absorb extra vertical space
            // This pushes all previous components to the top of the container
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        @Override
        public String toString() {
            return "UI";
        }
    }

    private class MusicianMusicalOptionsPanel extends JPanel {
        private static final String LOW_NOTE = "Low note";
        private static final String HIGH_NOTE = "High note";
        private final int id;
        private final MusicianOptions options;
        private final JComboBox<String> key;
        private final JSpinner rangeLow;
        private final JSpinner rangeHi;
        private JSpinner channel;
        private JComboBox<String> bus;

        @Override
        public String toString() {
            return "Music";
        }

        MusicianMusicalOptionsPanel(MusicianOptions options) {
            this.id = options.getId();
            this.options = options;
            setName("Musical");
            setBorder(BorderFactory.createTitledBorder(MUSICIAN + id + " musical options"));
            setLayout(new GridBagLayout());

            var keyList = new ArrayList<String>();
            keyList.add("");
            Key.BUILTIN_KEYS.keySet().forEach(keyList::add);
            var keyModel = new DefaultComboBoxModel<String>(keyList.toArray(new String[0]));
            key = new JComboBox<>(keyModel);
            key.setSelectedItem(options.getKeyName());
            key.addActionListener(_ -> options.setKeyName((String) key.getSelectedItem()));
            key.setEditable(true);
            var keyLabel = new JLabel("Key");
            keyLabel.setLabelFor(key);

            rangeLow = createSpinner(LOW_NOTE, options.getRange().low(), 0, 127, 1);
            rangeLow.addChangeListener(_ -> options.setRange(options.getRange().withLow((int) rangeLow.getValue())));
            var rangeLowLabel = createLabelFor(LOW_NOTE, rangeLow);
            rangeHi = createSpinner(HIGH_NOTE, options.getRange().high(), 0, 127, 1);
            rangeHi.addChangeListener(_ -> options.setRange(options.getRange().withHigh((int) rangeHi.getValue())));
            var rangeHiLabel = createLabelFor(HIGH_NOTE, rangeHi);
            channel = createSpinner("MIDI channel", options.getChannel() + 1, 1, 16, 1);
            channel.addChangeListener(_ -> options.setChannel((int) channel.getValue() - 1));
            var channelLabel = createLabelFor("MIDI Channel", channel);
            var availableDevices = new TreeSet<String>();
            availableDevices.add("");
            availableDevices.add(MusicianOptions.ALL_BUSSES);
            availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
            var busModel = new DefaultComboBoxModel<String>(availableDevices.toArray(new String[0]));
            bus = new JComboBox<>(busModel);
            bus.setSelectedItem(options.getBusName());
            bus.addActionListener(_ -> options.setBusName((String) bus.getSelectedItem()));
            var busLabel = new JLabel("MIDI Bus");
            busLabel.setLabelFor(bus);

            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            c.gridx = 0;
            c.gridy = 0;
            add(keyLabel, c);
            c.gridx = 1;
            add(key, c);
            c.gridx = 0;
            c.gridy++;
            add(rangeLowLabel, c);
            c.gridx = 1;
            add(rangeLow, c);
            c.gridx = 0;
            c.gridy++;
            add(rangeHiLabel, c);
            c.gridx = 1;
            add(rangeHi, c);
            c.gridx = 0;
            c.gridy++;
            add(channelLabel, c);
            c.gridx = 1;
            add(channel, c);
            c.gridx = 0;
            c.gridy++;
            add(busLabel, c);
            c.gridx = 1;
            add(bus, c);

            // Add a "filler" component to absorb extra vertical space
            // This pushes all previous components to the top of the container
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);
        }

        MusicianMusicalOptionsPanel(List<MusicianMusicalOptionsPanel> others) {
            this.id = -1; // unused
            this.options = new MusicianOptions();
            setName("Musical");

            var idList = others.stream().map(p -> p.id).toList();
            var optionsList = others.stream().map(p -> p.options).toList();
            var firstKeyName = optionsList.get(0).getKeyName();
            var allKeysSame = optionsList.stream().map(MusicianOptions::getKeyName).allMatch(firstKeyName::equals);
            if (allKeysSame)
                options.setKeyName(firstKeyName);
            else
                options.setKeyName("");
            var firstLow = optionsList.get(0).getRange().low();
            var allLowsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::low)
                    .allMatch(l -> firstLow == l);
            if (allLowsSame)
                options.setRange(options.getRange().withLow(firstLow));
            var firstHigh = optionsList.get(0).getRange().high();
            var allHighsSame = optionsList.stream().map(MusicianOptions::getRange).map(NoteRange::high)
                    .allMatch(h -> firstHigh == h);
            if (allHighsSame)
                options.setRange(options.getRange().withHigh(firstHigh));
            var firstChannel = optionsList.get(0).getChannel();
            var allChannelsSame = optionsList.stream().map(MusicianOptions::getChannel)
                    .allMatch(c -> firstChannel == c);
            if (allChannelsSame)
                options.setChannel(firstChannel);
            var firstBusName = optionsList.get(0).getBusName();
            var allBusNamesSame = optionsList.stream().map(MusicianOptions::getBusName).allMatch(firstBusName::equals);
            if (allBusNamesSame)
                options.setBusName(firstBusName);
            else
                options.setBusName("");

            setBorder(BorderFactory.createTitledBorder(MUSICIAN + idList + " musical options"));
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            var keyList = new ArrayList<String>();
            keyList.add("");
            Key.BUILTIN_KEYS.keySet().forEach(keyList::add);
            var keyModel = new DefaultComboBoxModel<String>(keyList.toArray(new String[0]));
            key = new JComboBox<>(keyModel);
            var normalKeyBackground = key.getBackground();
            if (!allKeysSame)
                key.setBackground(Color.red);
            key.setSelectedItem(options.getKeyName());
            key.addActionListener(_ -> {
                if ("".equals(key.getSelectedItem()))
                    return;
                options.setKeyName((String) key.getSelectedItem());
                key.setBackground(normalKeyBackground);
                others.forEach(o -> o.key.setSelectedItem(key.getSelectedItem()));
            });
            key.setEditable(true);
            var keyLabel = new JLabel("Key");
            keyLabel.setLabelFor(key);

            rangeLow = createSpinner(LOW_NOTE, options.getRange().low(), 0, 127, 1);
            var normalRangeLowBackground = rangeLow.getBackground();
            if (!allLowsSame)
                rangeLow.setBackground(Color.red);
            rangeLow.addChangeListener(_ -> {
                rangeLow.setBackground(normalRangeLowBackground);
                options.setRange(options.getRange().withLow((int) rangeLow.getValue()));
                others.forEach(o -> o.rangeLow.setValue(rangeLow.getValue()));
            });
            var rangeLowLabel = createLabelFor(LOW_NOTE, rangeLow);
            rangeHi = createSpinner(HIGH_NOTE, options.getRange().high(), 0, 127, 1);
            var normalRangeHiBackground = rangeHi.getBackground();
            if (!allHighsSame)
                rangeHi.setBackground(Color.red);
            rangeHi.addChangeListener(_ -> {
                rangeHi.setBackground(normalRangeHiBackground);
                options.setRange(options.getRange().withHigh((int) rangeHi.getValue()));
                others.forEach(o -> o.rangeHi.setValue(rangeHi.getValue()));
            });
            var rangeHiLabel = createLabelFor(HIGH_NOTE, rangeHi);
            channel = createSpinner("MIDI channel", options.getChannel() + 1, 1, 16, 1);
            var normalChannelBackground = channel.getBackground();
            if (!allChannelsSame)
                channel.setBackground(Color.red);
            channel.addChangeListener(_ -> {
                channel.setBackground(normalChannelBackground);
                options.setChannel((int) channel.getValue() - 1);
                others.forEach(o -> o.channel.setValue(channel.getValue()));
            });
            var channelLabel = createLabelFor("MIDI Channel", channel);
            var availableDevices = new TreeSet<String>();
            availableDevices.add("");
            availableDevices.add(MusicianOptions.ALL_BUSSES);
            availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
            var busModel = new DefaultComboBoxModel<String>(availableDevices.toArray(new String[0]));
            bus = new JComboBox<>(busModel);
            var normalBusBackground = bus.getBackground();
            if (!allBusNamesSame)
                bus.setBackground(Color.red);
            bus.setSelectedItem(options.getBusName());
            bus.addActionListener(_ -> {
                if ("".equals(bus.getSelectedItem()))
                    return;
                bus.setBackground(normalBusBackground);
                options.setBusName((String) bus.getSelectedItem());
                others.forEach(o -> o.bus.setSelectedItem(bus.getSelectedItem()));
            });
            var busLabel = new JLabel("MIDI Bus");
            busLabel.setLabelFor(bus);

            c.gridx = 0;
            c.gridy = 0;
            add(keyLabel, c);
            c.gridx = 1;
            add(key, c);
            c.gridx = 0;
            c.gridy++;
            add(rangeLowLabel, c);
            c.gridx = 1;
            add(rangeLow, c);
            c.gridx = 0;
            c.gridy++;
            add(rangeHiLabel, c);
            c.gridx = 1;
            add(rangeHi, c);
            c.gridx = 0;
            c.gridy++;
            add(channelLabel, c);
            c.gridx = 1;
            add(channel, c);
            c.gridx = 0;
            c.gridy++;
            add(busLabel, c);
            c.gridx = 1;
            add(bus, c);

            // Add a "filler" component to absorb extra vertical space
            // This pushes all previous components to the top of the container
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);

        }
    }

    private class MusicianRuleOptionsPanel extends JPanel {
        private RuleSpecificOptionsPanel ruleSpecificOptionsPanel;
        private final Map<String, MusicianRule> availableRules;
        private final List<String> ruleNames;
        private final int id;
        private final RuleOptions options;
        private final JComboBox<String> rule;

        @Override
        public String toString() {
            return "Rule";
        }

        public MusicianRuleOptionsPanel(int id, RuleOptions options) {
            this.id = id;
            this.options = options;
            setName("Rule");
            setBorder(BorderFactory.createTitledBorder(MUSICIAN + id + " rule options"));
            var bl = new BorderLayout();
            bl.setVgap(5);
            setLayout(bl);

            availableRules = new HashMap<>();
            ruleNames = new ArrayList<>();
            ServiceLoader.load(MusicianRule.class).forEach(r -> {
                availableRules.put(r.getName(), r);
                ruleNames.add(r.getName());
            });

            var ruleModel = new DefaultComboBoxModel<String>(ruleNames.toArray(new String[0]));
            ruleModel.setSelectedItem("");
            rule = new JComboBox<>(ruleModel);
            rule.setSelectedItem(options.getName());
            rule.addActionListener(_ -> {
                options.setName((String) rule.getSelectedItem());
                populateRuleSpecificParams(options);
            });

            var ruleLabel = new JLabel("Rule");
            ruleLabel.setLabelFor(rule);
            var upperPanel = new JPanel(new GridLayout(1, 2));
            upperPanel.add(ruleLabel);
            upperPanel.add(rule);
            add(upperPanel, BorderLayout.NORTH);

            populateRuleSpecificParams(options);
        }

        @SuppressWarnings("unchecked")
        // multiple selection
        MusicianRuleOptionsPanel(List<MusicianRuleOptionsPanel> others) {
            this.id = -1; // unused
            this.options = new RuleOptions();
            setName("Rule");
            availableRules = new HashMap<>();
            ruleNames = new ArrayList<>();
            ServiceLoader.load(MusicianRule.class).forEach(r -> {
                availableRules.put(r.getName(), r);
                ruleNames.add(r.getName());
            });

            var idList = others.stream().map(p -> p.id).toList();
            setBorder(BorderFactory.createTitledBorder(MUSICIAN + idList + " rule options"));
            var bl = new BorderLayout();
            bl.setVgap(5);
            setLayout(bl);

            var optionsList = others.stream().map(p -> p.options).toList();
            var firstName = optionsList.get(0).getName();
            var allNamesSame = optionsList.stream().map(RuleOptions::getName).allMatch(firstName::equals);
            if (allNamesSame)
                options.setName(firstName);
            else
                options.setName("");
            var ruleModel = new DefaultComboBoxModel<String>(ruleNames.toArray(new String[0]));
            ruleModel.setSelectedItem("");
            rule = new JComboBox<>(ruleModel);
            var normalRuleBackground = rule.getBackground();
            if (!allNamesSame)
                rule.setBackground(Color.red);
            rule.setSelectedItem(options.getName());
            rule.addActionListener(_ -> {
                if ("".equals(rule.getSelectedItem()))
                    return;
                rule.setBackground(normalRuleBackground);
                options.setName((String) rule.getSelectedItem());
                others.forEach(o -> o.rule.setSelectedItem(rule.getSelectedItem()));
                populateRuleSpecificParams(options);
            });

            options.addChangeListener(RuleOptions.RULE_SPECIFIC_OPTION_CHANGED_PROPERTY, e -> {
                if (e.getSource() instanceof ChangeSource(_, Object val) && val instanceof Map.Entry<?, ?>) {
                    var updatedOpts = (Map.Entry<String, Object>) val;
                    others.forEach(o -> {
                        var otherComp = o.ruleSpecificOptionsPanel.editableComponents.get(updatedOpts.getKey());
                        if (otherComp instanceof JCheckBox cb) {
                            cb.setSelected((boolean) updatedOpts.getValue());
                        } else if (otherComp instanceof JSpinner spin) {
                            spin.setValue(updatedOpts.getValue());
                        } else if (otherComp instanceof JComboBox<?> cbo) {
                            cbo.setSelectedItem(updatedOpts.getValue());
                        }
                    });
                }
            });

            var ruleLabel = new JLabel("Rule");
            ruleLabel.setLabelFor(rule);
            var upperPanel = new JPanel(new GridLayout(1, 2));
            upperPanel.add(ruleLabel);
            upperPanel.add(rule);
            add(upperPanel, BorderLayout.NORTH);

            populateRuleSpecificParams(options);
        }

        private void populateRuleSpecificParams(RuleOptions ruleOpts) {
            SwingUtilities.invokeLater(() -> {
                if (ruleSpecificOptionsPanel != null)
                    remove(ruleSpecificOptionsPanel);
                var selectedRule = availableRules.get(ruleOpts.getName());
                if (selectedRule == null)
                    return;
                var ruleParams = selectedRule.getSettableParameters();
                ruleSpecificOptionsPanel = new RuleSpecificOptionsPanel(ruleOpts, ruleParams);
                add(ruleSpecificOptionsPanel, BorderLayout.CENTER);
                revalidate();
                repaint();
            });
        }

    }

    private static class RuleSpecificOptionsPanel extends JPanel {
        private final Map<String, JComponent> editableComponents = new HashMap<>();

        RuleSpecificOptionsPanel(RuleOptions ruleOpts, List<SettableParamDescription> ruleParams) {
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.insets = new Insets(2, 2, 2, 2);
            c.anchor = GridBagConstraints.NORTHWEST;

            for (var ruleParam : ruleParams) {
                c.gridx = 0;
                c.gridy++;
                switch (ruleParam) {
                case IntegerParamDescription(String propertyName, String displayName, int minValue, int maxValue, int step, int defaultValue): {
                    var comp = createSpinner(propertyName,
                            ((int) ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue)), minValue,
                            maxValue, step);
                    comp.addChangeListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getValue()));
                    var label = createLabelFor(displayName, comp);
                    add(label, c);
                    c.gridx = 1;
                    add(comp, c);
                    editableComponents.put(propertyName, comp);
                }
                    break;
                case BooleanParamDescription(String propertyName, String displayName, boolean defaultValue): {
                    var comp = new JCheckBox(displayName);
                    comp.setSelected((Boolean) ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addChangeListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.isSelected()));
                    add(new JLabel(""), c);
                    c.gridx = 1;
                    add(comp, c);
                    editableComponents.put(propertyName, comp);
                }
                    break;
                case StringListParamDescription(String propertyName, String displayName, List<String> possibleValues, String defaultValue): {
                    var model = new DefaultComboBoxModel<String>(possibleValues.toArray(new String[0]));
                    var comp = new JComboBox<String>(model);
                    comp.setSelectedItem(ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addActionListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                    var label = new JLabel(displayName);
                    label.setLabelFor(comp);
                    add(label, c);
                    c.gridx = 1;
                    add(comp, c);
                    editableComponents.put(propertyName, comp);
                }
                    break;
                case EnumParamDescription(String propertyName, String displayName, Enum<?> defaultValue): {
                    @SuppressWarnings("unchecked")
                    var comp = createEnumComboBox(defaultValue.getClass());
                    comp.setSelectedItem(ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addActionListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                    var label = new JLabel(displayName);
                    label.setLabelFor(comp);
                    add(label, c);
                    c.gridx = 1;
                    add(comp, c);
                    editableComponents.put(propertyName, comp);
                }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "I haven't been programmed to understand a " + ruleParam.getClass().getName());
                }
            }
            /*
             * Add a "filler" component to absorb extra vertical space This pushes all
             * previous components to the top of the container
             */
            c.gridx = 0;
            c.gridy++;
            c.weighty = 1.0; // Give all extra vertical space to this row
            c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
            add(Box.createVerticalGlue(), c);

        }

        private static <E extends Enum<E>> JComboBox<E> createEnumComboBox(Class<E> clazz) {
            var model = new DefaultComboBoxModel<E>(clazz.getEnumConstants());
            return new JComboBox<>(model);
        }

    }

    private static JLabel createLabelFor(String propertyName, JSpinner spinner) {
        var spinnerLabel = new JLabel(propertyName.replace('_', ' '));
        spinnerLabel.setLabelFor(spinner);
        return spinnerLabel;
    }

    private static JSpinner createSpinner(String propertyName, double defValue, double min, double max, double step) {
        var model = new SpinnerNumberModel(defValue, min, max, step);
        var spinner = new JSpinner(model);
        spinner.setName(propertyName);
        return spinner;
    }

    private static JSpinner createSpinner(String propertyName, int defValue, int min, int max, int step) {
        var model = new SpinnerNumberModel(defValue, min, max, step);
        var spinner = new JSpinner(model);
        spinner.setName(propertyName);
        return spinner;
    }

    private static JSpinner createSpinner(String propertyName, long defValue, long min, long max, long step) {
        var model = new SpinnerNumberModel(defValue, min, max, step);
        var spinner = new JSpinner(model);
        spinner.setName(propertyName);
        return spinner;
    }

    void updateOptions() {
        var options = Options.getInstance();
        uiPanel.gravity.setValue(options.getUiOptions().getGravity());
        uiPanel.showIcons.setSelected(options.getUiOptions().isShowNumbers());
        uiPanel.edgeLength.setValue(options.getUiOptions().getEdgeLength());
        uiPanel.windSpeed.setValue(options.getUiOptions().getWindSpeed());
        uiPanel.animateBackground.setSelected(options.getUiOptions().isAnimateBackground());

        miscPanel.randomSeedSpinner.setValue((double) options.getRandomSeed());

        midiPanel.external.setSelected(options.getMidiOptions().isUsingExternalMidi());
        midiPanel.sendMidiTimecode.setSelected(options.getMidiOptions().isSendingMidiTimecode());
        midiPanel.autoStartOnNoteOn.setSelected(options.getMidiOptions().isAutoStartOnNoteOn());
        midiPanel.sendPanMessage.setSelected(options.getMidiOptions().isSendPanMessage());
        midiPanel.panController.setValue(options.getMidiOptions().getPanController());
        midiPanel.panWithRelativeLocations.setSelected(options.getMidiOptions().isPanWithRelativeLocations());
        midiPanel.sendVerticalPanMessage.setSelected(options.getMidiOptions().isSendVerticalPanMessage());
        midiPanel.verticalPanController.setValue(options.getMidiOptions().getVerticalPanController());
        midiPanel.verticalPanWithRelativeLocations
                .setSelected(options.getMidiOptions().isVerticalPanWithRelativeLocations());
    }
}
