package org.roach.margia.controller.rules.states;

import org.roach.margia.controller.Musician;

/**
 * A {@link MusicianState} that operates on a countdown. The {@code value}
 * starts at {@code startValue}, then counts down each tick. When the
 * {@code value} gets to 0, the transition to {@code toState} is triggered, and
 * the {@code value} resets to {@code startValue}
 */
public class CountdownState extends AbstractMusicianState<CountdownState> {
    private final int startValue;
    private int value;
    private final MusicianState toState;

    /**
     * @param name       name of state
     * @param startValue value to start at
     * @param toState    state to transition to once the value reaches 0
     */
    public CountdownState(final String name, final int startValue, final MusicianState toState) {
        super(name);
        this.startValue = value = startValue;
        this.toState = toState;
    }

    @Override
    public MusicianState transition(Musician musician) {
        value--;
        if (value <= 0) {
            value = startValue;
            return toState;
        }
        return this;
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
