package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tell the musician to play the given chord, but with twice the original
 * length, up to 16 ticks (1 whole note).
 * 
 * @param musician the musician
 * @param message  the received message, ignored if not a Chord
 */
public record PlayChordTwiceLength(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.playChord(chord.withLength(Math.min(chord.getLength() * 2, 16)));
    }

}
