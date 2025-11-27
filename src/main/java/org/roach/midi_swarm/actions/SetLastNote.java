package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Tell the given musician to pretend that the given note is the last note that
 * it played
 * 
 * @param musician musician
 * @param note     the note
 * 
 */
public record SetLastNote(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.setMyLastNote(note);
	}

}
