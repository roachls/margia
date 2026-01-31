package org.roach.margia.actions;

import org.roach.margia.controller.Musician;

/**
 * Have the musician rest one tick
 * 
 * @param musician the musician
 */
public record RestOneTick(Musician musician) implements MusicalAction {

    @Override
    public void perform() {
        musician.rest();
    }

}
