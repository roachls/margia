package org.roach.margia.model;

import java.util.Objects;

import org.roach.margia.storage.Options;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Music-related options
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class MusicOptions {
    private IntegerProperty tempo = new SimpleIntegerProperty(DEFAULT_TEMPO);
    private int tempoMinimum = 1;
    private int tempoMaximum = 300;
    private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;

    /**
     * Default tempo
     */
    public static final int DEFAULT_TEMPO = 60;
    /**
     * default maximum queue size
     */
    public static final int DEFAULT_MAX_QUEUE_SIZE = 12;
    /**
     * the property fired when the tempo changes
     */
    public static final String TEMPO_PROPERTY = "music.tempo";

    /**
     * @return the tempo
     */
    public int getTempo() { return tempo.get(); }

    /**
     * @param tempo the tempo to set
     */
    public void setTempo(int tempo) {
        var oldTempo = this.tempo.get();
        this.tempo.set(tempo);
        if (oldTempo != tempo) {
            Options.getInstance().setDirty();
        }
    }
    
    /**
     * @return the tempo property
     */
    public IntegerProperty tempoProperty() { return this.tempo; }

    /**
     * @return the tempoMinimum
     */
    public int getTempoMinimum() { return tempoMinimum; }

    /**
     * @param tempoMinimum the tempoMinimum to set
     */
    public void setTempoMinimum(int tempoMinimum) {
        var oldTempoMin = this.tempoMinimum;
        this.tempoMinimum = tempoMinimum;
        if (oldTempoMin != this.tempoMinimum)
            Options.getInstance().setDirty();
    }

    /**
     * @return the tempoMaximum
     */
    public int getTempoMaximum() { return tempoMaximum; }

    /**
     * @param tempoMaximum the tempoMaximum to set
     */
    public void setTempoMaximum(int tempoMaximum) {
        var old = this.tempoMaximum;
        this.tempoMaximum = tempoMaximum;
        if (old != this.tempoMaximum)
            Options.getInstance().setDirty();
    }

    /**
     * @return the maximum size that a musician's queue is allowed to grow to; old
     *         events past this size will be replaced by new events
     */
    public int getMaxQueueSize() { return maxQueueSize; }

    /**
     * @param maxQueueSize the maximum size that a musician's queue is allowed to
     *                     grow to; old events past this size will be replaced by
     *                     new events
     */
    public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }

    @Override
    public String toString() {
        return "MusicOptions [tempo=" + tempo + ", tempoMinimum=" + tempoMinimum + ", tempoMaximum=" + tempoMaximum
                + ", maxQueueSize=" + maxQueueSize + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxQueueSize, tempo, tempoMaximum, tempoMinimum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MusicOptions other = (MusicOptions) obj;
        return maxQueueSize == other.maxQueueSize && tempo == other.tempo && tempoMaximum == other.tempoMaximum
                && tempoMinimum == other.tempoMinimum;
    }
}