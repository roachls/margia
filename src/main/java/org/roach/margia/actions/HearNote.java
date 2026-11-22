package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tells the {@link Musician} that it "heard" the given chord, as if a peer
 * played it
 * 
 * @param musician the {@link Musician}
 * @param message  the message being changed, ignored if not a {@link Chord}
 */
public record HearNote(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        musician.receiveMessage(message);
    }

}
