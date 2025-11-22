package org.roach.midi_swarm;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a sequence of notes
 */
public class NoteSequence {
	private final List<NoteInfo> notes = new ArrayList<>();

	/**
	 * @param notes the notes in the sequence
	 */
	public NoteSequence(final List<NoteInfo> notes) {
		this.notes.addAll(notes);
	}

	/**
	 * @return number of ticks this sequence lasts
	 */
	public int getLengthInTicks() {
		return notes.stream().map(NoteInfo::length).collect(Collectors.summingInt(i -> i));
	}

	/**
	 * @return the notes in this sequence
	 */
	public List<NoteInfo> notes() {
		return Collections.unmodifiableList(notes);
	}

	@Override
	public String toString() {
		return "NoteSequence [notes=" + notes + "]";
	}

	/**
	 * @param interval interval to go up
	 * @return this entire note sequence up the given interval (disregarding key)
	 */
	public NoteSequence upInterval(int interval) {
		return new NoteSequence(
				notes.stream().map(n -> new NoteInfo(n.note() + interval, n.velocity(), n.length())).toList());
	}
}
