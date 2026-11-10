package org.roach.margia.view;

import java.beans.PropertyChangeListener;

import org.roach.margia.controller.Transport;
import org.roach.margia.controller.timing.TimingSource;
import org.roach.margia.storage.Options;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Swing UI for controlling / viewing the {@link Transport} and
 * {@link TimingSource}
 */
public class TransportPanel extends HBox implements PropertyChangeListener {
    private final Label measure;
    private final Label beat;
    private final Label clockPulse;
    private final Label tick;
    private final Spinner<Integer> tempo;
    private transient TimingSource timing;
    private final Button startBtn;
    private final Button rewindBtn;
    private final ImageView startIcon;
    private final ImageView pauseIcon;
    private ObjectProperty<Integer> tempoObjectProperty;

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     */
    public TransportPanel(TimingSource timing, Transport transport) {
        super(3); // 3px horizontal spacing between children
        setAlignment(Pos.CENTER);
        setPadding(new Insets(3));
        this.timing = timing;

        transport.addPropertyListener(this);

        measure = new Label("000");
        beat = new Label("0");
        clockPulse = new Label("00");
        tick = new Label(" (000)");

        tempo = new Spinner<>(1, 400, Options.getInstance().getMusicOptions().getTempo(), 1);
        tempo.setId("tempo");
        tempo.setEditable(true);
        tempo.setPrefWidth(75);
        tempoObjectProperty = Options.getInstance().getMusicOptions().tempoProperty().asObject();
        tempo.getValueFactory().valueProperty().bindBidirectional(tempoObjectProperty);

        startIcon = Icons.getButtonIcon(Icons.START);
        var rewindIcon = Icons.getButtonIcon(Icons.REWIND);
        pauseIcon = Icons.getButtonIcon(Icons.PAUSE);

        startBtn = new Button(null, startIcon);
        startBtn.setFocusTraversable(false);
        startBtn.setOnAction(_ -> toggleStartStop());

        rewindBtn = new Button(null, rewindIcon);
        rewindBtn.setFocusTraversable(false);
        rewindBtn.setOnAction(_ -> {
            timing.stop();
            transport.reset();
            startBtn.setGraphic(startIcon);
        });

        boolean externalTiming = Options.getInstance().getMidiOptions().isUsingExternalTiming();
        startBtn.setDisable(externalTiming);
        rewindBtn.setDisable(externalTiming);
        tempo.setDisable(externalTiming);

        getChildren().addAll(new Label("Time:"), measure, new Label(":"), beat, new Label("."), clockPulse, tick,
                new Label("Tempo: "), tempo, startBtn, rewindBtn);
    }

    void toggleStartStop() {
        if (timing.isRunning()) {
            timing.stop();
            startBtn.setGraphic(startIcon);
        } else {
            timing.start();
            startBtn.setGraphic(pauseIcon);
        }
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
