package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tells the musician to play the given note
 * 
 * @param musician the musician
 * @param chord    the chord to play
 */
public record PlayChord(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord);
    }

}
