package org.roach.margia.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NoteRangeTest {
    final NoteRange start = new NoteRange(Octave.O3);

    @Test
    void testAdjustToRangeByOctaves() {
        var range = new NoteRange(Note.C2, Note.G4);
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C2));
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C1));
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C_N1));
        assertEquals(Note.A3, range.adjustToRangeByOctaves(Note.A3));
        assertEquals(Note.A3, range.adjustToRangeByOctaves(Note.A4));
    }

    @Test
    void testNoteRangeIntInt() {
        NoteRange nr = new NoteRange(60, 70);
        assertEquals(60, nr.low());
        assertEquals(70, nr.high());
        assertEquals(10, nr.span());
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(70, 60));
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(-100, 60));
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(0, 160));
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(160, 160));
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(100, 160));
        assertThrows(IllegalArgumentException.class, () -> new NoteRange(100, -160));
    }

    @Test
    void testNoteRangeOctave() {
        NoteRange nr = new NoteRange(Octave.O1);
        assertEquals(36, nr.low());
        assertEquals(47, nr.high());
        assertEquals(11, nr.span());
    }

    @Test
    void testNoteRangeOctaveOctave() {
        NoteRange nr = new NoteRange(Octave.O1, Octave.O3);
        assertEquals(36, nr.low());
        assertEquals(47 + 24, nr.high());
        assertEquals(11 + 24, nr.span());
    }

    @Test
    @SuppressWarnings("java:S5778")
    void testWithLow() {
        NoteRange adjusted = start.withLow(20);
        assertEquals(20, adjusted.low());
        assertEquals(start.high(), adjusted.high());
        assertEquals(start.span() + (start.low() - 20), adjusted.span());
        assertThrows(IllegalArgumentException.class, () -> start.withLow(start.high() + 1));
    }

    @Test
    void testSpan() {
        assertEquals(11, start.span());
    }

    @Test
    void testWithHigh() {
        NoteRange adjusted = start.withHigh(100);
        assertEquals(start.low(), adjusted.low());
        assertEquals(100, adjusted.high());
        assertEquals(start.span() + (100 - start.high()), adjusted.span());
    }

    @Test
    void testCopy() {
        assertEquals(start, start.copy());
        assertNotSame(start, start.copy());
    }

}
