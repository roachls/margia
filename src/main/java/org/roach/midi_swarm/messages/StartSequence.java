package org.roach.midi_swarm.messages;

public record StartSequence(int numNotesInSequence, int repeatAtTick) implements MusicianMessage {

}
