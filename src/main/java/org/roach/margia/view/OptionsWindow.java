package org.roach.margia.view;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import org.roach.margia.model.MidiOptions;
import org.roach.margia.model.UiOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

class OptionsWindow extends JDialog {
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";
    private JSpinner gravity;
    private JSpinner windSpeed;
    private JSpinner edgeLength;
    private JCheckBox showIcons;
    private JCheckBox animateBackground;
    private static JSpinner randomSeedSpinner;
    private static JCheckBox external;
    private static JCheckBox sendMidiTimecode;
    private static JCheckBox autoStartOnNoteOn;
    private static JCheckBox sendPanMessage;
    private static JSpinner panController;
    private static JCheckBox panWithRelativeLocations;
    private static JCheckBox sendVerticalPanMessage;
    private static JSpinner verticalPanController;
    private static JCheckBox verticalPanWithRelativeLocations;

    OptionsWindow() {
        super((JFrame) null, "Options");
        setName(OPTIONS_WINDOW_NAME);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    var loc = getLocationOnScreen();
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_x", Integer.toString(loc.x));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_y", Integer.toString(loc.y));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_width",
                            Integer.toString(getBounds().width));
                    Persistence.getInstance().saveProperty(OPTIONS_WINDOW_NAME + "_height",
                            Integer.toString(getBounds().height));
                }
            }
        });

        createUi();
        var x = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + "_x", 100);
        var y = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + "_y", 100);
        var w = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + "_width", 100);
        var h = Persistence.getInstance().getInt(OPTIONS_WINDOW_NAME + "_height", 100);
        this.setLocation(x, y);
        this.setSize(w, h);
        setVisible(Persistence.getInstance().getBoolean(OptionsWindow.OPTIONS_WINDOW_NAME + "_visible", false));
    }

    private void createUi() {
        var tabPane = new JTabbedPane();
        setPreferredSize(new Dimension(350, 300));
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());

        add(tabPane, BorderLayout.CENTER);

        tabPane.addTab("Graphics Options", createUiOptionsPanel());
        tabPane.addTab("Misc Options", createMiscPanel());
        tabPane.addTab("MIDI Options", createMidiPanel());

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
        panel.setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);

        gravity = createSpinner("Gravity", Options.getInstance().getUiOptions().getGravity(), -20.0, 20.0, 0.1,
                Double.class);
        gravity.addChangeListener(_ -> Options.getInstance().getUiOptions().setGravity((double) gravity.getValue()));
        var gravityLabel = createLabelFor("Gravitational Constant", gravity);
        windSpeed = createSpinner("Wind speed (clockwise)", Options.getInstance().getUiOptions().getWindSpeed(), -200.0,
                200.0, 0.1, Double.class);
        windSpeed.addChangeListener(
                _ -> Options.getInstance().getUiOptions().setWindSpeed((double) windSpeed.getValue()));
        var windSpeedLabel = createLabelFor("WindSpeed", windSpeed);
        edgeLength = createSpinner(UiOptions.EDGE_LENGTH_PROPERTY,
                (double) Options.getInstance().getUiOptions().getEdgeLength(), 20d, 300d, 1d, Integer.class);
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

        return panel;
    }

    private static JPanel createMiscPanel() {
        var miscPanel = new JPanel();
        miscPanel.setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);

        randomSeedSpinner = createSpinner("Random seed", 0d, (double) -Long.MAX_VALUE, (double) Long.MAX_VALUE, 1d,
                Long.class);
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
        return miscPanel;
    }

    private static JPanel createMidiPanel() {
        var midiPanel = new JPanel();
        midiPanel.setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);

        external = new JCheckBox("Use External MIDI");
        external.setSelected(Options.getInstance().getMidiOptions().isUseExternalMidi());
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

        panController = createSpinner("Pan controller", (double) MidiOptions.DEFAULT_PAN_CONTROLLER, 0d, 127d, 1d,
                Integer.class);
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
                (double) MidiOptions.DEFAULT_VERTICAL_PAN_CONTROLLER, 0d, 127d, 1d, Integer.class);
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
            Options.getInstance().getMidiOptions().setUseExternalMidi(external.isSelected());
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

        return midiPanel;
    }

    private static JLabel createLabelFor(String propertyName, JSpinner spinner) {
        var spinnerLabel = new JLabel(propertyName.replace('_', ' '));
        spinnerLabel.setLabelFor(spinner);
        return spinnerLabel;
    }

    private static JSpinner createSpinner(String propertyName, Double defValue, Double min, Double max, Double step,
            Class<? extends Number> type) {
        SpinnerNumberModel model;
        if (type.equals(Integer.class))
            model = new SpinnerNumberModel(defValue.intValue(), min.intValue(), max.intValue(), step.intValue());
        else if (type.equals(Long.class) || type.equals(long.class))
            model = new SpinnerNumberModel(defValue.longValue(), min.longValue(), max.longValue(), step.longValue());
        else
            model = new SpinnerNumberModel(defValue.doubleValue(), min.doubleValue(), max.doubleValue(),
                    step.doubleValue());
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

        external.setSelected(options.getMidiOptions().isUseExternalMidi());
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
