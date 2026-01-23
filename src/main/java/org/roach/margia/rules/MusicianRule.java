package org.roach.margia.rules;

import java.util.List;

import org.roach.margia.Musician;

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
    void reset();

    /**
     * @return settable parameter types
     */
    @SuppressWarnings("java:S1452")
    public abstract List<SettableParamDescription<?>> getSettableParameters();

    /**
     * @param <T>          type of property
     * @param propertyName storable name of property
     * @param displayName  display name of property
     * @param type         {@link Class} of property
     * @param minValue     minimum value (only applicable if numeric type)
     * @param maxValue     maximum value (only applicable if numeric type)
     * @param step         step for UI spinners (only applicable if numeric type)
     */
    public static record SettableParamDescription<T>(String propertyName, String displayName, Class<T> type,
            Double minValue, Double maxValue, Double step) {
    }

}