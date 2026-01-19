package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.roach.margia.Key;
import org.roach.margia.rules.AbstractMusicianRule;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.storage.Options;

/**
 * GUI and musical options
 */
@SuppressWarnings({ "java:S1948" })
public class OptionPanel extends JPanel implements VetoableChangeListener {
    private JComboBox<String> rule;
    private JPanel ruleParamsPanel;
    private final JPanel[] paramComps = new JPanel[5];
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
        musPanel = createMusicianPanel();
        add(musPanel, constraints);
        constraints.gridy = row++;
        createRuleParamPanel();
        add(ruleParamsPanel, constraints);

        // Add a "filler" component to absorb extra vertical space
        // This pushes all previous components to the top of the container
        constraints.gridy = row;
        constraints.weighty = 1.0; // Give all extra vertical space to this row
        constraints.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        add(Box.createVerticalGlue(), constraints);
    }

    private static JSpinner addSpinner(JPanel panel, String propertyName, Double defValue, Double min, Double max,
            Double step, Class<? extends Number> type) {

        var spinner = createSpinner(propertyName, defValue, min, max, step, type);
        var spinnerLabel = createLabelFor(propertyName, spinner);
        panel.add(spinnerLabel);
        panel.add(spinner);
        return spinner;
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

    private void createRuleParamPanel() {
        // add JPanel for rule params
        ruleParamsPanel = new JPanel();
        var ruleParamsBorder = BorderFactory.createTitledBorder("Rule-specific params");
        ruleParamsPanel.setBorder(ruleParamsBorder);
        ruleParamsPanel.setLayout(new GridLayout(5, 2, 3, 5));
        for (var i = 0; i < 5; i++) {
            paramComps[i] = new JPanel(new GridLayout(1, 2, 3, 5));
            ruleParamsPanel.add(paramComps[i]);
        }
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
            selectedMusician.setEdited(false);
            selectedMusician = null;
            resetUiAndListeners();
        }
        var sel = (MusicianComponent) evt.getNewValue();
        if (sel == null) {
            musicianPanelBorder.setTitle("Musician options");
            resetUiAndListeners();
        } else {
            selectedMusician = sel;
            selectedMusician.setEdited(true);
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

            // populate rulesPanel
            var selectedRule = (AbstractMusicianRule) selectedMusician.getMusician().getRule();
            var ruleParams = selectedRule.getSettableParameters();
            var ruleOpts = Options.getInstance().getMusicians().get(selectedMusician.getMusician().getId())
                    .getRuleOptions();
            SwingUtilities.invokeLater(() -> {
                var row = 0;
                for (var ruleParam : ruleParams) {
                    paramComps[row].removeAll();
                    if (Number.class.isAssignableFrom(ruleParam.type())) {
                        @SuppressWarnings("unchecked")
                        var comp = createSpinner(ruleParam.propertyName(),
                                ((Number) ruleOpts.getRuleSpecificOptions().get(ruleParam.propertyName()))
                                        .doubleValue(),
                                ruleParam.minValue(), ruleParam.maxValue(), ruleParam.step(),
                                (Class<? extends Number>) ruleParam.type());
                        comp.addChangeListener(
                                _ -> ruleOpts.setRuleSpecificOption(ruleParam.propertyName(), comp.getValue()));
                        var label = createLabelFor(ruleParam.displayName(), comp);
                        paramComps[row].add(label);
                        paramComps[row].add(comp);
                        row++;
                    }
                }
                ruleParamsPanel.revalidate();
                ruleParamsPanel.repaint();
            });
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
        for (var paramPanel : paramComps) {
            paramPanel.removeAll();
            paramPanel.revalidate();
        }
    }

}
