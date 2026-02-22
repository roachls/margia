package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tell the musician to play the given chord, but with half the original length.
 * This will never result in a length less than 1.
 * 
 * @param musician the musician
 * @param message  the message being transformed, ignored if not a {@link Chord}
 */
public record PlayChordHalfLength(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.playChord(chord.withLength(Math.max(1, chord.getLength() / 2)));
    }

}
