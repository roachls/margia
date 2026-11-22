package org.roach.margia.model;

import org.roach.margia.controller.Musician;

/**
 * Represents the range of notes that a {@link Musician} is allowed to play
 * 
 * @param low  the lowest note in the range
 * @param high the highest note in the range
 */
public record NoteRange(int low, int high) {

    /**
     * @param low  low note
     * @param high high note
     * @throws IllegalArgumentException if low or high are outside of the range
     *                                  0-127, or if low > high
     */
    public NoteRange {
        if (low < 0 || low > 127)
            throw new IllegalArgumentException("Range low must be between 0 and 127 inclusive");
        if (high < 0 || high > 127)
            throw new IllegalArgumentException("Range high must be between 0 and 127 inclusive");
        if (low > high)
            throw new IllegalArgumentException("Range low must be less than range high");
    }

    /**
     * Constructs a range of the given octave
     * 
     * @param octave the octave
     */
    public NoteRange(Octave octave) {
        this(octave.low, octave.high);
    }

    /**
     * Constructs a range from the lowest note of the start octave to the highest
     * note of the end octave
     * 
     * @param start start {@link Octave}
     * @param end   end {@link Octave}
     */
    public NoteRange(Octave start, Octave end) {
        this(start.low, end.high);
    }

    /**
     * @param newLow new low note
     * @return a new {@link NoteRange} with the given low note
     */
    public NoteRange withLow(int newLow) {
        return new NoteRange(newLow, this.high);
    }

    /**
     * @return the span from low to high
     */
    public int span() {
        return high - low;
    }

    /**
     * @param newHigh the new high note
     * @return a new {@link NoteRange} with the given high note
     */
    public NoteRange withHigh(int newHigh) {
        return new NoteRange(this.low, newHigh);
    }

    /**
     * Moves the note by octaves until it is within range
     * 
     * @param originalNote the original note
     * @return a note that is in the range of this {@link NoteRange}
     */
    public int adjustToRangeByOctaves(int originalNote) {
        int nStart = originalNote;
        if (nStart > high) {
            while (nStart > high) {
                nStart -= 12;
            }
        } else if (nStart < low) {
            while (nStart < low) {
                nStart += 12;
            }
        }
        return nStart;
    }

    public static final NoteRange DRUMPAD = new NoteRange(Octave.O1);

    /**
     * @return a new {@link NoteRange} instance that is a copy of this one
     */
    public NoteRange copy() {
        return new NoteRange(low, high);
    }
}
