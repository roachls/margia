package org.roach.margia.actions;

import org.roach.margia.storage.Options;

/**
 * @param musicianId ID of the musician to mute
 */
public record UnmuteMusician(int musicianId) implements MusicalAction {

    @Override
    public void perform() {
        var musician = Options.getInstance().getMusicians().get(musicianId);
        if (musician != null)
            musician.setMuted(false);
    }

}
