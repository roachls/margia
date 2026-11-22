package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tells the musician to play the given note
 * 
 * @param musician the musician
 * @param message  the message to play
 */
public record PlayChord(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.playChord(chord);
    }

}
