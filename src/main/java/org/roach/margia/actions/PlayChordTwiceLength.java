package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tell the musician to play the given chord, but with twice the original
 * length, up to 16 ticks (1 whole note).
 * 
 * @param musician the musician
 * @param chord    the chord to play
 */
public record PlayChordTwiceLength(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord.withLength(Math.min(chord.getLength() * 2, 16)));
    }

}
