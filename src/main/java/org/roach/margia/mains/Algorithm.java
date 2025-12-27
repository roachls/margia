package org.roach.margia.mains;

import java.util.List;

import org.roach.margia.MidiController;
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
     * @param controller {@link MidiController} to use
     * @return list of musicians
     */
    List<Musician> initMusicians(MainParams mainParams, MargiaParams algParams, MidiController controller);

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
