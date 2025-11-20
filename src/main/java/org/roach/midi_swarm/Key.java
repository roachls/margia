package org.roach.midi_swarm;

import java.security.SecureRandom;
import java.util.*;

@SuppressWarnings("javadoc")
public interface Key {
	Random RANDOM = new SecureRandom();
	
	static final List<Integer> MAJOR_INTERVALS = List.of(2, 2, 1, 2, 2, 2, 1);
	static final List<Integer> CHROMATIC_INTERVALS = List.of(1);
	
	static Key generateKey(List<Integer> intervals, int startingNote) {
		var n = startingNote;
		var intervalNum = 0;
		var list = new ArrayList<Integer>();
		while (n < 128) {
			list.add(n);
			n += intervals.get(intervalNum);
			intervalNum++;
			intervalNum %= intervals.size();
		}
		return () -> list;
	}

	Key CMajor = generateKey(MAJOR_INTERVALS, 0);
	
	Key Chromatic = generateKey(CHROMATIC_INTERVALS, 0);

	List<Integer> notes();

	default int randomNote() {
		var noteNum = RANDOM.nextInt(notes().size());
		return notes().get(noteNum);
	}

	default int upInterval(int start, int interval) {
		var list = this.notes();
		int startIndex = list.indexOf(start);
		if (startIndex == -1)
			throw new IllegalArgumentException(start + " is not in this key");
		int endIndex = (startIndex + interval - 1) % list.size();
		return list.get(endIndex);
	}

	default int downInterval(int start, int interval) {
		var list = this.notes();
		int startIndex = list.indexOf(start);
		if (startIndex == -1)
			throw new IllegalArgumentException(start + " is not in this key");
		int endIndex = (startIndex - (interval - 1));
		if (endIndex < 0)
			endIndex = 0;
		endIndex %= list.size();
		return list.get(endIndex);
	}

}
