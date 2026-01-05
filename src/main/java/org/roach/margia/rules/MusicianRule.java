package org.roach.margia.rules;

import org.roach.margia.Musician;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public sealed interface MusicianRule permits AbstractMusicianRule {

    /**
     * Calculate action to be taken when doAction is called. This should set
     * nextNote.
     * 
     * @param tick the tick number
     */
    void calculateAction(long tick);
    
    /**
     * @return the name of this rule
     */
    String getName();

}