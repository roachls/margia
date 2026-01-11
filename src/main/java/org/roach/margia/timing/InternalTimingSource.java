package org.roach.margia.timing;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.roach.margia.Transport;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.quantity.time.TimeQuantities;

/**
 * Use internal system clock as timing source
 */
public class InternalTimingSource implements TimingSource {
    private final Transport transport;
    private Future<?> clockFuture;
    private volatile int tempo;
    private AtomicReference<Quantity<Time>> tickLengthMicros;
    private final ScheduledExecutorService clockExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * @param transport the transport to control
     * @param tempo     the tempo in bpm
     */
    public InternalTimingSource(Transport transport, int tempo) {
        this.transport = transport;
        clockExecutor = Executors.newSingleThreadScheduledExecutor();
        setTempo(tempo);
    }

    @Override
    public void setTempo(int tempo) {
        this.tempo = tempo;
        this.tickLengthMicros = new AtomicReference<>(
                Quantities.getQuantity(60000000 / (tempo * 24), TimeQuantities.MICROSECOND));

        if (isRunning()) {
            stopClock();
            startClock();
        }
    }

    @Override
    public int getTempo() { return tempo; }

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

}
