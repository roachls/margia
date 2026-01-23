package org.roach.margia.actions;

import org.roach.margia.*;

/**
 * Command to play the given note down the given interval
 * 
 * @param musician {@link Musician} that will play the note
 * @param chord    chord to transform
 * @param interval interval by which to go down, relative to the scale. An
 *                 interval of 1 means no change. An interval of 2 is a "second"
 *                 away as determined by the key.
 */
public record PlayChordDownInterval(Musician musician, Chord chord, int interval) implements MusicalAction {

    @Override
    public void perform() {
        var noteList = chord.getNotes().stream().map(n -> musician.getKey().down(n, interval)).toList();
        musician.playChord(chord.withNotes(noteList));
    }

}
