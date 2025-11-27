package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;

/**
 * Tell the {@link Musician} to repeat its last note
 * 
 * @param musician the musician
 */
public record RepeatLastNote(Musician musician) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(musician.getMyLastNote());
	}

}
