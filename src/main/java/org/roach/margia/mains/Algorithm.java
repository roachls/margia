package org.roach.margia.mains;

/**
 * A music-generating algorithm
 * 
 * @param <T> type of parameters
 */
public interface Algorithm<T extends MargiaParams> {

    /**
     * @return command-line name of this algorithm
     */
    String command();

    /**
     * @return a new instance of T
     */
    T createParams();
    
    /**
     * @return display name of the algorithm
     */
    String displayName();
}
