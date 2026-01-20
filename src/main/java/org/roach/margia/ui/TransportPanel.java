package org.roach.margia.ui;

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import org.roach.margia.Transport;
import org.roach.margia.storage.Options;
import org.roach.margia.timing.TimingSource;

/**
 * Swing UI for controlling / viewing the {@link Transport} and
 * {@link TimingSource}
 */
public class TransportPanel extends JPanel implements PropertyChangeListener {
    private final JLabel measure;
    private final JLabel beat;
    private final JLabel clockPulse;
    private final JLabel tick;
    private final JSpinner tempo;

    /**
     * @param timing    the {@link TimingSource}
     * @param transport the {@link Transport}
     */
    public TransportPanel(TimingSource timing, Transport transport) {
        super(new FlowLayout(FlowLayout.CENTER, 3, 3));

        transport.addPropertyListener(this);

        add(new JLabel("Time:"));
        measure = new JLabel("000");
        add(measure);
        add(new JLabel(":"));
        beat = new JLabel("0");
        add(beat);
        add(new JLabel("."));
        clockPulse = new JLabel("00");
        add(clockPulse);
        tick = new JLabel(" (000)");
        add(tick);

        add(new JLabel("Tempo: "));
        var tempoModel = new SpinnerNumberModel(timing.getTempo(), 1, 400, 1);
        tempo = new JSpinner(tempoModel);
        tempo.setName("tempo");
        tempo.addChangeListener(_ -> Options.getInstance().getMusicOptions().setTempo((int) tempo.getValue()));
        add(tempo);

        var startIcon = Icons.getButtonIcon(Icons.START);
        var rewindIcon = Icons.getButtonIcon(Icons.REWIND);
        var pauseIcon = Icons.getButtonIcon(Icons.PAUSE);
        
        var startBtn = new JButton(startIcon);
        startBtn.addActionListener(_ -> {
            if (timing.isRunning()) {
                timing.stop();
                startBtn.setIcon(startIcon);
            } else {
                timing.start();
                startBtn.setIcon(pauseIcon);
            }
        });
        add(startBtn);
        var rewindBtn = new JButton(rewindIcon);
        rewindBtn.addActionListener(_ -> {
            timing.stop();
            transport.reset();
            startBtn.setIcon(startIcon);
        });
        add(rewindBtn);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        var propName = evt.getPropertyName();
        SwingUtilities.invokeLater(() -> {
            switch (propName) {
            case Transport.MEASURE_PROPERTY:
                measure.setText(String.format("%03d", (int) evt.getNewValue()));
                break;
            case Transport.BEAT_PROPERTY:
                beat.setText(String.format("%01d", (int) evt.getNewValue()));
                break;
            case Transport.CLOCK_PULSE_PROPERTY:
                clockPulse.setText(String.format("%02d", (int) evt.getNewValue()));
                break;
            case Transport.TICK_PROPERTY:
                tick.setText(String.format(" (%03d)", (long) evt.getNewValue()));
                break;
            default:
            }
        });
    }

    /**
     * @param listener a listener for tempo changes
     */
    public void addTempoListener(ChangeListener listener) {
        this.tempo.addChangeListener(listener);
    }
}
