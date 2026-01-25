package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import org.roach.margia.Key;
import org.roach.margia.rules.AbstractMusicianRule;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.Persistence;

/**
 * GUI and musical options
 */
@SuppressWarnings({ "java:S1948" })
public class MusicianOptionWindow extends JDialog implements VetoableChangeListener {
    static final String MUSICIAN_OPTION_WINDOW_NAME = "MusicianOptionPanel";
    private JComboBox<String> rule;
    private final JPanel[] paramComps = new JPanel[5];
    private JComboBox<String> key;
    private JSpinner rangeLow;
    private JSpinner rangeHi;
    private JSpinner channel;
    private JSpinner mass;
    
    private JPanel ruleOptsPanel;
    private HashMap<String, MusicianRule> availableRules;

    /**
     * constructor
     */
    MusicianOptionWindow() {
        super((JFrame) null, "Musician Options");
        setName(MUSICIAN_OPTION_WINDOW_NAME);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    var loc = getLocationOnScreen();
                    Persistence.getInstance().saveProperty(MUSICIAN_OPTION_WINDOW_NAME + "_x", Integer.toString(loc.x));
                    Persistence.getInstance().saveProperty(MUSICIAN_OPTION_WINDOW_NAME + "_y", Integer.toString(loc.y));
                    Persistence.getInstance().saveProperty(MUSICIAN_OPTION_WINDOW_NAME + "_width",
                            Integer.toString(getBounds().width));
                    Persistence.getInstance().saveProperty(MUSICIAN_OPTION_WINDOW_NAME + "_height",
                            Integer.toString(getBounds().height));
                }
            }
        });

        var x = Persistence.getInstance().getInt(MUSICIAN_OPTION_WINDOW_NAME + "_x", 100);
        var y = Persistence.getInstance().getInt(MUSICIAN_OPTION_WINDOW_NAME + "_y", 100);
        var w = Persistence.getInstance().getInt(MUSICIAN_OPTION_WINDOW_NAME + "_width", 100);
        var h = Persistence.getInstance().getInt(MUSICIAN_OPTION_WINDOW_NAME + "_height", 100);
        this.setLocation(x, y);
        this.setSize(w, h);
        setVisible(Persistence.getInstance().getBoolean(MUSICIAN_OPTION_WINDOW_NAME + "_visible", false));

        createUi();
    }

    private void createUi() {
        JPanel musicianUiPanel;
        JPanel musicalOptsPanel;
        var tabPane = new JTabbedPane();
        setPreferredSize(new Dimension(350, 300));
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());
        add(tabPane, BorderLayout.CENTER);

        musicianUiPanel = createMusicianUiPanel();
        tabPane.addTab("UI Options", musicianUiPanel);
        musicalOptsPanel = createMusicalOptionsPanel();
        tabPane.addTab("Musical Options", musicalOptsPanel);
        ruleOptsPanel = createRuleParamPanel();
        tabPane.addTab("Rule Options", ruleOptsPanel);

        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        Persistence.getInstance().saveProperty(MUSICIAN_OPTION_WINDOW_NAME + "_visible",
                Boolean.toString(this.isVisible()));
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

    private JPanel createMusicianUiPanel() {
        var panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);
        
        mass = createSpinner("Mass", 1d, 0.1d, 100d, 0.1d, Double.class);
        var massLabel = createLabelFor("Mass", mass);
        var row = 0;
        c.gridx = 0;
        c.gridy = row++;
        panel.add(massLabel, c);
        c.gridx = 1;
        panel.add(mass, c);

        // Add a "filler" component to absorb extra vertical space
        // This pushes all previous components to the top of the container
        c.gridy = row++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        panel.add(Box.createVerticalGlue(), c);

        return panel;
    }

    private JPanel createMusicalOptionsPanel() {
        var panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2, 3, 5));

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

        rangeLow = createSpinner("Low note", 0d, 0d, 127d, 1d, Integer.class);
        var rangeLowLabel = createLabelFor("Low note", rangeLow);
        rangeHi = createSpinner("High note", 127d, 0d, 127d, 1d, Integer.class);
        var rangeHiLabel = createLabelFor("High note", rangeHi);
        channel = createSpinner("MIDI channel", 0d, 0d, 16d, 1d, Integer.class);
        var channelLabel = createLabelFor("MIDI Channel", channel);

        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);
        
        var row = 0;
        c.gridx = 0;
        c.gridy = row++;
        panel.add(rangeLowLabel, c);
        c.gridx = 1;
        panel.add(rangeLow, c);
        c.gridx = 0;
        c.gridy = row++;
        panel.add(rangeHiLabel, c);
        c.gridx = 1;
        panel.add(rangeHi, c);
        c.gridx = 0;
        c.gridy = row++;
        panel.add(channelLabel, c);
        c.gridx = 1;
        panel.add(channel, c);
        
        // Add a "filler" component to absorb extra vertical space
        // This pushes all previous components to the top of the container
        c.gridy = row++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        panel.add(Box.createVerticalGlue(), c);
        return panel;
    }

    private JPanel createRuleParamPanel() {
        // add JPanel for rule params
        var panel = new JPanel();
        var ruleParamsBorder = BorderFactory.createTitledBorder("Rule-specific params");
        panel.setBorder(ruleParamsBorder);
        panel.setLayout(new GridBagLayout());
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
        
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 5, 5, 5);
        
        var row = 0;
        c.gridx = 0;
        c.gridy = row++;
        panel.add(ruleLabel, c);
        c.gridx = 1;
        panel.add(rule, c);

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        for (var i = 0; i < 5; i++) {
            paramComps[i] = new JPanel(new GridLayout(1, 2, 3, 5));
            panel.add(paramComps[i], c);
            c.gridy = row++;
        }
        // Add a "filler" component to absorb extra vertical space
        // This pushes all previous components to the top of the container
        c.gridx = 0;
        c.gridy = row++;
        c.weighty = 1.0; // Give all extra vertical space to this row
        c.fill = GridBagConstraints.BOTH; // Allow the filler to expand
        panel.add(Box.createVerticalGlue(), c);
        return panel;
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
            setTitle("Musician options");
            resetUiAndListeners();
        } else {
            selectedMusician = sel;
            selectedMusician.setEdited(true);
            setTitle("Musician options (" + sel.getMusician().getId() + ")");
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
                                ((Number) ruleOpts.getRuleSpecificOptions().getOrDefault(ruleParam.propertyName(),
                                        ruleParam.minValue())).doubleValue(),
                                ruleParam.minValue(), ruleParam.maxValue(), ruleParam.step(),
                                (Class<? extends Number>) ruleParam.type());
                        comp.addChangeListener(
                                _ -> ruleOpts.setRuleSpecificOption(ruleParam.propertyName(), comp.getValue()));
                        var label = createLabelFor(ruleParam.displayName(), comp);
                        paramComps[row].add(label);
                        paramComps[row].add(comp);
                        row++;
                    } else if (Boolean.class.equals(ruleParam.type())) {
                        var comp = new JCheckBox(ruleParam.displayName());
                        comp.setSelected((Boolean) ruleOpts.getRuleSpecificOptions().get(ruleParam.propertyName()));
                        comp.addChangeListener(
                                _ -> ruleOpts.setRuleSpecificOption(ruleParam.propertyName(), comp.isSelected()));
                        paramComps[row].add(Box.createHorizontalStrut(1));
                        paramComps[row].add(comp);
                        row++;
                    }
                }
                ruleOptsPanel.revalidate();
                ruleOptsPanel.repaint();
            });
        }
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
