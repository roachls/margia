package org.roach.midi_swarm;

import java.util.List;

/**
 * A message sent from one {@link Musician} to another
 * @param message the message that was sent
 * @param myNotes the notes that the sending agent just played
 */
public record MusicalMessage(List<Note> myNotes) {

}
