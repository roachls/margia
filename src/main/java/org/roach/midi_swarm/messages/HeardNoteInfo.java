package org.roach.midi_swarm.messages;

import org.roach.midi_swarm.NoteInfo;

public record HeardNoteInfo(long tick, NoteInfo noteInfo) implements MusicianMessage {

}
