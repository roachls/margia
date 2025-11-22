package org.roach.midi_swarm;

/**
 * @param note     the note to play
 * @param velocity the velocity of the note
 * @param length   the length of the note
 * 
 */
public record NoteInfo(int note, int velocity, int length) {
}
