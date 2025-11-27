package org.roach.midi_swarm.random;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DieRollerTest {

	@ParameterizedTest
	@MethodSource("args")
	void testGetMin(String diceDescription, int expectedMin, int expectedMax) {
		assertEquals(expectedMin, DieRoller.getMin(diceDescription));
	}

	@ParameterizedTest
	@MethodSource("args")
	void testGetMax(String diceDescription, int expectedMin, int expectedMax) {
		assertEquals(expectedMax, DieRoller.getMax(diceDescription));
	}

	static Stream<Arguments> args() {
		// @formatter:off
		return Stream.of(
				Arguments.of("1d6", 1, 6),
				Arguments.of("2d6", 2, 12),
				Arguments.of("1d100", 1, 100),
				Arguments.of("2d50", 2, 100),
				Arguments.of("4d25", 4, 100),
				Arguments.of("5d20", 5, 100),
				Arguments.of("10d10", 10, 100),
				Arguments.of("20d5", 20, 100),
				Arguments.of("1000d2", 1000, 2000)
		);
		// @formatter:on
	}
}
