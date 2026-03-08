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
    private MusicPanel globalMusicPanel;
    private JTree tree;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode musiciansNode;
    private final Map<Integer, DefaultMutableTreeNode> musicianNodes = new HashMap<>();

    private static final double LEFT_COLUMN_WEIGHT = 0.2;
    private static final double RIGHT_COLUMN_WEIGHT = 1.0 - LEFT_COLUMN_WEIGHT;

    public static final String OPTIONS_AGENTS_SELECTED_PROPERTY = "options agents selected";

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
        updateOptions();
    }

    private void createUi() {
        AtomicReference<JPanel> selectedPanel = new AtomicReference<>();
        setLayout(new BorderLayout());
        root = new DefaultMutableTreeNode("root");
        uiPanel = new UiOptionsPanel();
        var uiOptions = new DefaultMutableTreeNode(uiPanel);
        midiPanel = new MidiPanel();
        var midiOptions = new DefaultMutableTreeNode(midiPanel);
        globalMusicPanel = new MusicPanel();
        var musicOptions = new DefaultMutableTreeNode(globalMusicPanel);
        miscPanel = new MiscPanel();
        var miscOptions = new DefaultMutableTreeNode(miscPanel);
        musiciansNode = new DefaultMutableTreeNode("Musicians");

        root.add(uiOptions);
        root.add(midiOptions);
        root.add(miscOptions);
        root.add(musicOptions);
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
            if (tree.getSelectionCount() > 0) {
                var selectedPaths = tree.getSelectionPaths();
                var musicianIdList = new ArrayList<Integer>();
                for (var selectedPath : selectedPaths) {
                    var lastPathComponent = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                    var string = lastPathComponent.getUserObject().toString();
                    if (string != null && string.startsWith(MUSICIAN)) {
                        var musicianId = Integer.parseInt(string.substring(MUSICIAN.length()));
                        musicianIdList.add(musicianId);
                    }
                }
                // fire change of musicianIdList with property OPTIONS_AGENTS_SELECTED_PROPERTY
                firePropertyChange(OPTIONS_AGENTS_SELECTED_PROPERTY, Collections.emptyList(), musicianIdList);
            }
            revalidate();
            repaint();
        });
        var treePane = new JPanel(new BorderLayout());
        treePane.setBorder(BorderFactory.createEtchedBorder());
        treePane.add(new JScrollPane(tree), BorderLayout.CENTER);
        add(treePane, BorderLayout.WEST);
        tree.setRootVisible(false);

        add(treePane, BorderLayout.WEST);
        pack();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (AgentPanel.AGENT_ADDED_PROPERTY.equals(e.getPropertyName())) {
            var mc = (MusicianComponent) e.getNewValue();
            var id = mc.getMusician().getId();
            var newUiPanel = new MusicianUiPanel(id, mc.getOptions());
            var newMusicianPanel = new MusicianMusicalOptionsPanel(mc.getMusician().getOptions());
            var newRulePanel = new MusicianRuleOptionsPanel(id, mc.getMusician().getOptions().getRuleOptions());
            var newOptionsPanel = new MusicianOptionsPanel(newUiPanel, newMusicianPanel, newRulePanel);
            var newNode = new DefaultMutableTreeNode(newOptionsPanel);
            var model = ((DefaultTreeModel) tree.getModel());
            model.insertNodeInto(newNode, musiciansNode, musiciansNode.getChildCount());
            musicianNodes.put(id, newNode);
            revalidate();
            repaint();
        } else if (AgentPanel.AGENT_REMOVED_PROPERTY.equals(e.getPropertyName())) {
            var mc = (MusicianComponent) e.getNewValue();
            var id = mc.getMusician().getId();
            if (musicianNodes.containsKey(id)) {
                var path = new TreePath(new Object[] { root, musiciansNode, musicianNodes.get(id) });
                var model = ((DefaultTreeModel) tree.getModel());
                model.removeNodeFromParent((DefaultMutableTreeNode) path.getLastPathComponent());
            }
            musicianNodes.remove(id);
            revalidate();
            repaint();
        } else if (AgentPanel.SELECTED_AGENT_PROPERTY.equals(e.getPropertyName())) {
            tree.clearSelection();
            var selectedMusicians = (List<Musician>) e.getNewValue();
            var paths = selectedMusicians.stream().map(Musician::getId).filter(musicianNodes::containsKey)
                    .map(id -> new TreePath(new Object[] { root, musiciansNode, musicianNodes.get(id) })).toList();
            tree.setSelectionPaths(paths.toArray(new TreePath[0]));
            if (!paths.isEmpty())
                tree.scrollPathToVisible(paths.get(0));
        }
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy = row++;
            add(gravityLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(gravity, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy = row++;
            add(windSpeedLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(windSpeed, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy = row++;
            add(edgeLengthLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            add(randomSeedLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            c.weightx = 1;
            add(external, c);
            c.gridy++;
            add(sendMidiTimecode, c);
            c.gridy++;
            add(sendPanMessage, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(panControllerLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(panController, c);
            c.gridx = 0;
            c.weightx = 1;
            c.gridy++;
            add(panWithRelativeLocations, c);
            c.gridy++;
            add(sendVerticalPanMessage, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(verticalPanControllerLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(verticalPanController, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
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

    private class MusicPanel extends JPanel {
        private static final String GLOBAL_MUSIC_OPTIONS = "Global Music Options";
        private final JSpinner maxQueueSize;

        MusicPanel() {
            setBorder(BorderFactory.createTitledBorder(GLOBAL_MUSIC_OPTIONS));
            setName(GLOBAL_MUSIC_OPTIONS);
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            maxQueueSize = createSpinner("Max Queue Size", MusicOptions.DEFAULT_MAX_QUEUE_SIZE, 1, 120, 1);
            maxQueueSize.addChangeListener(
                    _ -> Options.getInstance().getMusicOptions().setMaxQueueSize((int) maxQueueSize.getValue()));
            var maxQueueSizeLabel = createLabelFor("Max Queue Size", maxQueueSize);

            c.gridx = 0;
            c.gridy = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            add(maxQueueSizeLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(maxQueueSize, c);

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
            return GLOBAL_MUSIC_OPTIONS;
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            add(radiusLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            add(radiusLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            add(keyLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(key, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(rangeLowLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(rangeLow, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(rangeHiLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(rangeHi, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(channelLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(channel, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(busLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy = 0;
            add(keyLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(key, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(rangeLowLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(rangeLow, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(rangeHiLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(rangeHi, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(channelLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            add(channel, c);
            c.gridx = 0;
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridy++;
            add(busLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
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
            rule.addActionListener(_ -> options.setName((String) rule.getSelectedItem()));

            var ruleLabel = new JLabel("Rule");
            ruleLabel.setLabelFor(rule);
            var upperPanel = new JPanel(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);
            c.weightx = LEFT_COLUMN_WEIGHT;
            c.gridx = 0;
            upperPanel.add(ruleLabel, c);
            c.gridx = 1;
            c.weightx = RIGHT_COLUMN_WEIGHT;
            upperPanel.add(rule, c);
            add(upperPanel, BorderLayout.NORTH);
        }

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
            var allNamesSame = optionsList.stream().map(RuleOptions::getName)
                    .allMatch(name -> Objects.equals(firstName, name));
            if (allNamesSame)
                options.setName(firstName == null ? "" : firstName);
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
            });

            var ruleLabel = new JLabel("Rule");
            ruleLabel.setLabelFor(rule);
            var upperPanel = new JPanel(new GridLayout(1, 2));
            upperPanel.add(ruleLabel);
            upperPanel.add(rule);
            add(upperPanel, BorderLayout.NORTH);
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
        midiPanel.sendPanMessage.setSelected(options.getMidiOptions().isSendPanMessage());
        midiPanel.panController.setValue(options.getMidiOptions().getPanController());
        midiPanel.panWithRelativeLocations.setSelected(options.getMidiOptions().isPanWithRelativeLocations());
        midiPanel.sendVerticalPanMessage.setSelected(options.getMidiOptions().isSendVerticalPanMessage());
        midiPanel.verticalPanController.setValue(options.getMidiOptions().getVerticalPanController());
        midiPanel.verticalPanWithRelativeLocations
                .setSelected(options.getMidiOptions().isVerticalPanWithRelativeLocations());

        globalMusicPanel.maxQueueSize.setValue(options.getMusicOptions().getMaxQueueSize());
    }
}
