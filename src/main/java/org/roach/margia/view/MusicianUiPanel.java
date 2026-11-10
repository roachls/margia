package org.roach.margia.view;

import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.roach.margia.model.MusicianComponentOptions;

public class MusicianUiPanel extends VBox {

    @FXML private Label titleLabel;
    @FXML private Spinner<Integer> radius;

    private final MusicianComponentOptions options;
    private final int id;

    public MusicianUiPanel(int id, MusicianComponentOptions options) {
        this.id = id;
        this.options = options;
        loadFxml();

        titleLabel.setText(OptionsWindow.MUSICIAN + id + " UI Options");
        radius.getValueFactory().setValue(options.getRadius());

        radius.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                options.setRadius(newVal);
            }
        });
    }

    // Called for multiple edits
    MusicianUiPanel(List<MusicianUiPanel> others) {
        this.id = -1; // not used
        this.options = new MusicianComponentOptions();
        loadFxml();

        var idList = others.stream().map(p -> p.id).toList();
        var optionsList = others.stream().map(p -> p.options).toList();
        var firstRadius = optionsList.get(0).getRadius();
        var allRadiiSame = optionsList.stream()
                .map(MusicianComponentOptions::getRadius)
                .allMatch(r -> firstRadius == r);

        if (allRadiiSame) {
            options.setRadius(firstRadius);
        }

        titleLabel.setText(OptionsWindow.MUSICIAN + idList + " UI Options");
        radius.getValueFactory().setValue(options.getRadius());

        if (!allRadiiSame) {
            // Highlight mismatched values across selected musicians
            radius.setStyle("-fx-base: #ffcccc;");
            radius.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
        }

        radius.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                // Clear highlight once the user sets a unified value
                radius.setStyle("");
                radius.getEditor().setStyle("");
                others.forEach(o -> o.radius.getValueFactory().setValue(newVal));
            }
        });
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MusicianUiPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MusicianUiPanel.fxml", e);
        }
    }

    @Override
    public String toString() {
        return "UI";
    }
}
