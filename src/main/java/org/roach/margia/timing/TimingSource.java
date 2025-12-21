package org.roach.margia.timing;

import org.roach.margia.ui.PropertyChangeEmitter;

/**
 * A source of timing clock pulses, 24 pulses per beat
 */
public interface TimingSource extends PropertyChangeEmitter {
	/**
	 * Start the clock
	 */
	void start();

	/**
	 * Stop the clock
	 */
	void stop();
	
	/**
	 * @param tempo the tempo in beats-per-minute
	 */
	void setTempo(int tempo);
	
	/**
	 * @return the current tempo in beats-per-minute
	 */
	int getTempo();
	
	/**
	 * @return true if the clock is running
	 */
	boolean isRunning();
}
