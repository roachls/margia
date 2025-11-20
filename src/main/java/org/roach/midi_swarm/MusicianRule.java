package org.roach.midi_swarm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public abstract class MusicianRule {

	protected Musician musician;
	protected final Logger logger = LogManager.getLogger(getClass());

	/**
	 * @param musician the {@link Musician} that this rule applies to
	 */
	public void setMusician(Musician musician) {
		this.musician = musician;
	}

	/**
	 * Take action
	 * 
	 * @param tick the tick number
	 */
	public abstract void act(long tick);
}
