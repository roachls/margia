package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Tell the musician to play the given note, but with half the original length.
 * This will never result in a length less than 1.
 * 
 * @param musician the musician
 * @param note     the note to play
 */
public record PlayNoteHalfLength(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		var newLength = Math.min(note.length() / 2, 1);
		musician.playNote(note.withLength(newLength));
	}

}
