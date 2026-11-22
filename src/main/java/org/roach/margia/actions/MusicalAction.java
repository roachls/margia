package org.roach.margia.actions;

import org.roach.margia.controller.rules.AbstractMusicianRule;

/**
 * Represents an action that may be performed by a {@link AbstractMusicianRule}
 */
@FunctionalInterface
public interface MusicalAction {
    /**
     * Perform the action
     */
    void perform();
}
