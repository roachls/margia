package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Tells the musician to play the given note
 * 
 * @param musician the musician
 * @param note     the note to play
 */
public record PlayNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(note);
	}

}
