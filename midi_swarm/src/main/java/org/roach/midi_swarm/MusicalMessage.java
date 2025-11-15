package org.roach.midi_swarm;

import java.util.List;

public record MusicalMessage(String message, List<Note> myNotes) {

}
