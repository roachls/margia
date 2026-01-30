package org.roach.margia.timing;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import javax.sound.midi.ShortMessage;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.MidiController;
import org.roach.margia.Transport;
import org.roach.margia.MidiController.MidiReceiver;
import org.roach.margia.storage.Options;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.quantity.time.TimeQuantities;

/**
 * Use internal system clock as timing source
 */
public class InternalTimingSource implements TimingSource, ChangeListener, MidiReceiver {
    private final Transport transport;
    private Future<?> clockFuture;
    private AtomicReference<Quantity<Time>> tickLengthMicros;
    private final ScheduledExecutorService clockExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * @param transport the transport to control
     */
    public InternalTimingSource(Transport transport) {
        this.transport = transport;
        clockExecutor = Executors.newSingleThreadScheduledExecutor();
        Options.getInstance().getMusicOptions().addChangeListener(TEMPO_PROPERTY, this);
        updateTempo();
        MidiController.getInstance().registerWithAllExternalReceivers(this);
    }

    @Override
    public void updateTempo() {
        var tempo = Options.getInstance().getMusicOptions().getTempo();
        this.tickLengthMicros = new AtomicReference<>(
                Quantities.getQuantity(60000000 / (tempo * 24), TimeQuantities.MICROSECOND));

        if (isRunning()) {
            stopClock();
            startClock();
        }
    }

    @Override
    public int getTempo() { return Options.getInstance().getMusicOptions().getTempo(); }

    private void startClock() {
        running.set(true);
        clockFuture = clockExecutor.scheduleAtFixedRate(transport::receiveClockPulse, 0,
                tickLengthMicros.get().getValue().longValue(), TimeUnit.MICROSECONDS);
    }

    @Override
    public void start() {
        transport.start();
        startClock();
    }

    private void stopClock() {
        if (clockFuture != null) {
            clockFuture.cancel(true);
            clockFuture = null;
        }

    }

    @Override
    public void stop() {
        transport.stop();
        stopClock();
        running.set(false);
    }

    @Override
    public boolean isRunning() { return running.get(); }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource cs && TEMPO_PROPERTY.equals(cs.key())) {
            updateTempo();
        }
    }

    @Override
    public void receive(ShortMessage message) {
        switch (message.getCommand()) {
        case ShortMessage.START, ShortMessage.CONTINUE:
            start();
            break;
        case ShortMessage.STOP:
            stop();
            break;
        case ShortMessage.CONTROL_CHANGE:
            if (message.getData1() == 117 && message.getData2() == 0) {
                // start button released
                start();
            } else if (message.getData1() == 116 && message.getData2() == 0) {
                // stop button released
                stop();
            } else if (message.getData1() == 114 && message.getData2() == 0) {
                // rewind button released
                stop();
                transport.reset();
            } else if (message.getData1() == 1) {
//                System.out.println("mod wheel: " + message.getData2());
            } else {
//                System.out.printf("Control change: %d %d%n", message.getData1(), message.getData2());
            }
            break;
        case ShortMessage.PITCH_BEND:
//            System.out.printf("pitch bend: %d %d%n", message.getData1(), message.getData2());
            break;
        case ShortMessage.NOTE_ON:
            if (Options.getInstance().getMidiOptions().isAutoStartOnNoteOn())
                start();
            break;
        default:
//            System.out.println("Received: " + message.getCommand());
            break;
        }
    }

}
