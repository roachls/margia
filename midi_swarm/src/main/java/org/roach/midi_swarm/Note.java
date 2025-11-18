package org.roach.midi_swarm;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("javadoc")
public enum Note {
    B_SHARP(0), C(0), C_SHARP(1), D_FLAT(1), D(2), D_SHARP(3), E_FLAT(3), E(4), F_FLAT(4), E_SHARP(5), F(5), F_SHARP(6),
    G_FLAT(6), G(7), G_SHARP(8), A_FLAT(8), A(9), A_SHARP(10), B_FLAT(10), B(11), C_FLAT(11), REST(-1);

    private static final Map<Integer, Note> NOTE_MAP = new HashMap<>();
    private final int noteNumber;

    Note(int noteNumber) {
	this.noteNumber = noteNumber;
    }

    public int getNoteNumber() {
	return this.noteNumber;
    }

    public int getNoteNumberForOctave(final int octave) {
	return (octave + 1) * 12 + noteNumber;
    }

    public static Note getNote(int midiNote) {
	if (NOTE_MAP.isEmpty()) {
	    for (var note : values()) {
		NOTE_MAP.put(note.getNoteNumber(), note);
	    }
	}
	var noteNum = midiNote % 12;
	return NOTE_MAP.get(noteNum);
    }

}
