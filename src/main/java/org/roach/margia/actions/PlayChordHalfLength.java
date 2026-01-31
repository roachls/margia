package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;

/**
 * Tell the musician to play the given chord, but with half the original length.
 * This will never result in a length less than 1.
 * 
 * @param musician the musician
 * @param chord    the chord to play
 */
public record PlayChordHalfLength(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord.withLength(Math.max(1, chord.getLength() / 2)));
    }

}
