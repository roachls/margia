package org.roach.margia.storage;

import javax.swing.event.ChangeListener;

import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * Music-related options
 */
public class MusicOptions {
    private int tempo = TimingSource.DEFAULT_TEMPO;
    private final ChangeEmitter emitter = new ChangeEmitter();

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
        if (oldTempo != tempo)
            emitter.fireChangeEvent(TimingSource.TEMPO_PROPERTY,
                    new ChangeSource(this, TimingSource.TEMPO_PROPERTY, tempo));
    }

    void addChangeListener(String property, ChangeListener listener) {
        this.emitter.addChangeListener(property, listener);
    }
}