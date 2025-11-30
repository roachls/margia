package org.roach.midi_swarm;

import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.midi_swarm.util.NamedThreadFactory;

/**
 * This is the "clock" that drives everything. It issues a "tick" once every
 * 16th note.
 */
public class Transport {
	private final List<Musician> musicians;
	private long tick = 1;
	private final Logger logger = LogManager.getLogger(getClass());
	private final Map<Long, Runnable> tickActions = new HashMap<>();
	private final MidiController controller;
	private ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(new NamedThreadFactory("transport"));
	private ScheduledExecutorService clockExecutor = Executors
			.newSingleThreadScheduledExecutor(new NamedThreadFactory("transport"));
	private Future<?> future;
	private Future<?> clockFuture;
	private final int tickLength;
	private final long timePulseInterval;
	private boolean controlDawTiming;

	/**
	 * @param musicians  the musicians
	 * @param tempo      the tempo
	 * @param controller the MIDI controller
	 */
	public Transport(final List<Musician> musicians, int tempo, final MidiController controller) {
		this.musicians = musicians;
		this.tickLength = Length.getMillisForTempo(1, tempo);
		this.controller = controller;
		this.timePulseInterval = (long) ((60000.0 / tempo) / 24.0);
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
		if (controlDawTiming) {
			controller.sendStart();
			clockFuture = clockExecutor.scheduleAtFixedRate(() -> controller.sendClockPulse(), 0, timePulseInterval,
					TimeUnit.MILLISECONDS);
		}
		future = executor.scheduleAtFixedRate(() -> {
			logger.atInfo().log("Tick: {}", tick);
			if (tickActions.containsKey(tick)) {
				logger.atDebug().log("Transport playing tick action {}", tick);
				tickActions.get(tick).run();
				tickActions.remove(tick);
			}
			musicians.forEach(m -> m.calculateAction(tick));
			musicians.forEach(m -> m.doAction(tick));
			controller.playNotesThisTick();

			tick++;
		}, 0, tickLength, TimeUnit.MILLISECONDS);
	}

	/**
	 * Stop the clock
	 */
	public void stop() {
		if (future != null) {
			future.cancel(true);
		}
		if (clockFuture != null) {
			clockFuture.cancel(true);
		}
		if (controlDawTiming)
			controller.sendStop();
		executor.shutdownNow();
		clockExecutor.shutdownNow();
		future = null;
	}

	/**
	 * Register an action to be performed at a certain tick number
	 * 
	 * @param tickNum the tick in which to perform the action
	 * @param action  the action to perform
	 */
	public void addTickAction(long tickNum, Runnable action) {
		tickActions.put(tickNum, action);
	}

	/**
	 * @return true if set to send MIDI clock pulses
	 */
	public boolean isControlDawTiming() {
		return controlDawTiming;
	}

	/**
	 * @param controlDawTiming set to true to send MIDI clock pulses, 24 each
	 *                         quarter note
	 */
	public void setControlDawTiming(boolean controlDawTiming) {
		this.controlDawTiming = controlDawTiming;
	}
}
