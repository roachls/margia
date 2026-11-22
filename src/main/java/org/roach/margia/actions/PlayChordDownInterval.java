package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Command to play the given note down the given interval
 * 
 * @param musician {@link Musician} that will play the note
 * @param message  message to transform (ignored if not a {@link Chord})
 * @param interval interval by which to go down, relative to the scale. An
 *                 interval of 1 means no change. An interval of 2 is a "second"
 *                 away as determined by the key.
 */
public record PlayChordDownInterval(Musician musician, MusicianMessage message, int interval) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord) {
            if (Musician.REST.equals(chord))
                return;
            var noteList = chord.getNotes().stream().map(n -> musician.getKey().down(n, interval)).toList();
            musician.playChord(chord.withNotes(noteList));
        }
    }

}
