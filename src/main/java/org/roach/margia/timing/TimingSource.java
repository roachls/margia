package org.roach.margia.timing;

/**
 * A source of timing clock pulses, 24 pulses per beat
 */
public interface TimingSource {
	/**
	 * Start the clock
	 */
	void start();

	/**
	 * Stop the clock
	 */
	void stop();
	
	/**
	 * @return true if the clock is running
	 */
	boolean isRunning();
}
