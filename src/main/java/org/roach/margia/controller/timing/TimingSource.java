package org.roach.margia.controller.timing;

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
     * re-read Options to get latest tempo
     */
    void updateTempo();

    /**
     * @return the current tempo in beats-per-minute
     */
    int getTempo();

    /**
     * @return true if the clock is running
     */
    boolean isRunning();
}
