package org.roach.margia;

import java.util.*;
import java.util.stream.Collectors;

import org.roach.margia.messages.MusicianMessage;

/**
 * @param notes    notes in the chord
 * @param length   length to play the chord (in ticks)
 * @param velocity velocity to play the chord
 * 
 */
public record Chord(Set<Integer> notes, int length, int velocity) implements MusicianMessage {

    /**
     * Sanity-checking of notes
     */
    public Chord {
        if (length < 1)
            length = 1;
        notes = notes.stream().map(n -> Math.clamp(n, -1, 127)).collect(Collectors.toSet());
        velocity = Math.clamp(velocity, Musician.MIN_VELOCITY, Musician.MAX_VELOCITY);
    }

    /**
     * @return the set of notes in the cord
     */
    public Set<Integer> getNotes() { return Collections.unmodifiableSet(notes); }

    /**
     * @return duration to play the chord in ticks
     */
    public int getLength() { return length; }

    /**
     * @return velocity to play the chord
     */
    public int getVelocity() { return velocity; }

    /**
     * @param newLength new duration in ticks
     * @return a new {@link Chord} that is a copy of this one but with the new
     *         length
     */
    public Chord withLength(int newLength) {
        return new Chord(this.notes, newLength, this.velocity);
    }

    /**
     * @param newNotes new notes
     * @return a new {@link Chord} with the given notes but the same velocity and
     *         length
     */
    public Chord withNotes(Collection<Integer> newNotes) {
        return new Chord(newNotes.stream().collect(Collectors.toSet()), this.length, this.velocity);
    }

    /**
     * @param newVelocity new velocity
     * @return a new {@link Chord} that is a copy of this one but with the given
     *         velocity
     */
    public Chord withVelocity(int newVelocity) {
        return new Chord(this.notes, this.length, newVelocity);
    }

    /**
     * @return the average note in this chord (used by the UI)
     */
    public float averageNote() {
        return notes.stream().map(Double::valueOf).collect(Collectors.averagingDouble(d -> d)).floatValue();
    }

}
