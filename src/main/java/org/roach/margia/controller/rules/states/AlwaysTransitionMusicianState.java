package org.roach.margia.controller.rules.states;

import org.roach.margia.controller.Musician;

/**
 * A {@link MusicianState} that always transitions to the same other state
 */
public class AlwaysTransitionMusicianState extends AbstractMusicianState<AlwaysTransitionMusicianState> {
    private MusicianState toState;

    /**
     * @param name The name of this state
     */
    public AlwaysTransitionMusicianState(String name) {
        super(name);
    }

    /**
     * @param toState the state to always transition to
     */
    public void setToState(MusicianState toState) { this.toState = toState; }

    @Override
    public MusicianState transition(Musician musician) {
        if (toState == null) {
            musician.getLogger().atError().log("{}: Transition state is null", name);
        }
        return toState;
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
