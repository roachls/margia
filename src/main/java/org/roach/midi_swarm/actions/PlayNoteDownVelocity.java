package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * Tells the {@link Musician} to play the given note, but with <code>diff</code>
 * less velocity. This will never result in a velocity below 0.
 * 
 * @param musician the musician
 * @param note     the note to play
 * @param diff     the difference in velocity
 */
public record PlayNoteDownVelocity(Musician musician, NoteInfo note, int diff) implements MusicalAction {

	@Override
	public void perform() {
		musician.playNote(note.withVelocity(note.velocity() - diff));
	}

}
