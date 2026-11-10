package org.roach.margia.view;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.roach.margia.storage.Options;

public class MiscPanel extends VBox {

    @FXML
    Spinner<Long> randomSeedSpinner;

    public MiscPanel() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MiscPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MiscPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        // Reuse the Long SpinnerValueFactory from OptionsWindow.createSpinner
        Spinner<Long> configured = OptionsWindow.createSpinner("Random seed", Options.getInstance().getRandomSeed(),
                -Long.MAX_VALUE, Long.MAX_VALUE, 1L);
        randomSeedSpinner.setValueFactory(configured.getValueFactory());
        randomSeedSpinner.getEditor().setPrefColumnCount(15);

        randomSeedSpinner.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                Options.getInstance().setRandomSeed(newVal);
            }
        });
    }

    @Override
    public String toString() {
        return "Miscellaneous";
    }
}
