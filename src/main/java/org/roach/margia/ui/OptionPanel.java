package org.roach.margia.ui;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.ServiceLoader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.roach.margia.*;
import org.roach.margia.rules.MusicianRule;

/**
 * GUI and musical options
 */
@SuppressWarnings({ "java:S1948" })
public class OptionPanel extends JPanel implements PropertyChangeListener {
    private JSpinner radius;
    private JSpinner gravity;
    private JSpinner edgeLength;
    private JCheckBox showNumbers;
    private final java.util.List<Musician> musicians;
    private JComboBox<String> rule;
    private JComboBox<String> key;
    private JSpinner rangeLow;
    private JSpinner rangeHi;
    private TitledBorder musicianPanelBorder;
    private JPanel musPanel;

    /**
     * constructor
     * 
     * @param musicians list of musicians
     */
    public OptionPanel(java.util.List<Musician> musicians) {
        this.musicians = musicians;
        this.setLayout(new GridBagLayout());
        var constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;

        var row = 0;
        constraints.gridy = row++;
        add(createUiPanel(), constraints);
        constraints.gridy = row++;
        musPanel = createMusicianPanel();
        add(musPanel, constraints);

        // Add a "filler" component to absorb extra vertical space
        // This pushes all previous components to the top of the container
        constraints.gridy = row;
        constraints.weighty = 1.0; // Give all extra vertical space to this row
        constraints.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        add(Box.createVerticalGlue(), constraints);
    }

    private JPanel createUiPanel() {
        var uiPanel = new JPanel();
        uiPanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Graphics options"));
        uiPanel.setLayout(new GridLayout(0, 2, 3, 5));
        radius = addSpinner(
                uiPanel, MusicianComponent.RADIUS_PROPERTY, Options.getInstance()
                        .getOrDefaultAsDouble(MusicianComponent.RADIUS_PROPERTY, MusicianComponent.DEFAULT_RADIUS),
                1d, 50d, 1d, Integer.class);
        gravity = addSpinner(
                uiPanel, AgentPanel.GRAVITY_PROPERTY, Options.getInstance()
                        .getOrDefaultAsDouble(AgentPanel.GRAVITY_PROPERTY, AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT),
                -20.0, 20.0, 0.1, Double.class);
        edgeLength = addSpinner(uiPanel, AgentPanel.EDGE_LENGTH_PROPERTY, Options.getInstance().getOrDefaultAsDouble(
                AgentPanel.EDGE_LENGTH_PROPERTY, AgentPanel.DEFAULT_EDGE_LENGTH), 20d, 150d, 1d, Integer.class);
        showNumbers = new JCheckBox();
        showNumbers.setSelected(
                Options.getInstance().getOrDefaultAsBoolean(MusicianComponent.SHOW_NUMBERS_PROPERTY, true));
        showNumbers.addActionListener(
                _ -> updateOption(MusicianComponent.SHOW_NUMBERS_PROPERTY, showNumbers.isSelected()));
        var showNumbersLabel = new JLabel(MusicianComponent.SHOW_NUMBERS_PROPERTY);
        showNumbersLabel.setLabelFor(showNumbers);
        uiPanel.add(showNumbersLabel);
        uiPanel.add(showNumbers);

        return uiPanel;
    }

    private JSpinner addSpinner(JPanel panel, String propertyName, Double defValue, Double min, Double max,
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
        panel.add(spinnerLabel);
        panel.add(spinner);
        spinner.addChangeListener(_ -> updateOption(propertyName, spinner.getValue()));
        if (type.equals(Integer.class))
            updateOption(propertyName, defValue.intValue());
        else
            updateOption(propertyName, defValue);
        return spinner;
    }

    private JPanel createMusicianPanel() {
        var panel = new JPanel();
        musicianPanelBorder = BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(),
                "Musician options");
        panel.setBorder(musicianPanelBorder);
        panel.setLayout(new GridLayout(0, 2, 3, 5));

        var availableRules = new ArrayList<String>();
        availableRules.add("");
        ServiceLoader.load(MusicianRule.class).forEach(r -> availableRules.add(r.getName()));
        var ruleModel = new DefaultComboBoxModel<String>(availableRules.toArray(new String[0]));
        ruleModel.setSelectedItem("");
        rule = new JComboBox<>(ruleModel);
        var ruleLabel = new JLabel("Rule");
        ruleLabel.setLabelFor(rule);
        panel.add(ruleLabel);
        panel.add(rule);

        var keyList = new ArrayList<String>();
        keyList.add("");
        Key.BUILTIN_KEYS.keySet().forEach(keyList::add);
        var keyModel = new DefaultComboBoxModel<String>(keyList.toArray(new String[0]));
        key = new JComboBox<>(keyModel);
        key.setEditable(true);
        var keyLabel = new JLabel("Key");
        keyLabel.setLabelFor(key);
        panel.add(keyLabel);
        panel.add(key);
        
        rangeLow = addSpinner(panel, "Low", 0d, 0d, 127d, 1d, Integer.class);
        rangeHi = addSpinner(panel, "High", 127d, 0d, 127d, 1d, Integer.class);

        return panel;
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
        default:
            break;
        }
    }

    void updateOptions() {
        var options = Options.getInstance();
        for (var option : options.entrySet()) {
            var value = option.getValue().toString();
            switch (option.getKey()) {
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
        Options.getInstance().put(propertyName, property.toString());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
        case AgentPanel.SELECTED_AGENT_PROPERTY:
            var musId = (int) evt.getNewValue();
            if (musId == -1) {
                musicianPanelBorder.setTitle("Musician options");
                rule.setSelectedItem("");
                key.setSelectedItem("");
                rangeLow.setValue(0);
                rangeHi.setValue(127);
            } else {
                var selectedMusician = musicians.stream().filter(m -> m.getId() == musId).findAny();
                selectedMusician.ifPresent(m -> {
                    musicianPanelBorder.setTitle("Musician options (" + musId + ")");
                    rule.setSelectedItem(m.getRule().getName());
                    key.setSelectedItem(m.getKey().getName());
                    rangeLow.setValue(m.getRangeLow());
                    rangeHi.setValue(m.getRangeHi());
                });
            }
            musPanel.repaint();
            break;
        default:
            break;
        }
    }
}
