package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record PlayNoteDownInterval(Musician musician, NoteInfo note, int interval) implements MusicalAction {

	@Override
	public void perform() {
		var newNoteNum = musician.getKey().down(note.noteNum(), interval);
		musician.playNote(note.withNote(newNoteNum));
	}

}
