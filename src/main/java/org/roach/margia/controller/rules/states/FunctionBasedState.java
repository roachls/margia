package org.roach.margia.controller.rules.states;

import java.util.Objects;
import java.util.function.BiFunction;

import org.roach.margia.controller.Musician;

/**
 * A {@link MusicianState} that transitions based on a function
 */
public class FunctionBasedState extends AbstractMusicianState<FunctionBasedState> {
    private final BiFunction<MusicianState, Musician, MusicianState> function;

    /**
     * @param name     The name of this state
     * @param function the function that controls the transition
     */
    public FunctionBasedState(String name, BiFunction<MusicianState, Musician, MusicianState> function) {
        super(name);
        this.function = Objects.requireNonNull(function);
    }

    @Override
    public MusicianState transition(Musician musician) {
        return function.apply(this, musician);
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
