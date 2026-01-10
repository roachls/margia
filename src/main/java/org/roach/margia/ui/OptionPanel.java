package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.roach.margia.*;
import org.roach.margia.rules.AbstractMusicianRule;
import org.roach.margia.rules.MusicianRule;

/**
 * GUI and musical options
 */
@SuppressWarnings({ "java:S1948" })
public class OptionPanel extends JPanel implements VetoableChangeListener {
    private JSpinner radius;
    private JSpinner gravity;
    private JSpinner edgeLength;
    private JCheckBox showNumbers;
    private JComboBox<String> rule;
    private JComboBox<String> key;
    private JSpinner rangeLow;
    private JSpinner rangeHi;
    private JSpinner channel;
    private JSpinner mass;
    private TitledBorder musicianPanelBorder;
    private JPanel musPanel;
    private HashMap<String, MusicianRule> availableRules;

    /**
     * constructor
     */
    public OptionPanel() {
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
                AgentPanel.EDGE_LENGTH_PROPERTY, AgentPanel.DEFAULT_EDGE_LENGTH), 20d, 300d, 1d, Integer.class);
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

    private JSpinner addSpinner(JPanel panel, String propertyName, Double defValue, Double min, Double max, Double step,
            Class<? extends Number> type) {

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

        availableRules = new HashMap<>();
        var ruleNames = new ArrayList<String>();
        ServiceLoader.load(MusicianRule.class).forEach(r -> {
            availableRules.put(r.getName(), r);
            ruleNames.add(r.getName());
        });
        var ruleModel = new DefaultComboBoxModel<String>(ruleNames.toArray(new String[0]));
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

        rangeLow = addSpinner(panel, "Low note", 0d, 0d, 127d, 1d, Integer.class);
        rangeHi = addSpinner(panel, "High note", 127d, 0d, 127d, 1d, Integer.class);
        channel = addSpinner(panel, "MIDI channel", 0d, 0d, 16d, 1d, Integer.class);
        mass = addSpinner(panel, "Mass", 1d, 0.1d, 100d, 0.1d, Double.class);

        return panel;
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
            case MusicianComponent.MASS_PROPERTY:
                mass.setValue(Double.parseDouble(value));
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
        Options.getInstance().put(propertyName, property);
    }

    private ActionListener ruleActionListener;
    private ActionListener keyActionListener;
    private ChangeListener channelChangeListener;
    private ChangeListener rangeLowChangeListener;
    private ChangeListener rangeHiChangeListener;
    private MusicianComponent selectedMusician;
    private ChangeListener massChangeListener;

    @Override
    public void vetoableChange(PropertyChangeEvent evt) {
        if (selectedMusician != null) {
            selectedMusician.setSelected(false);
            selectedMusician = null;
            resetUiAndListeners();
        }
        var sel = (MusicianComponent) evt.getNewValue();
        if (sel == null) {
            musicianPanelBorder.setTitle("Musician options");
            resetUiAndListeners();
        } else {
            selectedMusician = sel;
            selectedMusician.setSelected(true);
            musicianPanelBorder.setTitle("Musician options (" + sel.getMusician().getId() + ")");
            rule.setSelectedItem(selectedMusician.getMusician().getRule().getName());
            ruleActionListener = _ -> selectedMusician.getMusician()
                    .setRule((AbstractMusicianRule) availableRules.get(rule.getSelectedItem()));
            rule.addActionListener(ruleActionListener);
            key.setSelectedItem(selectedMusician.getMusician().getKey().getName());
            keyActionListener = _ -> selectedMusician.getMusician().setKey(Key.BUILTIN_KEYS.get(key.getSelectedItem()));
            key.addActionListener(keyActionListener);
            rangeLow.setValue(selectedMusician.getMusician().getRangeLow());
            rangeLowChangeListener = _ -> selectedMusician.getMusician().setRangeLow((int) rangeLow.getValue());
            rangeLow.addChangeListener(rangeLowChangeListener);
            rangeHi.setValue(selectedMusician.getMusician().getRangeHi());
            rangeHiChangeListener = _ -> selectedMusician.getMusician().setRangeHi((int) rangeHi.getValue());
            rangeHi.addChangeListener(rangeHiChangeListener);
            channel.setValue(selectedMusician.getMusician().getChannel());
            channelChangeListener = _ -> selectedMusician.getMusician().setChannel((int) channel.getValue());
            channel.addChangeListener(channelChangeListener);
            mass.setValue(selectedMusician.getMass());
            massChangeListener = _ -> selectedMusician.setMass((double) mass.getValue());
            mass.addChangeListener(massChangeListener);
        }
        musPanel.repaint();
    }

    private void resetUiAndListeners() {
        if (ruleActionListener != null) {
            rule.removeActionListener(ruleActionListener);
            ruleActionListener = null;
        }
        rule.setSelectedItem("");
        if (keyActionListener != null) {
            key.removeActionListener(keyActionListener);
            keyActionListener = null;
        }
        key.setSelectedItem("");
        if (rangeLowChangeListener != null) {
            rangeLow.removeChangeListener(rangeLowChangeListener);
            rangeLowChangeListener = null;
        }
        rangeLow.setValue(0);
        if (rangeHiChangeListener != null) {
            rangeHi.removeChangeListener(rangeHiChangeListener);
            rangeHiChangeListener = null;
        }
        rangeHi.setValue(127);
        if (channelChangeListener != null) {
            channel.removeChangeListener(channelChangeListener);
            channelChangeListener = null;
        }
        channel.setValue(0);
        if (massChangeListener != null) {
            mass.removeChangeListener(massChangeListener);
            massChangeListener = null;
        }
        mass.setValue(1.0);
    }
}
