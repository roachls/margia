package org.roach.margia.controller.rules.states;

import java.util.Map.Entry;

import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.NumericRange;
import org.roach.margia.util.DieRoller;

/**
 * A {@link MusicianState} that rolls a die and chooses which state to
 * transition to based on the die roll. Note that if a die roll falls outside of
 * the ranges defined through the
 * {@link #withStateTransition(NumericRange, MusicianState)} method, a null
 * transition will be applied by default (i.e., a transition back to the same
 * state).
 */
public class DieRollingMusicianState extends NumberBasedMusicianState<DieRollingMusicianState> {
    private final String diceDescription;

    /**
     * @param name            name of this state
     * @param diceDescription description of dice (see
     *                        {@link DieRoller#rollDice(String)}
     */
    public DieRollingMusicianState(String name, String diceDescription) {
        super(name);
        this.diceDescription = diceDescription;
    }

    /**
     * Add a state transition to this state
     * 
     * @param range   range of numbers (inclusive) for the die roll that will
     *                trigger the transition
     * @param toState the transition state
     * @return this {@link DieRollingMusicianState}
     */
    public DieRollingMusicianState withStateTransition(NumericRange range, MusicianState toState) {
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
        int dieRoll = DieRoller.rollDice(diceDescription);
        var appropriateTransition = stateMap.entrySet().stream()
                .filter(t -> dieRoll >= t.getKey().min() && dieRoll <= t.getKey().max()).findFirst();
        return appropriateTransition.map(Entry::getValue).orElse(this);
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
