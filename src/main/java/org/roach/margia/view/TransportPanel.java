package org.roach.margia.view;

import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.TimingSource;
import org.roach.margia.storage.Options;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Swing UI for controlling / viewing the {@link Transport} and
 * {@link TimingSource}
 */
public class TransportPanel extends HBox implements PropertyChangeListener {
    @FXML private Label measure;
    @FXML private Label beat;
    @FXML private Label clockPulse;
    @FXML private Label tick;
    @FXML private Spinner<Integer> tempo;
    @FXML private Button startBtn;
    @FXML private Button rewindBtn;
    private final transient TimingSource timing;
    private final Transport transport;
    private final ImageView startIcon;
    private final ImageView pauseIcon;
    private ObjectProperty<Integer> tempoObjectProperty;

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     */
    public TransportPanel(TimingSource timing, Transport transport) {
        this.timing = timing;
        this.transport = transport;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransportPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load TransportPanel.fxml", exception);
        }

        // Initialize state and bindings after FXML nodes are injected
        this.transport.addPropertyListener(this);

        tempo.getValueFactory().setValue(Options.getInstance().getMusicOptions().getTempo());
        tempoObjectProperty = Options.getInstance().getMusicOptions().tempoProperty().asObject();
        tempo.getValueFactory().valueProperty().bindBidirectional(tempoObjectProperty);

        startIcon = Icons.getButtonIcon(Icons.START);
        pauseIcon = Icons.getButtonIcon(Icons.PAUSE);
        startBtn.setGraphic(startIcon);
        rewindBtn.setGraphic(Icons.getButtonIcon(Icons.REWIND));

        boolean externalTiming = Options.getInstance().getMidiOptions().isUsingExternalTiming();
        startBtn.setDisable(externalTiming);
        rewindBtn.setDisable(externalTiming);
        tempo.setDisable(externalTiming);
    }
    
    @FXML
    void toggleStartStop() {
        if (timing.isRunning()) {
            timing.stop();
            startBtn.setGraphic(startIcon);
        } else {
            timing.start();
            startBtn.setGraphic(pauseIcon);
        }
    }
    
    @FXML
    private void handleRewind() {
        timing.stop();
        transport.reset();
        startBtn.setGraphic(startIcon);
    }

    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        var propName = evt.getPropertyName();
        Platform.runLater(() -> {
            switch (propName) {
            case Transport.MEASURE_PROPERTY -> measure.setText(String.format("%03d", (int) evt.getNewValue()));
            case Transport.BEAT_PROPERTY -> beat.setText(String.format("%01d", (int) evt.getNewValue()));
            case Transport.CLOCK_PULSE_PROPERTY -> clockPulse.setText(String.format("%02d", (int) evt.getNewValue()));
            case Transport.TICK_PROPERTY -> tick.setText(String.format(" (%03d)", (long) evt.getNewValue()));
            default -> {
                // do nothing
            }
            }
        });
    }

    void initFromOptions() {
//        tempo.getValueFactory().setValue(Options.getInstance().getMusicOptions().getTempo());
        boolean externalTiming = Options.getInstance().getMidiOptions().isUsingExternalTiming();
        startBtn.setDisable(externalTiming);
        rewindBtn.setDisable(externalTiming);
        tempo.setDisable(externalTiming);
    }

}
