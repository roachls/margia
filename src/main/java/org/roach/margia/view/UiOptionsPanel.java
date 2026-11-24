package org.roach.margia.view;

import java.io.IOException;

import org.roach.margia.storage.Options;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Panel for selecting global UI options
 */
public class UiOptionsPanel extends VBox {

    @FXML
    Spinner<Double> gravity;
    @FXML
    Spinner<Double> windSpeed;
    @FXML
    Spinner<Integer> edgeLength;
    @FXML
    CheckBox showIcons;
    @FXML
    CheckBox animateBackground;
    @FXML
    Spinner<Double> rippleExpansionSpeed;
    @FXML
    ColorPicker colorPicker;
    @FXML
    Spinner<Integer> maxActiveRipples;
    @FXML
    Spinner<Double> rippleScreenFraction;

    /**
     * constructor
     */
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
        rippleExpansionSpeed.getValueFactory().setValue(uiOptions.getRippleExpansionSpeed());
        colorPicker.setValue(uiOptions.getBackgroundColor());
        maxActiveRipples.getValueFactory().setValue(uiOptions.getMaxActiveRipples());
        rippleScreenFraction.getValueFactory().setValue(uiOptions.getRippleScreenFraction());

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

        showIcons.selectedProperty().addListener((_, _, newVal) -> uiOptions.setShowNumbers(newVal));

        animateBackground.selectedProperty().addListener((_, _, newVal) -> uiOptions.setAnimateBackground(newVal));

        rippleExpansionSpeed.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setRippleExpansionSpeed(newVal);
            }
        });

        maxActiveRipples.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setMaxActiveRipples(newVal);
            }
        });

        rippleScreenFraction.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setRippleScreenFraction(newVal);
            }
        });

        colorPicker.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                uiOptions.setBackgroundColor(newVal);
            }
        });
    }

    @Override
    public String toString() {
        return "UI";
    }
}
