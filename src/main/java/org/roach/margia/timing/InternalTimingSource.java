package org.roach.margia.timing;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.roach.margia.Transport;

/**
 * Use internal system clock as timing source
 */
public class InternalTimingSource implements TimingSource {
	private final Transport transport;
	private Future<?> clockFuture;
	private volatile int tempo;
	private volatile int tickLengthMicros;
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
		this.tickLengthMicros = 60000000 / (tempo * 24);
		
		if (isRunning()) {
			stopClock();
			startClock();
		}
	}

	@Override
	public int getTempo() {
		return tempo;
	}

	private void startClock() {
		running.set(true);
		clockFuture = clockExecutor.scheduleAtFixedRate(transport::receiveClockPulse, 0, tickLengthMicros,
				TimeUnit.MICROSECONDS);
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
	public boolean isRunning() {
		return running.get();
	}

}
