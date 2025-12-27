package org.roach.margia;

import org.roach.margia.messages.MusicianMessage;

/**
 * @param noteNum  the note to play
 * @param velocity the velocity of the note
 * @param length   the length of the note in ticks
 * 
 */
public record NoteInfo(int noteNum, int velocity, int length) implements MusicianMessage {

    /**
     * Sanity check params
     * 
     * @param noteNum  note number (0-127)
     * @param velocity velocity (0-127)
     * @param length   length in ticks (&gt;= 1)
     */
    public NoteInfo {
        if (noteNum < -1)
            noteNum = -1;
        else if (noteNum > 127)
            noteNum = 127;
        if (velocity < Musician.MIN_VELOCITY)
            velocity = Musician.MIN_VELOCITY;
        else if (velocity > Musician.MAX_VELOCITY)
            velocity = Musician.MAX_VELOCITY;
        if (length < 1)
            length = 1;
    }

    /**
     * @param newNoteNum new note number
     * @return a copy of this {@link NoteInfo} with the given note number
     */
    public NoteInfo withNote(int newNoteNum) {
        return new NoteInfo(newNoteNum, velocity, length);
    }

    /**
     * @param newVelocity new velocity
     * @return a copy of this {@link NoteInfo} with the new velocity
     */
    public NoteInfo withVelocity(int newVelocity) {
        return new NoteInfo(noteNum, newVelocity, length);
    }

    /**
     * @param newLength new length in ticks
     * @return a copy of this {@link NoteInfo} with the new length
     */
    public NoteInfo withLength(int newLength) {
        return new NoteInfo(noteNum, velocity, newLength);
    }
}
