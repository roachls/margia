package org.roach.margia.storage;

import org.roach.margia.ui.ChangeEmitter;

/**
 * MIDI-related options
 */
public class MidiOptions {
    boolean useExternalMidi;
    final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * @return whether to use external MIDI devices
     */
    public boolean isUseExternalMidi() { return useExternalMidi; }

    /**
     * @param useExternalMidi whether to use external MIDI devices
     */
    public void setUseExternalMidi(boolean useExternalMidi) { this.useExternalMidi = useExternalMidi; }
}