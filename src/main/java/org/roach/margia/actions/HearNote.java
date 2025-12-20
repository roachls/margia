package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Tells the {@link Musician} that it "heard" the given note, as if a peer
 * played it
 * 
 * @param musician the {@link Musician}
 * @param note     the note
 */
public record HearNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.receiveMessage(note);
	}

}
