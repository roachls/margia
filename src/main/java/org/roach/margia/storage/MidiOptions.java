package org.roach.margia.storage;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;

/**
 * MIDI-related options
 */
public class MidiOptions {
    private boolean useExternalMidi;
    private final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * @return whether to use external MIDI devices
     */
    public boolean isUseExternalMidi() { return useExternalMidi; }

    /**
     * @param useExternalMidi whether to use external MIDI devices
     */
    public void setUseExternalMidi(boolean useExternalMidi) { this.useExternalMidi = useExternalMidi; }

    void addChangeListener(String key, ChangeListener listener) {
        this.emitter.addChangeListener(key, listener);
    }
}