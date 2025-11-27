package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.MusicianRule;

/**
 * Represents an action that may be performed by a {@link MusicianRule}
 */
@FunctionalInterface
public interface MusicalAction {
	/**
	 * Perform the action
	 */
	void perform();
}
