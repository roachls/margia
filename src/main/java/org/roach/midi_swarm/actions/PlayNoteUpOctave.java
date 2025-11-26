package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record PlayNoteUpOctave(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		var newNoteNum = musician.getKey().adjustToKeyByOctaves(note.noteNum() + 12);
		musician.playNote(note.withNote(newNoteNum));
	}

}
