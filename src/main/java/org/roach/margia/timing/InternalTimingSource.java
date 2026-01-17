package org.roach.margia.timing;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Transport;
import org.roach.margia.storage.Options;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.quantity.time.TimeQuantities;

/**
 * Use internal system clock as timing source
 */
public class InternalTimingSource implements TimingSource, ChangeListener {
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

}
