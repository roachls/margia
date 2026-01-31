package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;

/**
 * Tell the given musician to pretend that the given chord is the last chord
 * that it played
 * 
 * @param musician musician
 * @param chord    the chord
 * 
 */
public record SetLastChord(Musician musician, Chord chord) implements MusicalAction {

    @Override
    public void perform() {
        musician.setMyLastChord(chord);
    }

}
