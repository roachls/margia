package org.roach.margia.actions;

import org.roach.margia.Musician;

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
