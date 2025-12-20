package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Tells the musician to play the given note
 * 
 * @param musician the musician
 * @param note     the note to play
 */
public record PlayNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(note);
	}

}
