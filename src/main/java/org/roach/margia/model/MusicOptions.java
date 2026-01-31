package org.roach.margia.model;

import javax.swing.event.ChangeListener;

import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

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
    private int tempo = DEFAULT_TEMPO;
    private int tempoMinimum = 1;
    private int tempoMaximum = 300;
    private final ChangeEmitter emitter = new ChangeEmitter();
    /**
     * Default tempo
     */
    public static final int DEFAULT_TEMPO = 60;
    /**
     * the property fired when the tempo changes
     */
    public static final String TEMPO_PROPERTY = "music.tempo";

    /**
     * @return the tempo
     */
    public int getTempo() { return tempo; }

    /**
     * @param tempo the tempo to set
     */
    public void setTempo(int tempo) {
        var oldTempo = this.tempo;
        this.tempo = tempo;
        if (oldTempo != tempo) {
            emitter.fireChangeEvent(TEMPO_PROPERTY, new ChangeSource(TEMPO_PROPERTY, tempo));
            Options.getInstance().setDirty();
        }
    }

    /**
     * @return the tempoMinimum
     */
    public int getTempoMinimum() { return tempoMinimum; }

    /**
     * @param tempoMinimum the tempoMinimum to set
     */
    public void setTempoMinimum(int tempoMinimum) { this.tempoMinimum = tempoMinimum; }

    /**
     * @return the tempoMaximum
     */
    public int getTempoMaximum() { return tempoMaximum; }

    /**
     * @param tempoMaximum the tempoMaximum to set
     */
    public void setTempoMaximum(int tempoMaximum) { this.tempoMaximum = tempoMaximum; }

    /**
     * @param property name of property
     * @param listener listener of property
     */
    public void addChangeListener(String property, ChangeListener listener) {
        this.emitter.addChangeListener(property, listener);
    }

    @Override
    public String toString() {
        return "MusicOptions [tempo=" + tempo + "]";
    }
}