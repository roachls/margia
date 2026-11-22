package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Tells the {@link Musician} to play the given chord, but with
 * <code>diff</code> less velocity. This will never result in a velocity below
 * 0.
 * 
 * @param musician the musician
 * @param message  the message being transformed, ignored if not a {@link Chord}
 * @param diff     the difference in velocity
 */
public record PlayChordDownVelocity(Musician musician, MusicianMessage message, int diff) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord)
            musician.playChord(chord.withVelocity(chord.getVelocity() - diff));
    }

}
