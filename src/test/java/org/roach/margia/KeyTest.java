package org.roach.margia;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KeyTest {

    @ParameterizedTest
    @CsvSource({ "0,3,4", "0,4,5", "7,1,7", "7,2,9", "7,3,11", "7,4,12", "7,5,14", "7,6,16", "7,7,17" })
    void testUpCMajor(int start, int interval, int expected) {
        assertEquals(expected, Key.CMajor.up(start, interval));
    }

    @ParameterizedTest
    @CsvSource({ "0,3,2", "0,4,3", "7,1,7", "7,2,8", "7,3,9", "7,4,10", "7,5,11", "7,6,12", "7,7,13" })
    void testUpChromatic(int start, int interval, int expected) {
        assertEquals(expected, Key.Chromatic.up(start, interval));
    }

    @ParameterizedTest
    @CsvSource({ "7,3,4", "12,4,7", "12,1,12", "7,2,5", "14,3,11", "0,2,127", "22,2,21" })
    void testDown(int start, int interval, int expected) {
        assertEquals(expected, Key.CMajor.down(start, interval));
    }

    @Test
    void testGenerateKey() {
        assertEquals(List.of(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35, 36, 38, 40,
                41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84,
                86, 88, 89, 91, 93, 95, 96, 98, 100, 101, 103, 105, 107, 108, 110, 112, 113, 115, 117, 119, 120, 122,
                124, 125, 127), Key.generateKey("Test", Key.MAJOR_INTERVAL_KEY, 0).notes());
        assertEquals(List.of(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35, 36, 38, 40,
                41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84,
                86, 88, 89, 91, 93, 95, 96, 98, 100, 101, 103, 105, 107, 108, 110, 112, 113, 115, 117, 119, 120, 122,
                124, 125, 127), Key.generateKey("Test", Key.MAJOR_INTERVAL_KEY, 0).notes());
        assertEquals(0, Octave.O_NEG2.getLow());
        assertEquals(11, Octave.O_NEG2.getHigh());
    }

}
