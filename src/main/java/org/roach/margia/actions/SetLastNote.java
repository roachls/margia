package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Tell the given musician to pretend that the given note is the last note that
 * it played
 * 
 * @param musician musician
 * @param note     the note
 * 
 */
public record SetLastNote(Musician musician, NoteInfo note) implements MusicalAction {

    @Override
    public void perform() {
        musician.setMyLastNote(note);
    }

}
