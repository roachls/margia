package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tells the {@link Musician} to play the given chord, but with {@code diff}
 * more velocity. This will never result in a velocity above
 * {@link Musician#MAX_VELOCITY}.
 * 
 * @param musician the musician
 * @param message  the message being changed, ignored if not a {@link Chord}
 * @param diff     the difference in velocity
 */
public record PlayChordUpVelocity(Musician musician, MusicianMessage message, int diff) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.playChord(chord.withVelocity(chord.getVelocity() + diff));
    }

}
