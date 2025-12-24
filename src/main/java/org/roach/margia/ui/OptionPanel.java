package org.roach.margia.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * GUI and musical options
 */
public class OptionPanel extends JPanel {
    private JSpinner radius;
    private JSpinner gravity;
    private JSpinner edgeLength;
    private JCheckBox showNumbers;
    private final Properties options = new Properties();
    private boolean dirty;
    static final String DIRTY_PROPERTY = "dirty";
    private final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * constructor
     */
    public OptionPanel() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Graphics options"));
        var innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(0, 2, 3, 5));
        radius = addSpinner(innerPanel, MusicianComponent.RADIUS_PROPERTY, (double) MusicianComponent.DEFAULT_RADIUS,
                1d, 50d, 1d, Integer.class);
        gravity = addSpinner(innerPanel, AgentPanel.GRAVITY_PROPERTY, AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT, 0.0,
                20.0, 0.1, Double.class);
        edgeLength = addSpinner(innerPanel, AgentPanel.EDGE_LENGTH_PROPERTY, (double) AgentPanel.DEFAULT_EDGE_LENGTH,
                20d, 150d, 1d, Integer.class);
        showNumbers = new JCheckBox();
        showNumbers.setSelected(true);
        showNumbers.addActionListener(
                _ -> updateOption(MusicianComponent.SHOW_NUMBERS_PROPERTY.replace('_', ' '), showNumbers.isSelected()));
        updateOption(MusicianComponent.SHOW_NUMBERS_PROPERTY, true);
        var showNumbersLabel = new JLabel(MusicianComponent.SHOW_NUMBERS_PROPERTY);
        showNumbersLabel.setLabelFor(showNumbers);
        innerPanel.add(showNumbersLabel);
        innerPanel.add(showNumbers);

        this.add(innerPanel, BorderLayout.NORTH);
        setDirty(false);
    }

    private JSpinner addSpinner(JPanel innerPanel, String propertyName, Double defValue, Double min, Double max,
            Double step, Class<? extends Number> type) {

        SpinnerNumberModel model;
        if (type.equals(Integer.class))
            model = new SpinnerNumberModel(defValue.intValue(), min.intValue(), max.intValue(), step.intValue());
        else
            model = new SpinnerNumberModel(defValue.doubleValue(), min.doubleValue(), max.doubleValue(),
                    step.doubleValue());
        var spinner = new JSpinner(model);
        spinner.setName(propertyName);
        var spinnerLabel = new JLabel(propertyName.replace('_', ' '));
        spinnerLabel.setLabelFor(spinner);
        innerPanel.add(spinnerLabel);
        innerPanel.add(spinner);
        spinner.addChangeListener(_ -> updateOption(propertyName, spinner.getValue()));
        if (type.equals(Integer.class))
            updateOption(propertyName, defValue.intValue());
        else
            updateOption(propertyName, defValue.doubleValue());
        return spinner;
    }

    /**
     * @param property property to listen for
     * @param listener listener for property changes
     */
    public void addChangeListener(String property, ChangeListener listener) {
        switch (property) {
        case MusicianComponent.RADIUS_PROPERTY:
            this.radius.addChangeListener(listener);
            break;
        case AgentPanel.GRAVITY_PROPERTY:
            this.gravity.addChangeListener(listener);
            break;
        case AgentPanel.EDGE_LENGTH_PROPERTY:
            this.edgeLength.addChangeListener(listener);
            break;
        case MusicianComponent.SHOW_NUMBERS_PROPERTY:
            this.showNumbers.addChangeListener(listener);
            break;
        case DIRTY_PROPERTY:
            emitter.addChangeListener(DIRTY_PROPERTY, listener);
            break;
        default:
            break;
        }
    }

    Properties getOptions() { return options; }

    void setOptions(Properties options) {
        this.options.clear();
        this.options.putAll(options);
        for (var option : options.entrySet()) {
            var value = option.getValue().toString();
            switch (option.getKey().toString()) {
            case MusicianComponent.RADIUS_PROPERTY:
                radius.setValue(Integer.parseInt(value));
                break;
            case AgentPanel.GRAVITY_PROPERTY:
                gravity.setValue(Double.parseDouble(value));
                break;
            case MusicianComponent.SHOW_NUMBERS_PROPERTY:
                showNumbers.setSelected(Boolean.parseBoolean(value));
                break;
            case AgentPanel.EDGE_LENGTH_PROPERTY:
                edgeLength.setValue(Integer.parseInt(value));
                break;
            default:
                break;
            }
        }
    }

    void updateOption(String propertyName, Object property) {
        this.options.put(propertyName, property.toString());
        setDirty(true);
    }

    /**
     * @return whether the properties have changed since last saved
     */
    boolean isDirty() { return dirty; }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, this.dirty));
    }
}
