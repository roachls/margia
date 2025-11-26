package org.roach.midi_swarm;

import org.roach.midi_swarm.messages.MusicianMessage;

/**
 * @param note     the note to play
 * @param velocity the velocity of the note
 * @param length   the length of the note in ticks
 * 
 */
public record NoteInfo(int note, int velocity, int length) implements MusicianMessage {

	public NoteInfo {
		if (note < -1 || note > 127)
			throw new IllegalArgumentException("note must be between -1 and 127 inclusive");
		if (velocity < 0 || velocity > 127)
			throw new IllegalArgumentException("velocity must be between 0 and 127 inclusive");
		if (length < 1)
			throw new IllegalArgumentException("length must at least 1");
	}

	public NoteInfo withNote(int newNote) {
		return new NoteInfo(newNote, velocity, length);
	}

	public NoteInfo withVelocity(int newVelocity) {
		return new NoteInfo(note, newVelocity, length);
	}

	public NoteInfo withLength(int newLength) {
		return new NoteInfo(note, velocity, newLength);
	}
}
