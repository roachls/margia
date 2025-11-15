package org.roach.midi_swarm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeyTest {

	@ParameterizedTest
	@CsvSource({"C,3,E","C,4,F","G,1,G","G,2,A","G,3,B","G,4,C","G,5,D","G,6,E","G,7,F"})
	void testUpInterval(Note start, int interval, Note expected) {
		assertEquals(expected, Key.CMajor.upInterval(start, interval));
	}

	@ParameterizedTest
	@CsvSource({"C,3,A","C,4,G","G,1,G","G,2,F","G,3,E","G,4,D","G,5,C","G,6,B","G,7,A"})
	void testDownInterval(Note start, int interval, Note expected) {
		assertEquals(expected, Key.CMajor.downInterval(start, interval));
	}

}
