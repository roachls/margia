package org.roach.margia.controller.rules.states;

import java.util.*;
import java.util.Map.Entry;

import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.NumericRange;

/**
 * A state that decides which state to transition to based on a pseudo-random
 * number (PRN). This number is calculated based on the current tick number, the
 * musician ID, and the musician's last chord played. Note that if the PRN falls
 * outside of the ranges defined through the
 * {@link #withStateTransition(NumericRange, MusicianState)} method, a null
 * transition will be applied by default (i.e., a transition back to the same
 * state).
 */
public class PseudoRandomMusicianState extends NumberRangeBasedMusicianState<PseudoRandomMusicianState> {
    private final int maxValue;

    /**
     * @param name     Name of this state
     * @param maxValue max value of pseudo-random number; calculated number will be
     *                 modded with this value
     */
    public PseudoRandomMusicianState(String name, int maxValue) {
        super(name);
        this.maxValue = maxValue;
    }

    /**
     * Adds a state transition
     * 
     * @param range   The range of the pseudo-random number (inclusive) that will
     *                trigger this transition
     * @param toState the state to transition to
     * @return this state
     */
    public PseudoRandomMusicianState withStateTransition(NumericRange range, MusicianState toState) {
        Objects.requireNonNull(range);
        if (range.min() > maxValue || range.max() > maxValue)
            throw new IllegalArgumentException("Range %s goes above max value of %d".formatted(range, maxValue));
        Objects.requireNonNull(toState);
        try {
            checkForOverlap(range);
            this.stateMap.put(range, toState);
        } catch (NumericRangeOverlapException e) {
            logger.atWarn().withThrowable(e).log("Error adding state transition");
        }
        return this;
    }

    @Override
    public MusicianState transition(Musician musician) {
        final var prn = calculatePseudoRandomNumber(musician);
        var appropriateTransition = stateMap.entrySet().stream().filter(t -> t.getKey().isInRange(prn)).findFirst();
        return appropriateTransition.map(Entry::getValue).orElse(this);
    }

    private int calculatePseudoRandomNumber(Musician musician) {
        var prn = musician.getCurrentTick() + musician.getId();
        var lastChord = musician.getMyLastChord();
        if (lastChord != null) {
            for (var lastNote : lastChord.getNotes()) {
                prn += lastNote;
            }
        }
        prn %= maxValue;
        return (int) prn;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
