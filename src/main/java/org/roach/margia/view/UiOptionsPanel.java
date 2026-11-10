package org.roach.margia.view;

import java.io.IOException;

import org.roach.margia.storage.Options;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;

public class UiOptionsPanel extends VBox {

    @FXML Spinner<Double> gravity;
    @FXML Spinner<Double> windSpeed;
    @FXML Spinner<Integer> edgeLength;
    @FXML CheckBox showIcons;
    @FXML CheckBox animateBackground;

    public UiOptionsPanel() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UiOptionsPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load UiOptionsPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        var uiOptions = Options.getInstance().getUiOptions();

        // Initialize values from Options
        gravity.getValueFactory().setValue(uiOptions.getGravity());
        windSpeed.getValueFactory().setValue(uiOptions.getWindSpeed());
        edgeLength.getValueFactory().setValue(uiOptions.getEdgeLength());
        showIcons.setSelected(uiOptions.isShowNumbers());
        animateBackground.setSelected(uiOptions.isAnimateBackground());

        // Attach change listeners
        gravity.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setGravity(newVal);
            }
        });

        windSpeed.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setWindSpeed(newVal);
            }
        });

        edgeLength.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setEdgeLength(newVal);
            }
        });

        showIcons.selectedProperty().addListener((_, _, newVal) ->
                uiOptions.setShowNumbers(newVal));

        animateBackground.selectedProperty().addListener((_, _, newVal) ->
                uiOptions.setAnimateBackground(newVal));
    }

    @Override
    public String toString() {
        return "UI";
    }
}
