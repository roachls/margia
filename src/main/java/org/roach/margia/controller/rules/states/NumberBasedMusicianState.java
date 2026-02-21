package org.roach.margia.controller.rules.states;

import java.util.*;

import org.roach.margia.controller.rules.AbstractMusicianState;
import org.roach.margia.controller.rules.NumericRange;

/**
 * A {@link MusicianState} that's state transitions are based on the value of a
 * number of some sort
 * 
 * @param <T> type of subclass
 */
public abstract class NumberBasedMusicianState<T extends NumberBasedMusicianState<T>> extends AbstractMusicianState<T> {
    protected final Map<NumericRange, MusicianState> stateMap = new TreeMap<>(
            (range1, range2) -> Integer.compare(range1.min(), range2.min()));

    protected NumberBasedMusicianState(String name) {
        super(name);
    }

    /*
     * TODO check for a situation like this: existing ranges (1-3), (7-9), trying to
     * add range (2-6) or (2-8) should thrown an exception, while (4-5) should not
     * 
     */
    protected void checkForOverlap(NumericRange rangeToCheck) throws NumericRangeOverlapException {
        for (var existingRange : stateMap.keySet()) {
            // @formatter:off
            if (existingRange.equals(rangeToCheck) // ranges are equal
                    || existingRange.min() == rangeToCheck.min() // ranges have same min
                    || existingRange.max() == rangeToCheck.max() // ranges have same max
                    // existingRange is contained within rangeToCheck
                    || (existingRange.min() > rangeToCheck.min() && existingRange.max() < rangeToCheck.max())
                    // rangeToCheck is contained within existingRange
                    || (rangeToCheck.min() > existingRange.min() && rangeToCheck.max() < existingRange.max())
                    )
            // @formatter:on
                throw new NumericRangeOverlapException(name, existingRange, rangeToCheck);
        }
    }

    /**
     * Exception thrown if ranges overlap
     */
    public static class NumericRangeOverlapException extends RuntimeException {
        /**
         * @param name          name of {@link MusicianState}
         * @param numericRange1 first range
         * @param numericRange2 second range
         */
        public NumericRangeOverlapException(String name, NumericRange numericRange1, NumericRange numericRange2) {
            super("Problem in state %s: %s overlaps with %s".formatted(name, numericRange1, numericRange2));
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * @return the stateMap
     */
    public Map<NumericRange, MusicianState> getStateMap() { return Collections.unmodifiableMap(stateMap); }
}
