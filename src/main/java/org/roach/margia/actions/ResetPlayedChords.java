package org.roach.margia.actions;

import org.roach.margia.Musician;

/**
 * Tell the given musician to reset its count of how many chords it has played
 * 
 * @param musician the musician
 */
public record ResetPlayedChords(Musician musician) implements MusicalAction {

    @Override
    public void perform() {
        musician.resetChordsIvePlayed();
    }

}
