package org.roach.margia.controller.rules.states;

import org.roach.margia.controller.Musician;

/**
 * A {@link MusicianState} that never transitions
 */
public class NeverTransitionState extends AbstractMusicianState<NeverTransitionState> {

    /**
     * @param name The name of this state
     */
    public NeverTransitionState(String name) {
        super(name);
    }

    @Override
    public MusicianState transition(Musician musician) {
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
