package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record PlayNoteHalfLength(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		var newLength = Math.min(note.length() / 2, 1);
		musician.playNote(note.withLength(newLength));
	}

}
