package org.roach.margia.actions;

import org.roach.margia.Musician;

/**
 * Tell the {@link Musician} to repeat its last chord
 * 
 * @param musician the musician
 */
public record RepeatLastChord(Musician musician) implements MusicalAction {

    @Override
    public void perform() {
        musician.playChord(musician.getMyLastChord());
    }

}
