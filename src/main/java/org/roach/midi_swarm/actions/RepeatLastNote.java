package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;

public record RepeatLastNote(Musician musician) implements MusicalAction {
	

	@Override
	public void perform() {
		musician.playNote(musician.getMyLastNote());
	}

}
