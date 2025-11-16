package org.roach.midi_swarm;

/**
 * @param note     the note to play
 * @param octave   the octave of the note
 * @param velocity the velocity of the note
 * @param length   the length of the note
 * 
 */
public record NoteInfo(Note note, int octave, int velocity, Length length) {

}
