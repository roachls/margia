package org.roach.margia.view;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;
import org.roach.margia.storage.params.*;

class OptionsWindow extends JDialog {
    private static final String HEIGHT_PROPERTY = "_height";
    private static final String WIDTH_PROPERTY = "_width";
    private static final String Y_PROPERTY = "_y";
    private static final String X_PROPERTY = "_x";
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";

    private JSpinner gravity;
    private JSpinner windSpeed;
    private JSpinner edgeLength;
    private JCheckBox showIcons;
    private JCheckBox animateBackground;
    private JSpinner randomSeedSpinner;
    private JCheckBox external;
    private JCheckBox sendMidiTimecode;
    private JCheckBox autoStartOnNoteOn;
    private JCheckBox sendPanMessage;
    private JSpinner panController;
    private JCheckBox panWithRelativeLocations;
    private JCheckBox sendVerticalPanMessage;
    private JSpinner verticalPanController;
    private JCheckBox verticalPanWithRelativeLocations;
    private final HashMap<String, MusicianRule> availableRules;
    private final ArrayList<String> ruleNames;

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

        availableRules = new HashMap<>();
        ruleNames = new ArrayList<String>();
        ServiceLoader.load(MusicianRule.class).forEach(r -> {
            availableRules.put(r.getName(), r);
            ruleNames.add(r.getName());
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
        var root = new DefaultMutableTreeNode("root");
        var uiOptions = new DefaultMutableTreeNode("UI");
        var midiOptions = new DefaultMutableTreeNode("MIDI");
        var miscOptions = new DefaultMutableTreeNode("Miscellaneous");
        var musicianUiOptions = new DefaultMutableTreeNode("Musician");
        var musicianOptions = new DefaultMutableTreeNode("Musicians");
        for (var musicianId : Options.getInstance().getMusicians().keySet()) {
            var mcPanel = new MusicianUiPanel(musicianId,
                    Options.getInstance().getUiOptions().getMusicianComponents().get(musicianId));
            var mcNode = new DefaultMutableTreeNode(mcPanel);
            musicianUiOptions.add(mcNode);

            var musicPanel = new MusicianMusicalOptionsPanel(Options.getInstance().getMusicians().get(musicianId));
            var musicNode = new DefaultMutableTreeNode(musicPanel);
            var rulePanel = new MusicianRuleOptionsPanel(musicianId,
                    Options.getInstance().getMusicians().get(musicianId).getRuleOptions());
            var ruleNode = new DefaultMutableTreeNode(rulePanel, false);
            musicNode.add(ruleNode);
            musicianOptions.add(musicNode);
        }
        uiOptions.add(musicianUiOptions);

        root.add(uiOptions);
        root.add(midiOptions);
        root.add(miscOptions);
        root.add(musicianOptions);
        var tree = new JTree(root);
        tree.setShowsRootHandles(true);
        var uiPanel = createUiOptionsPanel();
        var miscPanel = createMiscPanel();
        var midiPanel = createMidiPanel();

        selectedPanel.set(uiPanel);
        add(uiPanel, BorderLayout.CENTER);
        tree.addTreeSelectionListener(e -> {
            remove(selectedPanel.get());
            var path = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
            if (uiOptions.equals(path))
                selectedPanel.set(uiPanel);
            else if (miscOptions.equals(path))
                selectedPanel.set(miscPanel);
            else if (midiOptions.equals(path))
                selectedPanel.set(midiPanel);
            else {
                var userObject = path.getUserObject();
                if (userObject instanceof JPanel panel) {
                    selectedPanel.set(panel);
                }
            }
            add(selectedPanel.get(), BorderLayout.CENTER);
            revalidate();
            repaint();
        });
        var treePane = new JPanel(new BorderLayout());
        treePane.setBorder(BorderFactory.createEtchedBorder());
        treePane.add(new JScrollPane(tree), BorderLayout.CENTER);
        add(treePane, BorderLayout.WEST);
        tree.setRootVisible(false);

        updateOptions();

        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_visible", Boolean.toString(this.isVisible()));
    }

    private JPanel createUiOptionsPanel() {
        var panel = new JPanel();
        panel.setName("UI Options");
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);

        gravity = createSpinner("Gravity", Options.getInstance().getUiOptions().getGravity(), -20.0, 20.0, 0.1);
        gravity.addChangeListener(_ -> Options.getInstance().getUiOptions().setGravity((double) gravity.getValue()));
        var gravityLabel = createLabelFor("Gravitational Constant", gravity);
        windSpeed = createSpinner("Wind speed (clockwise)", Options.getInstance().getUiOptions().getWindSpeed(), -200.0,
                200.0, 0.1);
        windSpeed.addChangeListener(
                _ -> Options.getInstance().getUiOptions().setWindSpeed((double) windSpeed.getValue()));
        var windSpeedLabel = createLabelFor("WindSpeed", windSpeed);
        edgeLength = createSpinner(UiOptions.EDGE_LENGTH_PROPERTY, Options.getInstance().getUiOptions().getEdgeLength(),
                20, 300, 1);
        edgeLength.addChangeListener(
                _ -> Options.getInstance().getUiOptions().setEdgeLength((int) edgeLength.getValue()));
        var edgeLengthLabel = createLabelFor("Edge length", edgeLength);
        showIcons = new JCheckBox("Show icons");
        showIcons.setToolTipText("Show/hide additional information on musicians");
        showIcons.setSelected(Options.getInstance().getUiOptions().isShowNumbers());
        showIcons.addActionListener(_ -> Options.getInstance().getUiOptions().setShowNumbers(showIcons.isSelected()));
        animateBackground = new JCheckBox("Animate background");
        animateBackground.setToolTipText("Animate background");
        animateBackground.setSelected(Options.getInstance().getUiOptions().isAnimateBackground());
        animateBackground.addActionListener(
                _ -> Options.getInstance().getUiOptions().setAnimateBackground(animateBackground.isSelected()));

        var row = 0;
        c.gridx = 0;
        c.gridy = row++;
        panel.add(gravityLabel, c);
        c.gridx = 1;
        panel.add(gravity, c);
        c.gridx = 0;
        c.gridy = row++;
        panel.add(windSpeedLabel, c);
        c.gridx = 1;
        panel.add(windSpeed, c);
        c.gridx = 0;
        c.gridy = row++;
        panel.add(edgeLengthLabel, c);
        c.gridx = 1;
        panel.add(edgeLength, c);
        c.gridy = row++;
        panel.add(showIcons, c);
        c.gridy = row++;
        panel.add(animateBackground, c);

        /*
         * Add a "filler" component to absorb extra vertical space This pushes all
         * previous components to the top of the container
         */
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        panel.add(Box.createVerticalGlue(), c);

        return panel;
    }

