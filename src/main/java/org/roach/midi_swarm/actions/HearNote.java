package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record HearNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.receiveMessage(note);
	}

}
