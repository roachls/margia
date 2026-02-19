package org.roach.margia.controller.rules;

import java.util.List;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.model.Chord;

/**
 * A state that a musician can be in
 */
public interface MusicianState {
    /**
     * @return list of actions that a musician in this state takes on a chord
     */
    List<Function<Chord, MusicalAction>> actions();

    /**
     * @return name of this state
     */
    String name();
}
