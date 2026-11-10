package org.roach.margia.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.model.RuleOptions;

public class MusicianRuleOptionsPanel extends VBox {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> rule;

    private final Map<String, MusicianRule> availableRules = new HashMap<>();
    private final List<String> ruleNames = new ArrayList<>();
    private final int id;
    private final RuleOptions options;

    public MusicianRuleOptionsPanel(int id, RuleOptions options) {
        this.id = id;
        this.options = options;
        loadFxml();

        titleLabel.setText(OptionsWindow.MUSICIAN + id + " rule options");
        rule.setValue(options.getName());

        rule.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setName(newVal);
            }
        });
    }

    // Multiple selection
    MusicianRuleOptionsPanel(List<MusicianRuleOptionsPanel> others) {
        this.id = -1; // unused
        this.options = new RuleOptions();
        loadFxml();

        var idList = others.stream().map(p -> p.id).toList();
        titleLabel.setText(OptionsWindow.MUSICIAN + idList + " rule options");

        var optionsList = others.stream().map(p -> p.options).toList();
        var firstName = optionsList.get(0).getName();
        var allNamesSame = optionsList.stream()
                .map(RuleOptions::getName)
                .allMatch(name -> Objects.equals(firstName, name));

        if (allNamesSame) {
            options.setName(firstName == null ? "" : firstName);
        } else {
            options.setName("");
            rule.setStyle("-fx-base: #ffcccc;");
        }

        rule.setValue(options.getName());
        rule.valueProperty().addListener((_, _, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                return;
            }
            rule.setStyle("");
            options.setName(newVal);
            others.forEach(o -> o.rule.setValue(newVal));
        });
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
        ServiceLoader.load(MusicianRule.class).forEach(r -> {
            availableRules.put(r.getName(), r);
            ruleNames.add(r.getName());
        });
        rule.getItems().addAll(ruleNames);
    }

    @Override
    public String toString() {
        return "Rule";
    }
}
