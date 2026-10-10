package org.roach.margia.actions;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Command to play the given note up an octave. Note that this is different from
 * {@link PlayChordUpInterval} with an interval of 12 or 8, which may not be
 * octaves depending on the current scale. (If the scale is
 * {@link org.roach.margia.model.Key#Chromatic}, then this is the equivalent of
 * {@link PlayChordUpInterval} with an interval of 12. Note that if the
 * transformed note ends up above the key's range, it will be automatically
 * adjusted back.
 * 
 * @param musician {@link Musician} that will play the note
 * @param message  the message being changed, ignored if not a {@link Chord}
 */
public record PlayNoteUpOctave(Musician musician, MusicianMessage message) implements MusicalAction {

    @Override
    public void perform() {
        if (message instanceof Chord chord) {
            var newNotes = chord.getNotes().stream().map(note -> musician.getRange().adjustToRangeByOctaves(note + 12))
                    .toList();
            musician.playChord(chord.withNotes(newNotes));
        }
    }

}
