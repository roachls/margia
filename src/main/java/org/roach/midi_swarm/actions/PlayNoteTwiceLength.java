package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Tell the musician to play the given note, but with twice the original length.
 * There is no upper limit to the length that can be played.
 * 
 * @param musician the musician
 * @param note     the note to play
 */
public record PlayNoteTwiceLength(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(note.withLength(note.length() * 2));
	}

}
