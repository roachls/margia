package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record PlayNoteDownVelocity(Musician musician, NoteInfo note, int diff) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(note.withVelocity(note.velocity() - diff));
	}

}
