package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

public record RestOneTick(Musician musician) implements MusicalAction {

	@Override
	public void perform() {
		musician.rest();
	}

}
