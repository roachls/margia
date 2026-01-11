package org.roach.margia.rules;

import java.util.Map;

import org.roach.margia.Musician;
import org.roach.margia.timing.Storable;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public sealed interface MusicianRule extends Storable permits AbstractMusicianRule {

    /**
     * property for storing rule classname
     */
    String RULE_CLASSNAME_PROPERTY = "classname";

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

    /**
     * Reset this rule back to its defaults (for rewinds)
     */
    void reset();
    
}