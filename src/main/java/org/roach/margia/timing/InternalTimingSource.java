package org.roach.margia.timing;

import java.util.concurrent.*;

import org.roach.margia.Transport;

/**
 * Use internal system clock as timing source
 */
public class InternalTimingSource implements TimingSource {
	private final Transport transport;
	private Future<?> clockFuture;
	private final int tickLengthMicros;
	private final ScheduledExecutorService clockExecutor;

	/**
	 * @param transport the transport to control
	 * @param tempo     the tempo in bpm
	 */
	public InternalTimingSource(Transport transport, int tempo) {
		this.transport = transport;
		this.tickLengthMicros = 60000000 / (tempo * 24);
		clockExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	@Override
	public void start() {
		clockFuture = clockExecutor.scheduleAtFixedRate(() -> {
			transport.receiveClockPulse();
		}, 0, tickLengthMicros, TimeUnit.MICROSECONDS);
	}

	@Override
	public void stop() {
		if (clockFuture != null) {
			clockFuture.cancel(true);
		}
		clockExecutor.shutdownNow();
	}

}
