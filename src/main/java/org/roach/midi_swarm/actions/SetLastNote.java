package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record SetLastNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.setMyLastNote(note);
	}

}
