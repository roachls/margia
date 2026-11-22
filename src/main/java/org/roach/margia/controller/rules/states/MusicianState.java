package org.roach.margia.controller.rules.states;

import java.util.List;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.model.MusicianMessage;

/**
 * A state that a musician can be in
 */
public interface MusicianState {
    /**
     * @return list of actions that a musician in this state takes on a
     *         {@link MusicianMessage}
     */
    List<Function<MusicianMessage, List<MusicalAction>>> actions();

    /**
     * @return name of this state
     */
    String name();

    /**
     * Transition to a new state (which could be this state)
     * 
     * @param musician musician that is transitioning
     * @return a new state
     */
    MusicianState transition(Musician musician);

    /**
     * Add actions to the musician's to-do list
     * 
     * @param musician musician doing the performing
     * @param rule     the rule that owns this state
     * @param message  the message received that is being acted on
     */
    void doActions(Musician musician, AbstractMusicianRule rule, MusicianMessage message);
}
