package org.roach.margia.controller.rules.states;

import java.util.*;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;

/**
 * A state that transitions to another state when its musician hears certain
 * notes
 */
public class NoteBasedTransitionState extends AbstractMusicianState<NoteBasedTransitionState> {
    private final Map<Set<Integer>, MusicianState> noteToStateMap = new HashMap<>();

    /**
     * @param name name of state
     */
    public NoteBasedTransitionState(String name) {
        super(name);
    }

    /**
     * Adds a state transition
     * 
     * @param chordToTransition The notes that will cause this state to transition
     * @param toState           The state to transition to
     * 
     * @return this state
     */
    public NoteBasedTransitionState withStateTransition(Set<Integer> chordToTransition, MusicianState toState) {
        Objects.requireNonNull(chordToTransition);
        Objects.requireNonNull(toState);
        noteToStateMap.put(chordToTransition, toState);
        return this;
    }

    @Override
    public MusicianState transition(Musician musician) {
        final var lastMessage = musician.getNextMessageReceived(false);
        if (lastMessage instanceof Chord chord) {
            var normalizedNotes = chord.normalizedNotes();
            logger.atDebug().setMessage("{}").addArgument(normalizedNotes).log();
            if (noteToStateMap.containsKey(normalizedNotes))
                logger.atDebug().setMessage("Valid transition notes found").log();
            return noteToStateMap.getOrDefault(normalizedNotes, this);
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
