package org.roach.margia.view;

import java.io.IOException;
import java.util.*;

import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.controller.rules.RulePropertyDescriptor;
import org.roach.margia.controller.rules.RulePropertyDescriptor.*;
import org.roach.margia.controller.rules.storage.RuleRepository;
import org.roach.margia.model.RuleOptions;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Panel for setting musician rule options
 */
public class MusicianRuleOptionsPanel extends VBox {

    private static final String MISMATCH_STYLE = "-fx-base: #ffcccc;";

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<String> rule;
    @FXML
    private GridPane optionsGrid;

    private final Map<String, MusicianRule> availableRules = new HashMap<>();
    private final List<String> ruleNames = new ArrayList<>();
    private final int id;
    private final RuleOptions options;
    private final List<MusicianRuleOptionsPanel> selectedPanels;

    /**
     * Constructor
     * 
     * @param id      id of musician
     * @param options options to display
     */
    public MusicianRuleOptionsPanel(int id, RuleOptions options) {
        this.id = id;
        this.options = options;
        this.selectedPanels = List.of();
        loadFxml();

        titleLabel.setText(id < 0 ? "Default rule options" : OptionsWindow.MUSICIAN + id + " rule options");
        rule.setValue(options.getName());
        rebuildDynamicControls(options.getName());

        rule.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setName(newVal);
                rebuildDynamicControls(newVal);
            }
        });
    }

    // Multiple selection
    MusicianRuleOptionsPanel(List<MusicianRuleOptionsPanel> others) {
        this.id = -1; // unused
        this.options = new RuleOptions();
        this.selectedPanels = List.copyOf(others);
        loadFxml();

        var idList = others.stream().map(p -> p.id).toList();
        titleLabel.setText(OptionsWindow.MUSICIAN + idList + " rule options");

        var optionsList = others.stream().map(p -> p.options).toList();
        var firstName = optionsList.get(0).getName();
        var allNamesSame = optionsList.stream().map(RuleOptions::getName)
                .allMatch(name -> Objects.equals(firstName, name));

        if (allNamesSame) {
            options.setName(firstName == null ? "" : firstName);
        } else {
            options.setName("");
            rule.setStyle(MISMATCH_STYLE);
        }

        rule.setValue(options.getName());
        rebuildDynamicControls(options.getName());

        rule.valueProperty().addListener((_, _, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                return;
            }
            rule.setStyle("");
            options.setName(newVal);
            others.forEach(o -> o.rule.setValue(newVal));
            rebuildDynamicControls(newVal);
        });
    }

    private void rebuildDynamicControls(String ruleName) {
        // Remove all rows after row 0 (the Rule selector row)
        optionsGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row > 0;
        });

        if (ruleName == null || ruleName.isEmpty()) {
            return;
        }

        var selectedRule = availableRules.get(ruleName);
        if (selectedRule == null) {
            return;
        }

        var descriptors = selectedRule.getPropertyDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            int rowIndex = i + 1;
            var desc = descriptors.get(i);
            var label = new Label(desc.label());
            var control = createControlForDescriptor(desc);
            label.setLabelFor(control);

            if (control instanceof Region region && !(control instanceof CheckBox)) {
                region.setMaxWidth(Double.MAX_VALUE);
            }

            optionsGrid.add(label, 0, rowIndex);
            optionsGrid.add(control, 1, rowIndex);
        }
    }

    private Control createControlForDescriptor(RulePropertyDescriptor desc) {
        boolean isMulti = !selectedPanels.isEmpty();
        boolean allSame = true;
        Object resolvedValue;

        if (isMulti) {
            var firstVal = selectedPanels.get(0).options.getRuleSpecificOptionOrDefault(desc.key(),
                    desc.defaultValue());
            allSame = selectedPanels.stream()
                    .map(p -> p.options.getRuleSpecificOptionOrDefault(desc.key(), desc.defaultValue()))
                    .allMatch(v -> Objects.equals(firstVal, v));
            resolvedValue = firstVal;
        } else {
            resolvedValue = options.getRuleSpecificOptionOrDefault(desc.key(), desc.defaultValue());
        }

        Control control = switch (desc) {
        case IntProperty ip -> buildIntSpinner(ip, resolvedValue);
        case DoubleProperty dp -> buildDoubleSpinner(dp, resolvedValue);
        case BooleanProperty bp -> buildCheckBox(bp, resolvedValue, !allSame);
        case EnumProperty<?> ep -> buildEnumComboBox(ep, resolvedValue, !allSame);
        case StringListProperty slp -> buildStringListComboBox(slp, resolvedValue, !allSame);
        case StringProperty sp -> buildTextField(sp, resolvedValue, !allSame);
        };

        if (!allSame) {
            control.setStyle(MISMATCH_STYLE);
        }
        return control;
    }

    private Spinner<Integer> buildIntSpinner(IntProperty prop, Object currentVal) {
        int initial = (currentVal instanceof Number n) ? n.intValue() : prop.defaultValue();
        var spinner = new Spinner<Integer>(prop.min(), prop.max(), initial, prop.step());
        spinner.setEditable(true);

        spinner.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                spinner.setStyle("");
                applyOptionValue(prop.key(), newVal);
            }
        });
        return spinner;
    }

    private Spinner<Double> buildDoubleSpinner(DoubleProperty prop, Object currentVal) {
        double initial = (currentVal instanceof Number n) ? n.doubleValue() : prop.defaultValue();
        var spinner = new Spinner<Double>(prop.min(), prop.max(), initial, prop.step());
        spinner.setEditable(true);

        spinner.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                spinner.setStyle("");
                applyOptionValue(prop.key(), newVal);
            }
        });
        return spinner;
    }

    private CheckBox buildCheckBox(BooleanProperty prop, Object currentVal, boolean mismatch) {
        boolean initial = (currentVal instanceof Boolean b) ? b : prop.defaultValue();
        var checkBox = new CheckBox();
        if (mismatch) {
            checkBox.setIndeterminate(true);
        } else {
            checkBox.setSelected(initial);
        }

        checkBox.selectedProperty().addListener((_, _, newVal) -> {
            checkBox.setStyle("");
            applyOptionValue(prop.key(), newVal);
        });
        return checkBox;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ComboBox buildEnumComboBox(EnumProperty<?> prop, Object currentVal, boolean mismatch) {
        var combo = new ComboBox<>();
        combo.getItems().addAll(prop.values());

        if (!mismatch && currentVal != null) {
            Object resolvedEnum = currentVal;
            if (currentVal instanceof String s && prop.defaultValue() != null) {
                resolvedEnum = Enum.valueOf((Class) prop.defaultValue().getClass(), s);
            }
            combo.setValue(resolvedEnum);
        }

        combo.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                combo.setStyle("");
                applyOptionValue(prop.key(), newVal);
            }
        });
        return combo;
    }

    private ComboBox<String> buildStringListComboBox(StringListProperty prop, Object currentVal, boolean mismatch) {
        var combo = new ComboBox<String>();
        combo.getItems().addAll(prop.values());

        if (!mismatch && currentVal != null) {
            combo.setValue(currentVal.toString());
        }

        combo.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                combo.setStyle("");
                applyOptionValue(prop.key(), newVal);
            }
        });
        return combo;
    }

    private TextField buildTextField(StringProperty prop, Object currentVal, boolean mismatch) {
        var textField = new TextField(mismatch || currentVal == null ? "" : currentVal.toString());

        textField.textProperty().addListener((_, _, newVal) -> {
            textField.setStyle("");
            applyOptionValue(prop.key(), newVal);
        });
        return textField;
    }

    private void applyOptionValue(String key, Object value) {
        options.setRuleSpecificOption(key, value);
        if (!selectedPanels.isEmpty()) {
            selectedPanels.forEach(panel -> {
                panel.options.setRuleSpecificOption(key, value);
                panel.rebuildDynamicControls(panel.options.getName());
            });
        }
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MusicianRuleOptionsPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MusicianRuleOptionsPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        refreshAvailableRules();
        RuleRepository.getInstance().addRepositoryChangeListener(() -> Platform.runLater(this::refreshAvailableRules));
    }

    private void refreshAvailableRules() {
        String previouslySelected = rule.getValue();
        availableRules.clear();
        ruleNames.clear();

        for (String name : RuleRepository.getInstance().getAllRuleNames()) {
            RuleRepository.getInstance().createRuleByName(name).ifPresent(r -> {
                availableRules.put(name, r);
                ruleNames.add(name);
            });
        }

        rule.getItems().setAll(ruleNames);
        if (previouslySelected != null && ruleNames.contains(previouslySelected)) {
            rule.setValue(previouslySelected);
        }
    }

    @Override
    public String toString() {
        return "Rule";
    }
}