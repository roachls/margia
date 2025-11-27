package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Command to play the given note up the given interval
 * 
 * @param musician {@link Musician} that will play the note
 * @param note     note to transform
 * @param interval interval by which to go up, relative to the scale. An
 *                 interval of 1 means no change. An interval of 2 is a "second"
 *                 away as determined by the key.
 */
public record PlayNoteUpInterval(Musician musician, NoteInfo note, int interval) implements MusicalAction {

	@Override
	public void perform() {
		var newNoteNum = musician.getKey().up(note.noteNum(), interval);
		musician.playNote(note.withNote(newNoteNum));
	}

}
