package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.controller.Musician;
import org.roach.margia.storage.params.SettableParamDescription;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public sealed interface MusicianRule permits AbstractMusicianRule {

    /**
     * property for storing rule classname
     */
    String RULE_NAME_PROPERTY = "name";

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
    default void reset() {
        // nothing to do
    }

    /**
     * Actions to be performed after this rule is assigned a {@link Musician}
     */
    void initActionsAfterMusicianAssigned();

    /**
     * @return settable parameter types
     */
    @SuppressWarnings("java:S1452")
    public abstract List<SettableParamDescription> getSettableParameters();
}