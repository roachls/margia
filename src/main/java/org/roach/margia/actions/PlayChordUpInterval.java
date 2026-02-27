package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;

/**
 * Command to play the given chord up the given interval
 * 
 * @param musician {@link Musician} that will play the chord
 * @param chord    chord to transform
 * @param interval interval by which to go up, relative to the scale. An
 *                 interval of 1 means no change. An interval of 2 is a "second"
 *                 away as determined by the key.
 */
public record PlayChordUpInterval(Musician musician, Chord chord, int interval) implements MusicalAction {

    @Override
    public void perform() {
        if (Musician.REST.equals(chord))
            return;
        var newNotes = chord.getNotes().stream().map(note -> musician.getKey().up(note, interval)).toList();
        musician.playChord(chord.withNotes(newNotes));
    }

}
