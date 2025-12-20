package org.roach.margia.actions;

import org.roach.margia.MusicianRule;

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
