package org.roach.midi_swarm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeyTest {

	@ParameterizedTest
	@CsvSource({"0,3,4","0,4,5","7,1,7","7,2,9","7,3,11","7,4,12","7,5,14","7,6,16","7,7,17"})
	void testUpInterval(int start, int interval, int expected) {
		assertEquals(expected, Key.CMajor.upInterval(start, interval));
	}

	@ParameterizedTest
	@CsvSource({"7,3,4","12,4,7","12,1,12","7,2,5","14,3,11"})
	void testDownInterval(int start, int interval, int expected) {
		assertEquals(expected, Key.CMajor.downInterval(start, interval));
	}

}
