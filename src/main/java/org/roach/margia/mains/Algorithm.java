package org.roach.margia.mains;

import java.util.List;

import org.roach.margia.Musician;

/**
 * A music-generating algorithm
 * 
 * @param <T> type of parameters
 */
public interface Algorithm<T extends MargiaParams> {
    /**
     * Initialize and return list of musicians
     * 
     * @param mainParams {@link MainParams}
     * @param algParams  params specific to this algorithm
     * @return list of musicians
     */
    List<Musician> initMusicians(MainParams mainParams, MargiaParams algParams);

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
