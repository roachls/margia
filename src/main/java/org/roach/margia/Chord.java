package org.roach.margia;

import java.util.*;
import java.util.stream.Collectors;

import org.roach.margia.messages.MusicianMessage;

public class Chord implements MusicianMessage {
    private final List<Integer> notes = new ArrayList<>();
    private final int length;
    private final int velocity;

    public Chord(List<Integer> notes, int length, int velocity) {
        this.length = length;
        for (var note : notes) {
            if (note < -1)
                this.notes.add(-1);
            else if (note > 127)
                this.notes.add(127);
            else
                this.notes.add(note);
        }
        this.notes.addAll(notes);
        if (velocity < Musician.MIN_VELOCITY)
            this.velocity = Musician.MIN_VELOCITY;
        else if (velocity > Musician.MAX_VELOCITY)
            this.velocity = Musician.MAX_VELOCITY;
        else
            this.velocity = velocity;
    }

    public List<Integer> getNotes() { return Collections.unmodifiableList(notes); }

    public int getLength() { return length; }

    public int getVelocity() { return velocity; }

    public Chord withLength(int newLength) {
        return new Chord(this.notes, newLength, this.velocity);
    }

    public Chord withNotes(List<Integer> newNotes) {
        return new Chord(newNotes, this.length, this.velocity);
    }

    @Override
    public String toString() {
        return "Chord [notes=" + notes + ", length=" + length + ", velocity=" + velocity + "]";
    }

    public Chord withVelocity(int newVelocity) {
        return new Chord(this.notes, this.length, newVelocity);
    }

    public float averageNote() {
        return notes.stream().map(Double::valueOf).collect(Collectors.averagingDouble(d -> d)).floatValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, notes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Chord other = (Chord) obj;
        return length == other.length && Objects.equals(notes, other.notes);
    }
}
