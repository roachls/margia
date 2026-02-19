package org.roach.margia.controller.rules;

import java.util.List;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.model.Chord;

/**
 * Implementation of {@link MusicianState}
 * 
 * @param name    name of state
 * @param actions list of actions
 */
public record MusicianStateImpl(String name, List<Function<Chord, MusicalAction>> actions) implements MusicianState {

}
