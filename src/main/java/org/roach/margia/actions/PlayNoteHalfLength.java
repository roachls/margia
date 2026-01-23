package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tell the musician to play the given chord, but with half the original length.
 * This will never result in a length less than 1.
 * 
 * @param musician the musician
 * @param chord    the chord to play
 */
public record PlayNoteHalfLength(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord.withLength(Math.min(1, chord.getLength() / 2)));
    }

}
