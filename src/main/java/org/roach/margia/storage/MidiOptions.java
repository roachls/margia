package org.roach.margia.storage;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * MIDI-related options
 */
public class MidiOptions {
    private boolean useExternalMidi;
    private final ChangeEmitter emitter = new ChangeEmitter();
    /**
     * property fired when useExternalMidi changes
     */
    public static final String EXTERNAL_MIDI_PROPERTY = "useExternalMidi";

    /**
     * @return whether to use external MIDI devices
     */
    public boolean isUseExternalMidi() { return useExternalMidi; }

    /**
     * @param useExternalMidi whether to use external MIDI devices
     */
    public void setUseExternalMidi(boolean useExternalMidi) {
        var oldUseExternalMidi = this.useExternalMidi;
        this.useExternalMidi = useExternalMidi;
        if (oldUseExternalMidi != this.useExternalMidi)
            emitter.fireChangeEvent(EXTERNAL_MIDI_PROPERTY,
                    new ChangeSource(EXTERNAL_MIDI_PROPERTY, this.useExternalMidi));
    }

    /**
     * @param key      name of property being listened for
     * @param listener listener for property
     */
    public void addChangeListener(String key, ChangeListener listener) {
        this.emitter.addChangeListener(key, listener);
    }

    @Override
    public String toString() {
        return "MidiOptions [useExternalMidi=" + useExternalMidi + "]";
    }
}