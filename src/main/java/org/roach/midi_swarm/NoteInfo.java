package org.roach.midi_swarm;

import org.roach.midi_swarm.messages.MusicianMessage;

/**
 * @param noteNum  the note to play
 * @param velocity the velocity of the note
 * @param length   the length of the note in ticks
 * 
 */
public record NoteInfo(int noteNum, int velocity, int length) implements MusicianMessage {

	/**
	 * Sanity check params
	 * 
	 * @param noteNum  note number (0-127)
	 * @param velocity velocity (0-127)
	 * @param length   length in ticks (&gt;= 1)
	 */
	public NoteInfo {
		if (noteNum < -1 || noteNum > 127)
			throw new IllegalArgumentException("note must be between -1 and 127 inclusive");
		if (velocity < 0 || velocity > 127)
			throw new IllegalArgumentException("velocity must be between 0 and 127 inclusive");
		if (length < 1)
			throw new IllegalArgumentException("length must at least 1");
	}

	/**
	 * @param newNoteNum new note number
	 * @return a copy of this {@link NoteInfo} with the given note number
	 */
	public NoteInfo withNote(int newNoteNum) {
		return new NoteInfo(newNoteNum, velocity, length);
	}

	/**
	 * @param newVelocity new velocity
	 * @return a copy of this {@link NoteInfo} with the new velocity
	 */
	public NoteInfo withVelocity(int newVelocity) {
		return new NoteInfo(noteNum, newVelocity, length);
	}

	/**
	 * @param newLength new length in ticks
	 * @return a copy of this {@link NoteInfo} with the new length
	 */
	public NoteInfo withLength(int newLength) {
		return new NoteInfo(noteNum, velocity, newLength);
	}
}
