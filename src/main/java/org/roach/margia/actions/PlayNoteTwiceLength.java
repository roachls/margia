package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Tell the musician to play the given note, but with twice the original length.
 * There is no upper limit to the length that can be played.
 * 
 * @param musician the musician
 * @param note     the note to play
 */
public record PlayNoteTwiceLength(Musician musician, NoteInfo note) implements MusicalAction {

    @Override
    public void perform() {
        var newLength = Math.max(note.length() * 2, 16);
        musician.playNote(note.withLength(newLength));
    }

}
