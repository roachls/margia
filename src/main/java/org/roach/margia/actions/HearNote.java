package org.roach.margia.actions;

import org.roach.margia.Chord;
import org.roach.margia.Musician;

/**
 * Tells the {@link Musician} that it "heard" the given chord, as if a peer
 * played it
 * 
 * @param musician the {@link Musician}
 * @param chord    the chord
 */
public record HearNote(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.receiveMessage(chord);
    }

}
