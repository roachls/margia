package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tells the {@link Musician} to play the given note, but with <code>diff</code>
 * more velocity. This will never result in a velocity below above
 * {@link Musician#MAX_VELOCITY}.
 * 
 * @param musician the musician
 * @param chord    the chord to play
 * @param diff     the difference in velocity
 */
public record PlayNoteUpVelocity(Musician musician, Chord chord, int diff) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(chord.withVelocity(chord.getVelocity() + diff));
    }

}
