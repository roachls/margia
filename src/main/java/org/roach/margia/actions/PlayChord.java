package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;

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
