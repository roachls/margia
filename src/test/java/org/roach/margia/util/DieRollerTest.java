package org.roach.margia.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.roach.margia.storage.Options;

class DieRollerTest {
    @BeforeAll
    static void setOpts() {
        Options.getInstance().setRandomSeed(1L);
        DieRoller.initFromOptions();
    }

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

    @Test
    void testRollDice() {
        var dieRolls = IntStream.range(0, 15).map(_ -> DieRoller.rollDice("2d10")).toArray();
        assertArrayEquals(new int[] { 15, 12, 10, 12, 18, 14, 12, 8, 6, 17, 10, 11, 15, 13, 11 }, dieRolls,
                Arrays.toString(dieRolls));
    }
    
    @Test
    void testBadDescription() {
        assertThrows(IllegalArgumentException.class, () -> DieRoller.rollDice("foo"));
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
