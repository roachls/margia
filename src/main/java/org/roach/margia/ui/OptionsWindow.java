package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

class OptionsWindow extends JDialog {
    static final String OPTIONS_WINDOW_NAME = "optionsWindow";
    private JSpinner radius;
    private JSpinner gravity;
    private JSpinner edgeLength;
    private JCheckBox showIcons;

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
        setName(OPTIONS_WINDOW_NAME);
        setLayout(new BorderLayout());

        add(tabPane, BorderLayout.CENTER);

        tabPane.addTab("Graphics Options", createUiOptionsPanel());
        tabPane.addTab("Misc Options", createMiscPanel());
        tabPane.addTab("MIDI Options", createMidiPanel());

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

        radius = createSpinner(MusicianComponent.RADIUS_PROPERTY,
                (double) Options.getInstance().getUiOptions().getRadius(), 1d, 50d, 1d, Integer.class);
        radius.addChangeListener(_ -> Options.getInstance().getUiOptions().setRadius((int) radius.getValue()));
        var radiusLabel = createLabelFor("Radius", radius);
        gravity = createSpinner("Gravity", Options.getInstance().getUiOptions().getGravity(), -20.0, 20.0, 0.1,
                Double.class);
        gravity.addChangeListener(_ -> Options.getInstance().getUiOptions().setGravity((double) gravity.getValue()));
        var gravityLabel = createLabelFor("Gravitational Constant", gravity);
        edgeLength = createSpinner(AgentPanel.EDGE_LENGTH_PROPERTY,
                (double) Options.getInstance().getUiOptions().getEdgeLength(), 20d, 300d, 1d, Integer.class);
        edgeLength.addChangeListener(
                _ -> Options.getInstance().getUiOptions().setEdgeLength((int) edgeLength.getValue()));
        var edgeLengthLabel = createLabelFor("Edge length", edgeLength);
        showIcons = new JCheckBox("Show icons");
        showIcons.setToolTipText("Show/hide additional information on musicians");
        showIcons.setSelected(Options.getInstance().getUiOptions().isShowNumbers());
        showIcons
                .addActionListener(_ -> Options.getInstance().getUiOptions().setShowNumbers(showIcons.isSelected()));

        c.gridx = 0;
        c.gridy = 0;
        panel.add(radiusLabel, c);
        c.gridx = 1;
        panel.add(radius, c);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(gravityLabel, c);
        c.gridx = 1;
        panel.add(gravity, c);
        c.gridx = 0;
        c.gridy = 2;
        panel.add(edgeLengthLabel, c);
        c.gridx = 1;
        panel.add(edgeLength, c);
        c.gridy = 3;
        panel.add(showIcons, c);

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

        var randomSeedSpinner = createSpinner("Random seed", 0d, (double) -Long.MAX_VALUE, (double) Long.MAX_VALUE, 1d,
                Long.class);
        JComponent editor = randomSeedSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defEditor) {
            JFormattedTextField textField = defEditor.getTextField();
            textField.setColumns(15); // Set width to 3 columns
        }
        var randomSeedLabel = createLabelFor("Random seed", randomSeedSpinner);
        randomSeedSpinner.setValue(Options.getInstance().getRandomSeed());
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

        var external = new JCheckBox("Use External MIDI");
        external.addActionListener(
                _ -> Options.getInstance().getMidiOptions().setUseExternalMidi(external.isSelected()));
        external.setSelected(Options.getInstance().getMidiOptions().isUseExternalMidi());

        c.gridx = 0;
        c.gridy = 0;
        midiPanel.add(external, c);
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
        else if (type.equals(Long.class))
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
        radius.setValue(options.getUiOptions().getRadius());
        gravity.setValue(options.getUiOptions().getGravity());
        showIcons.setSelected(options.getUiOptions().isShowNumbers());
        edgeLength.setValue(options.getUiOptions().getEdgeLength());
    }
}
