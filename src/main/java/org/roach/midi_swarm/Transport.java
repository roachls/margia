package org.roach.midi_swarm;

import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the "clock" that drives everything. It issues a "tick" once every
 * 16th note.
 */
public class Transport {
	private final List<Musician> musicians;
	private long tick;
	private final Logger logger = LogManager.getLogger(getClass());
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "transport");
		}
		
	});
	private Future<?> future;
	private final int tickLength;

	/**
	 * @param musicians the musicians
	 * @param tempo     the tempo
	 */
	public Transport(final List<Musician> musicians, int tempo) {
		this.musicians = musicians;
		this.tickLength = Length.L1_16.getMillisForTempo(tempo);
	}

	/**
	 * @return the number of the latest tick
	 */
	public long getTick() {
		return this.tick;
	}

	/**
	 * @return the length of a tick in milliseconds
	 */
	public int getTickLength() {
		return this.tickLength;
	}

	/**
	 * Start the clock
	 */
	public void start() {
		future = executor.scheduleAtFixedRate(() -> {
			logger.atInfo().log("Tick: {}", tick);
			musicians.forEach(m -> m.calculateAction(tick));
			musicians.forEach(m -> m.doAction(tick));
			tick++;
		}, 0, tickLength, TimeUnit.MILLISECONDS);
	}

	/**
	 * Stop the clock
	 */
	public void stop() {
		if (future != null) {
			future.cancel(false);
		}
		executor.shutdownNow();
		future = null;
	}
}
