package org.roach.margia.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ChordTest {
    final Chord chord = new Chord(Set.of(50, 53, 57), 2, 60);

    @Test
    void testGetNotes() {
        assertEquals(Set.of(53, 57, 50), chord.getNotes());
    }

    @Test
    void testGetLength() {
        assertEquals(2, chord.getLength());
    }

    @Test
    void testGetVelocity() {
        assertEquals(60, chord.getVelocity());
    }

    @Test
    void testWithLength() {
        Chord c = chord.withLength(4);
        assertEquals(4, c.getLength());
        assertEquals(2, chord.getLength(), "original chord length shouldn't change");
        assertEquals(chord.getNotes(), c.getNotes());
        assertEquals(chord.getVelocity(), c.getVelocity());
    }

    @Test
    void testWithNotes() {
        Chord c = chord.withNotes(List.of(4, 5, 6));
        assertEquals(Set.of(5, 4, 6), c.getNotes());
        assertEquals(chord.getLength(), c.getLength());
        assertEquals(chord.getVelocity(), c.getVelocity());
    }

    @Test
    void testWithVelocity() {
        Chord c = chord.withVelocity(70);
        assertEquals(chord.getNotes(), c.getNotes());
        assertEquals(chord.getLength(), c.getLength());
        assertEquals(70, c.getVelocity());
    }

    @Test
    void testAverageNote() {
        assertEquals(53.333332f, chord.averageNote());
    }

    @Test
    void testNormalizedNotes() {
        assertEquals(Set.of(2, 5, 9), chord.normalizedNotes());
    }

    @Test
    void testConstructor() {
        assertEquals(1, new Chord(chord.notes(), -5, 1).length());
        assertThrows(NullPointerException.class, () -> new Chord(null, 5, 1));
    }
}
