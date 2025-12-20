package org.roach.margia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeyTest {

	@ParameterizedTest
	@CsvSource({ "0,3,4", "0,4,5", "7,1,7", "7,2,9", "7,3,11", "7,4,12", "7,5,14", "7,6,16", "7,7,17" })
	void testUp(int start, int interval, int expected) {
		assertEquals(expected, Key.CMajor.up(start, interval));
	}

	@ParameterizedTest
	@CsvSource({ "7,3,4", "12,4,7", "12,1,12", "7,2,5", "14,3,11", "0,2,127", "22,2,21" })
	void testDown(int start, int interval, int expected) {
		assertEquals(expected, Key.CMajor.down(start, interval));
	}

	@Test
	void testGenerateKey() {
		assertEquals(List.of(0, 2, 4, 5, 7, 9, 11, 12), Key.generateKey(Key.MAJOR_INTERVALS, 0, 12).notes());
		assertEquals(List.of(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24),
				Key.generateKey(Key.MAJOR_INTERVALS, 0, 24).notes());
		assertEquals(0, Octave.O_NEG2.getLow());
		assertEquals(11, Octave.O_NEG2.getHigh());
		assertEquals(List.of(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23),
				Key.generateKey(Key.MAJOR_INTERVALS, List.of(Octave.O_NEG2, Octave.O_NEG1)).notes());
		assertEquals(List.of(0, 2, 4, 7, 9, 12, 14, 16, 19, 21),
				Key.generateKey(Key.PENTATONIC_INTERVALS, List.of(Octave.O_NEG2, Octave.O_NEG1)).notes());
	}

	@Test
	void testOf() {
		assertEquals(List.of(21, 23, 24, 26, 28, 29, 31, 33, 35, 36), Key.CMajor.of(21, 36).notes());
		assertEquals(List.of(36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51),
				Key.Chromatic.of(36, 51).notes());
	}
}
