package org.roach.margia.ui;

import java.awt.*;

import javax.swing.*;

import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

class UiOptionsWindow extends JDialog {
    static final String UI_OPTIONS_WINDOW_NAME = "uiOptionsWindow";
    private JSpinner radius;
    private JSpinner gravity;
    private JSpinner edgeLength;
    private JCheckBox showNumbers;

    UiOptionsWindow() {
        createUi();
        var x = Persistence.getInstance().getInt(UI_OPTIONS_WINDOW_NAME + "_x", 100);
        var y = Persistence.getInstance().getInt(UI_OPTIONS_WINDOW_NAME + "_y", 100);
        this.setLocation(x, y);
        setVisible(Persistence.getInstance().getBoolean(UiOptionsWindow.UI_OPTIONS_WINDOW_NAME + "_visible", false));
    }

    private void createUi() {
        setUndecorated(true);
        setAlwaysOnTop(true);
        setName(UI_OPTIONS_WINDOW_NAME);
        setLayout(new BorderLayout());
        var panel = createPanel();
        add(panel, BorderLayout.CENTER);
        var draggable = new MouseDragAdapter(this);
        panel.addMouseListener(draggable);
        panel.addMouseMotionListener(draggable);

        var hidePanel = new JPanel();
        var hideBtn = new JButton("X");
        hideBtn.addActionListener(_ -> this.setVisible(false));
        hidePanel.add(hideBtn);
        add(hidePanel, BorderLayout.SOUTH);

        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        Persistence.getInstance().saveProperty(UI_OPTIONS_WINDOW_NAME + "_visible", Boolean.toString(this.isVisible()));
    }

    private JPanel createPanel() {
        var panel = new JPanel();
        panel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black, 2), "Graphics options"));
        panel.setLayout(new GridLayout(0, 2, 3, 5));
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
        showNumbers = new JCheckBox();
        showNumbers.setSelected(Options.getInstance().getUiOptions().isShowNumbers());
        showNumbers
                .addActionListener(_ -> Options.getInstance().getUiOptions().setShowNumbers(showNumbers.isSelected()));
        var showNumbersLabel = new JLabel(MusicianComponent.SHOW_NUMBERS_PROPERTY);
        showNumbersLabel.setLabelFor(showNumbers);
        panel.add(radiusLabel);
        panel.add(radius);
        panel.add(gravityLabel);
        panel.add(gravity);
        panel.add(edgeLengthLabel);
        panel.add(edgeLength);
        panel.add(showNumbersLabel);
        panel.add(showNumbers);
        return panel;
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
        showNumbers.setSelected(options.getUiOptions().isShowNumbers());
        edgeLength.setValue(options.getUiOptions().getEdgeLength());
    }
}
