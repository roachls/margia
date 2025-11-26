package org.roach.midi_swarm;

import java.security.SecureRandom;
import java.util.*;

@SuppressWarnings("javadoc")
public interface Key {
	Random RANDOM = new SecureRandom();

	static final List<Integer> MAJOR_INTERVALS = List.of(2, 2, 1, 2, 2, 2, 1);
	static final List<Integer> PENTATONIC_INTERVALS = List.of(2, 2, 3, 2, 3);
	static final List<Integer> CHROMATIC_INTERVALS = List.of(1);

	/**
	 * Generate a key using the specified intervals starting at the given start note
	 * and ending with the note within the key that is less then or equal to the
	 * range hi
	 * 
	 * @param intervals    intervals making up the key
	 * @param startingNote start note
	 * @param rangeHi      end range
	 * @return a key
	 */
	static Key generateKey(List<Integer> intervals, int startingNote, int rangeHi) {
		var n = startingNote;
		var intervalNum = 0;
		var list = new ArrayList<Integer>();
		while (n <= rangeHi) {
			list.add(n);
			n += intervals.get(intervalNum);
			intervalNum++;
			intervalNum %= intervals.size();
		}
		return () -> list;
	}

	/**
	 * @param intervals intervals making up the key
	 * @param octaves   a list of octaves; need not be in order, and need not
	 *                  include all intervening octaves. For example, including O2
	 *                  and O6 will also include all notes from O3, O4, and O5.
	 * @return a key
	 */
	static Key generateKey(List<Integer> intervals, List<Octave> octaves) {
		var low = octaves.stream().map(Octave::getLow).min(Integer::compare).get();
		var high = octaves.stream().map(Octave::getHigh).max(Integer::compare).get();
		return generateKey(intervals, low, high);
	}

	Key CMajor = generateKey(MAJOR_INTERVALS, 0, 127);

	Key CPentatonic = generateKey(PENTATONIC_INTERVALS, 0, 127);

	Key Chromatic = generateKey(CHROMATIC_INTERVALS, 0, 127);

	List<Integer> notes();

	default int randomNote() {
		var noteNum = RANDOM.nextInt(notes().size());
		return notes().get(noteNum);
	}

	/**
	 * Find the note the given number of indices up within the key; if it goes above
	 * the top index, wrap around to the bottom
	 * 
	 * @param start    starting note
	 * @param interval number of indices away
	 * @return the note the given interval up
	 */
	default int up(int start, int interval) {
		int nStart = adjustToKeyByOctaves(start);
		var list = this.notes();
		int startIndex = list.indexOf(nStart);
		while (startIndex == -1) {
			nStart--;
			startIndex = list.indexOf(nStart);
		}
		int endIndex = (startIndex + interval - 1) % list.size();
		return list.get(endIndex);
	}

	default int adjustToKeyByOctaves(int start) {
		int nStart = start;
		if (nStart > highestNote()) {
			while (nStart > highestNote()) {
				nStart -= 12;
			}
		} else if (nStart < lowestNote()) {
			while (nStart < lowestNote()) {
				nStart += 12;
			}
		}
		return nStart;
	}

	/**
	 * Find the note the given number of indices away within the key; if it goes
	 * below 0, wrap around to the top
	 * 
	 * @param start    starting note
	 * @param interval number of indices away
	 * @return the note the given interval down
	 */
	default int down(int start, int interval) {
		int nStart = adjustToKeyByOctaves(start);
		var list = this.notes();
		int startIndex = list.indexOf(nStart);
		while (startIndex == -1) {
			nStart++;
			startIndex = list.indexOf(nStart);
		}
		int endIndex = (startIndex - (interval - 1));
		if (endIndex < 0)
			endIndex = list.size() - 1;
		endIndex %= list.size();
		return list.get(endIndex);
	}

	default int lowestNote() {
		return notes().get(0);
	}

	default int highestNote() {
		return notes().get(notes().size() - 1);
	}
}
