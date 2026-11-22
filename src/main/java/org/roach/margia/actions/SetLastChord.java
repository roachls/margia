package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tell the given musician to pretend that the given chord is the last chord
 * that it played
 * 
 * @param musician musician
 * @param message  the message being changed, ignored if not a {@link Chord}
 * 
 * 
 */
public record SetLastChord(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.setMyLastChord(chord);
    }

}
