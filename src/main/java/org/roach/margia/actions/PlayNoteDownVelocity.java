package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tells the {@link Musician} to play the given chord, but with
 * <code>diff</code> less velocity. This will never result in a velocity below
 * 0.
 * 
 * @param musician the musician
 * @param chord    the chord to play
 * @param diff     the difference in velocity
 */
public record PlayNoteDownVelocity(Musician musician, Chord chord, int diff) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord.withVelocity(chord.getVelocity() - diff));
    }

}
