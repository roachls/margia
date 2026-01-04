package org.roach.margia.timing;

/**
 * A source of timing clock pulses, 24 pulses per beat
 */
public interface TimingSource {
    /**
     * Default tempo
     */
    static final int DEFAULT_TEMPO = 60;
    /**
     * the property fired when the tempo changes
     */
    String TEMPO_PROPERTY = "music.tempo";

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
