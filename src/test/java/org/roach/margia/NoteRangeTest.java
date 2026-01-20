package org.roach.margia;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NoteRangeTest {

    @Test
    void testAdjustToRangeByOctaves() {
        var range = new NoteRange(Note.C2, Note.G4);
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C2));
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C1));
        assertEquals(Note.C2, range.adjustToRangeByOctaves(Note.C_N1));
        assertEquals(Note.A3, range.adjustToRangeByOctaves(Note.A3));
        assertEquals(Note.A3, range.adjustToRangeByOctaves(Note.A4));
    }

}
