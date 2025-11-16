package org.roach.midi_swarm;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public interface MusicianRule {
	/**
	 * Act on hearing a note
	 * 
	 * @param note     Note to act on
	 */
	void act(NoteInfo note);
}
