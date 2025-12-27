package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Tells the {@link Musician} to play the given note, but with <code>diff</code>
 * more velocity. This will never result in a velocity below above
 * {@link Musician#MAX_VELOCITY}.
 * 
 * @param musician the musician
 * @param note     the note to play
 * @param diff     the difference in velocity
 */
public record PlayNoteUpVelocity(Musician musician, NoteInfo note, int diff) implements MusicalAction {

    @Override
    public void perform() {
        musician.playNote(note.withVelocity(note.velocity() + diff));
    }

}
