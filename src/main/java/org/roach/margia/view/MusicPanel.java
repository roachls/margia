package org.roach.margia.view;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.roach.margia.model.MusicOptions;
import org.roach.margia.storage.Options;

public class MusicPanel extends VBox {
    private static final String GLOBAL_MUSIC_OPTIONS = "Global Music Options";

    @FXML
    Spinner<Integer> maxQueueSize;

    public MusicPanel() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MusicPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MusicPanel.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        maxQueueSize.getValueFactory().setValue(MusicOptions.DEFAULT_MAX_QUEUE_SIZE);

        maxQueueSize.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                Options.getInstance().getMusicOptions().setMaxQueueSize(newVal);
            }
        });
    }

    @Override
    public String toString() {
        return GLOBAL_MUSIC_OPTIONS;
    }
}
