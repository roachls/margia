package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Command to play the given note down the given interval
 * 
 * @param musician {@link Musician} that will play the note
 * @param note     note to transform
 * @param interval interval by which to go down, relative to the scale. An
 *                 interval of 1 means no change. An interval of 2 is a "second"
 *                 away as determined by the key.
 */
public record PlayNoteDownInterval(Musician musician, NoteInfo note, int interval) implements MusicalAction {

    @Override
    public void perform() {
        var newNoteNum = musician.getKey().down(note.noteNum(), interval);
        musician.playNote(note.withNote(newNoteNum));
    }

}
