package org.roach.margia;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

import org.apache.logging.log4j.*;
import org.roach.margia.timing.TimingSource;

/**
 * This is the "clock" that drives everything. It issues a "tick" once every
 * 16th note.
 */
public class Transport {
	/**
	 * Property fired when tick updates
	 */
	public static final String TICK_PROPERTY = "tick";
	/**
	 * Property fired when clock pulse updates
	 */
	public static final String CLOCK_PULSE_PROPERTY = "clockPulse";
	/**
	 * Property fired when measureNum updates
	 */
	public static final String MEASURE_PROPERTY = "measure";
	/**
	 * Property fired when beatNum updates
	 */
	public static final String BEAT_PROPERTY = "beat";
	private final List<Musician> musicians;
	private long tick = 1;
	private final Logger logger = LogManager.getLogger(getClass());
	private final Map<Long, Runnable> tickActions = new HashMap<>();
	private final MidiController controller;
	private final int tickLength;
	private boolean controlDawTiming;
	private int currentClockPulse = 1;
	private int beatNum = 1;
	private int measureNum = 1;
	private final PropertyChangeSupport propertyChangeSupport;

	/**
	 * @param musicians  the musicians
	 * @param tempo      the tempo
	 * @param controller the MIDI controller
	 */
	public Transport(final List<Musician> musicians, int tempo, final MidiController controller) {
		this.musicians = musicians;
		this.tickLength = Length.getMillisForTempo(1, tempo).getValue().intValue();
		this.controller = controller;
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	/**
	 * @param listener a listener to add
	 */
	public void addPropertyListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
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
		}
	}

	/**
	 * Fires once every 24th of a beat from the {@link TimingSource}
	 */
	public void receiveClockPulse() {
		logger.printf(Level.TRACE, "%03d:%01d.%02d (%03d)", measureNum, beatNum, currentClockPulse, tick);
		if (controlDawTiming) {
			controller.sendClockPulse();
		}
		// Once each 16th note (every 6 clock pulses) we kick off musician actions
		// TODO should we do more than just 4/4?
		if (currentClockPulse == 1 || currentClockPulse == 7 || currentClockPulse == 13 || currentClockPulse == 19) {
			if (tickActions.containsKey(tick)) {
				logger.atDebug().log("Transport playing tick action {}", tick);
				tickActions.remove(tick).run();
			}
			// each musician calculate their next action
			musicians.forEach(m -> m.calculateAction(tick));
			// each musician perform the action they just calculated
			musicians.forEach(m -> m.doAction(tick));
			// controller actually play notes from each musician
			controller.playNotesThisTick();
		}
		if (currentClockPulse % 6 == 0) {
			var oldValue = tick++;
			propertyChangeSupport.firePropertyChange(TICK_PROPERTY, oldValue, tick);
		}
		var oldClockPulse = currentClockPulse++;
		if (currentClockPulse > 24) {
			currentClockPulse = 1;
			var oldBeatNum = beatNum++;
			if (beatNum > 4) {
				beatNum = 1;
				var oldMeasure = measureNum++;
				propertyChangeSupport.firePropertyChange(MEASURE_PROPERTY, oldMeasure, measureNum);
			}
			propertyChangeSupport.firePropertyChange(BEAT_PROPERTY, oldBeatNum, beatNum);
		}
		propertyChangeSupport.firePropertyChange(CLOCK_PULSE_PROPERTY, oldClockPulse, currentClockPulse);
	}

	/**
	 * Stop the clock
	 */
	public void stop() {
		if (controlDawTiming)
			controller.sendStop();
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

	/**
	 * Reset measure, beat, current clock pulse, and tick to 1
	 */
	public void reset() {
		propertyChangeSupport.firePropertyChange(MEASURE_PROPERTY, measureNum, 1);
		propertyChangeSupport.firePropertyChange(BEAT_PROPERTY, beatNum, 1);
		propertyChangeSupport.firePropertyChange(CLOCK_PULSE_PROPERTY, currentClockPulse, 1);
		propertyChangeSupport.firePropertyChange(TICK_PROPERTY, tick, 1L);
		this.measureNum = 1;
		this.beatNum = 1;
		this.currentClockPulse = 1;
		this.tick = 1;
	}
}