    private JPanel createMiscPanel() {
        var miscPanel = new JPanel();
        miscPanel.setBorder(BorderFactory.createEtchedBorder());
        miscPanel.setName("Misc Options");
        miscPanel.setLayout(new GridBagLayout());
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
        miscPanel.add(randomSeedLabel, c);
        c.gridx = 1;
        miscPanel.add(randomSeedSpinner, c);

        /*
         * Add a "filler" component to absorb extra vertical space This pushes all
         * previous components to the top of the container
         */
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        miscPanel.add(Box.createVerticalGlue(), c);

        return miscPanel;
    }

    private JPanel createMidiPanel() {
        var midiPanel = new JPanel();
        midiPanel.setBorder(BorderFactory.createEtchedBorder());
        midiPanel.setName("MIDI Options");
        midiPanel.setLayout(new GridBagLayout());
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

        verticalPanController = createSpinner("Vertical pan controller", MidiOptions.DEFAULT_VERTICAL_PAN_CONTROLLER, 0,
                127, 1);
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
            verticalPanWithRelativeLocations.setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
        });

        sendPanMessage.addActionListener(_ -> {
            panControllerLabel.setEnabled(external.isSelected() && sendPanMessage.isSelected());
            panWithRelativeLocations.setEnabled(external.isSelected() && sendPanMessage.isSelected());
        });
        sendVerticalPanMessage.addActionListener(_ -> {
            verticalPanControllerLabel.setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
            verticalPanWithRelativeLocations.setEnabled(external.isSelected() && sendVerticalPanMessage.isSelected());
        });

        c.gridx = 0;
        c.gridy = 0;
        midiPanel.add(external, c);
        c.gridy++;
        midiPanel.add(sendMidiTimecode, c);
        c.gridy++;
        midiPanel.add(autoStartOnNoteOn, c);
        c.gridy++;
        midiPanel.add(sendPanMessage, c);
        c.gridx = 0;
        c.gridy++;
        midiPanel.add(panControllerLabel, c);
        c.gridx = 1;
        midiPanel.add(panController, c);
        c.gridx = 0;
        c.gridy++;
        midiPanel.add(panWithRelativeLocations, c);
        c.gridy++;
        midiPanel.add(sendVerticalPanMessage, c);
        c.gridx = 0;
        c.gridy++;
        midiPanel.add(verticalPanControllerLabel, c);
        c.gridx = 1;
        midiPanel.add(verticalPanController, c);
        c.gridx = 0;
        c.gridy++;
        midiPanel.add(verticalPanWithRelativeLocations, c);

        /*
         * Add a "filler" component to absorb extra vertical space This pushes all
         * previous components to the top of the container
         */
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        midiPanel.add(Box.createVerticalGlue(), c);

        return midiPanel;
    }

    private static class MusicianUiPanel extends JPanel {
        private final int id;

        MusicianUiPanel(int id, MusicianComponentOptions options) {
            this.id = id;
            setBorder(BorderFactory.createTitledBorder("Musician " + options.getRadius()));
            setLayout(new GridBagLayout());
            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);

            var radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY, options.getRadius(), 1, 50, 1);
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

        @Override
        public String toString() {
            return Integer.toString(id);
        }
    }

    private static class MusicianMusicalOptionsPanel extends JPanel {
        private final int id;

        @Override
        public String toString() {
            return Integer.toString(id);
        }

        public MusicianMusicalOptionsPanel(MusicianOptions options) {
            this.id = options.getId();
            setBorder(BorderFactory.createTitledBorder("Musician " + id + " musical options"));
            setLayout(new GridBagLayout());

            var keyList = new ArrayList<String>();
            keyList.add("");
            Key.BUILTIN_KEYS.keySet().forEach(keyList::add);
            var keyModel = new DefaultComboBoxModel<String>(keyList.toArray(new String[0]));
            var key = new JComboBox<>(keyModel);
            key.setSelectedItem(options.getKeyName());
            key.addActionListener(_ -> options.setKeyName((String) key.getSelectedItem()));
            key.setEditable(true);
            var keyLabel = new JLabel("Key");
            keyLabel.setLabelFor(key);

            var rangeLow = createSpinner("Low note", options.getRange().low(), 0, 127, 1);
            rangeLow.addChangeListener(_ -> options.setRange(options.getRange().withLow((int) rangeLow.getValue())));
            var rangeLowLabel = createLabelFor("Low note", rangeLow);
            var rangeHi = createSpinner("High note", options.getRange().high(), 0, 127, 1);
            rangeHi.addChangeListener(_ -> options.setRange(options.getRange().withHigh((int) rangeHi.getValue())));
            var rangeHiLabel = createLabelFor("High note", rangeHi);
            var channel = createSpinner("MIDI channel", options.getChannel() + 1, 1, 16, 1);
            channel.addChangeListener(_ -> options.setChannel((int) channel.getValue() - 1));
            var channelLabel = createLabelFor("MIDI Channel", channel);
            var availableDevices = new TreeSet<String>();
            availableDevices.add("");
            availableDevices.add(MusicianOptions.ALL_BUSSES);
            availableDevices.addAll(MidiController.getInstance().getAvailableOutputDevices());
            var busModel = new DefaultComboBoxModel<String>(availableDevices.toArray(new String[0]));
            var bus = new JComboBox<>(busModel);
            bus.setSelectedItem(options.getBusName());
            bus.addActionListener(_ -> options.setBusName((String) bus.getSelectedItem()));
            var busLabel = new JLabel("MIDI Bus");
            busLabel.setLabelFor(bus);

            var c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(5, 5, 5, 5);

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
        
        @Override
        public String toString() {
            return "Rule";
        }

        public MusicianRuleOptionsPanel(int id, RuleOptions options) {
            setBorder(BorderFactory.createTitledBorder("Musician " + id + " rule options"));
            var bl = new BorderLayout();
            bl.setVgap(5);
            setLayout(bl);

            var ruleModel = new DefaultComboBoxModel<String>(ruleNames.toArray(new String[0]));
            ruleModel.setSelectedItem("");
            var rule = new JComboBox<>(ruleModel);
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

        private void populateRuleSpecificParams(RuleOptions ruleOpts) {
            SwingUtilities.invokeLater(() -> {
                if (ruleSpecificOptionsPanel != null)
                    remove(ruleSpecificOptionsPanel);
                var selectedRule = availableRules.get(ruleOpts.getName());
                var ruleParams = selectedRule.getSettableParameters();
                ruleSpecificOptionsPanel = new RuleSpecificOptionsPanel(ruleOpts, ruleParams);
                add(ruleSpecificOptionsPanel, BorderLayout.CENTER);
                revalidate();
                repaint();
            });
        }

    }
    
    private static class RuleSpecificOptionsPanel extends JPanel {
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
                }
                    break;
                case BooleanParamDescription(String propertyName, String displayName, boolean defaultValue): {
                    var comp = new JCheckBox(displayName);
                    comp.setSelected((Boolean) ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addChangeListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.isSelected()));
                    add(new JLabel(""), c);
                    c.gridx = 1;
                    add(comp, c);
                }
                    break;
                case StringListParamDescription(String propertyName, String displayName, List<String> possibleValues, String defaultValue): {
                    var model = new DefaultComboBoxModel<String>(possibleValues.toArray(new String[0]));
                    var comp = new JComboBox<String>(model);
                    comp.setSelectedItem(ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addActionListener(
                            _ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                    var label = new JLabel(displayName);
                    label.setLabelFor(comp);
                    add(label, c);
                    c.gridx = 1;
                    add(comp, c);
                }
                    break;
                case EnumParamDescription(String propertyName, String displayName, Enum<?> defaultValue): {
                    @SuppressWarnings("unchecked")
                    var comp = createEnumComboBox(defaultValue.getClass());
                    comp.setSelectedItem(ruleOpts.getRuleSpecificOptionOrDefault(propertyName, defaultValue));
                    comp.addActionListener(
                            _ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                    var label = new JLabel(displayName);
                    label.setLabelFor(comp);
                    add(label, c);
                    c.gridx = 1;
                    add(comp, c);
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
        gravity.setValue(options.getUiOptions().getGravity());
        showIcons.setSelected(options.getUiOptions().isShowNumbers());
        edgeLength.setValue(options.getUiOptions().getEdgeLength());
        windSpeed.setValue(options.getUiOptions().getWindSpeed());
        animateBackground.setSelected(options.getUiOptions().isAnimateBackground());

        randomSeedSpinner.setValue((double) options.getRandomSeed());

        external.setSelected(options.getMidiOptions().isUsingExternalMidi());
        sendMidiTimecode.setSelected(options.getMidiOptions().isSendingMidiTimecode());
        autoStartOnNoteOn.setSelected(options.getMidiOptions().isAutoStartOnNoteOn());
        sendPanMessage.setSelected(options.getMidiOptions().isSendPanMessage());
        panController.setValue(options.getMidiOptions().getPanController());
        panWithRelativeLocations.setSelected(options.getMidiOptions().isPanWithRelativeLocations());
        sendVerticalPanMessage.setSelected(options.getMidiOptions().isSendVerticalPanMessage());
        verticalPanController.setValue(options.getMidiOptions().getVerticalPanController());
        verticalPanWithRelativeLocations.setSelected(options.getMidiOptions().isVerticalPanWithRelativeLocations());
    }
}
