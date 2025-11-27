package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;

/**
 * Tell the given musician to reset its count of how many notes it has played
 * 
 * @param musician the musician
 */
public record ResetPlayedNotes(Musician musician) implements MusicalAction {

	@Override
	public void perform() {
		musician.resetNotesIvePlayed();
	}

}
