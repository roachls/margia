package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;

public record ResetPlayedNotes(Musician musician) implements MusicalAction {
	

	@Override
	public void perform() {
		musician.resetNotesIvePlayed();
	}

}
