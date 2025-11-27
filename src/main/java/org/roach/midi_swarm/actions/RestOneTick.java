package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;

/**
 * Have the musician rest one tick
 * 
 * @param musician the musician
 */
public record RestOneTick(Musician musician) implements MusicalAction {

	@Override
	public void perform() {
		musician.rest();
	}

}
