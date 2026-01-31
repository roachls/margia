package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import org.roach.margia.Key;
import org.roach.margia.rules.AbstractMusicianRule;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.storage.*;
import org.roach.margia.storage.params.*;

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
    private JSpinner radius;

    private JPanel ruleOptsPanel;

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

        radius = createSpinner(MusicianComponentOptions.RADIUS_PROPERTY,
                (double) MusicianComponentOptions.DEFAULT_RADIUS, 1d, 50d, 1d, Integer.class);
        var radiusLabel = createLabelFor("Radius", radius);
        c.gridx = 0;
        c.gridy = row++;
        panel.add(radiusLabel, c);
        c.gridx = 1;
        panel.add(radius, c);

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
        HashMap<String, MusicianRule> availableRules;
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
    private ChangeListener radiusChangeListener;

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
            var id = selectedMusician.getMusician().getId();
            setTitle(String.format("Musician options (%d)", id));
            var compOpts = selectedMusician.getOptions();
            var musOpts = selectedMusician.getMusician().getOptions();
            rule.setSelectedItem(musOpts.getRuleOptions().getName());
            ruleActionListener = _ -> musOpts.getRuleOptions().setName((String) rule.getSelectedItem());
            rule.addActionListener(ruleActionListener);
            key.setSelectedItem(selectedMusician.getMusician().getKey().getName());
            keyActionListener = _ -> musOpts.setKeyName((String) key.getSelectedItem());
            key.addActionListener(keyActionListener);
            rangeLow.setValue(musOpts.getRange().low());
            rangeLowChangeListener = _ -> musOpts.setRange(musOpts.getRange().withLow((int) rangeLow.getValue()));
            rangeLow.addChangeListener(rangeLowChangeListener);
            rangeHi.setValue(musOpts.getRange().high());
            rangeHiChangeListener = _ -> musOpts.setRange(musOpts.getRange().withHigh((int) rangeHi.getValue()));
            rangeHi.addChangeListener(rangeHiChangeListener);
            channel.setValue(musOpts.getChannel());
            channelChangeListener = _ -> musOpts.setChannel((int) channel.getValue());
            channel.addChangeListener(channelChangeListener);
            mass.setValue(compOpts.getMass());
            massChangeListener = _ -> compOpts.setMass((double) mass.getValue());
            mass.addChangeListener(massChangeListener);
            radius.setValue(compOpts.getRadius());
            radiusChangeListener = _ -> compOpts.setRadius((int) radius.getValue());
            radius.addChangeListener(radiusChangeListener);

            // populate rulesPanel
            var selectedRule = (AbstractMusicianRule) selectedMusician.getMusician().getRule();
            var ruleParams = selectedRule.getSettableParameters();
            var ruleOpts = Options.getInstance().getMusicians().get(selectedMusician.getMusician().getId())
                    .getRuleOptions();
            SwingUtilities.invokeLater(() -> {
                var row = 0;
                for (var ruleParam : ruleParams) {
                    paramComps[row].removeAll();
                    switch (ruleParam) {
                    case NumericParamDescription(String propertyName, String displayName, Class<? extends Number> type, Double minValue, Double maxValue, Double step): {
                        var comp = createSpinner(propertyName,
                                ((Number) ruleOpts.getRuleSpecificOptions().getOrDefault(propertyName, minValue))
                                        .doubleValue(),
                                minValue, maxValue, step, type);
                        comp.addChangeListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getValue()));
                        var label = createLabelFor(displayName, comp);
                        paramComps[row].add(label);
                        paramComps[row].add(comp);
                        row++;
                    }
                        break;
                    case BooleanParamDescription(String propertyName, String displayName): {
                        var comp = new JCheckBox(displayName);
                        comp.setSelected((Boolean) ruleOpts.getRuleSpecificOptions().get(propertyName));
                        comp.addChangeListener(_ -> ruleOpts.setRuleSpecificOption(propertyName, comp.isSelected()));
                        paramComps[row].add(Box.createHorizontalStrut(1));
                        paramComps[row].add(comp);
                        row++;
                    }
                        break;
                    case StringListParamDescription(String propertyName, String displayName, List<String> possibleValues): {
                        var model = new DefaultComboBoxModel<String>(possibleValues.toArray(new String[0]));
                        var comp = new JComboBox<String>(model);
                        comp.setSelectedItem(ruleOpts.getRuleSpecificOptions().get(propertyName));
                        comp.addActionListener(
                                _ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                        var label = new JLabel(displayName);
                        label.setLabelFor(comp);
                        paramComps[row].add(label);
                        paramComps[row].add(comp);
                        row++;
                    }
                        break;
                    case EnumParamDescription(String propertyName, String displayName, Enum<?> defaultValue): {
                        @SuppressWarnings("unchecked")
                        var comp = createEnumComboBox(defaultValue.getClass());
                        comp.setSelectedItem(ruleOpts.getRuleSpecificOptions().get(propertyName));
                        comp.addActionListener(
                                _ -> ruleOpts.setRuleSpecificOption(propertyName, comp.getSelectedItem()));
                        var label = new JLabel(displayName);
                        label.setLabelFor(comp);
                        paramComps[row].add(label);
                        paramComps[row].add(comp);
                        row++;
                    }
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "I haven't been programmed to understand a " + ruleParam.getClass().getName());
                    }
                }
                ruleOptsPanel.revalidate();
                ruleOptsPanel.repaint();
            });
        }
    }

    private static <E extends Enum<E>> JComboBox<E> createEnumComboBox(Class<E> clazz) {
        var model = new DefaultComboBoxModel<E>(clazz.getEnumConstants());
        return new JComboBox<E>(model);
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
        mass.setValue(MusicianComponent.DEFAULT_MASS);
        if (radiusChangeListener != null) {
            radius.removeChangeListener(radiusChangeListener);
            radiusChangeListener = null;
        }
        radius.setValue(MusicianComponentOptions.DEFAULT_RADIUS);
        for (var paramPanel : paramComps) {
            paramPanel.removeAll();
            paramPanel.revalidate();
        }
    }

}
