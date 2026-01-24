package org.roach.margia.storage;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * MIDI-related options
 */
public class MidiOptions {
    private boolean useExternalMidi;
    private boolean sendingMidiTimecode;
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
        if (oldUseExternalMidi != this.useExternalMidi) {
            emitter.fireChangeEvent(EXTERNAL_MIDI_PROPERTY,
                    new ChangeSource(EXTERNAL_MIDI_PROPERTY, this.useExternalMidi));
            Options.getInstance().setDirty();
        }
    }

    /**
     * @return the sendingMidiTimecode
     */
    public boolean isSendingMidiTimecode() { return sendingMidiTimecode; }

    /**
     * @param sendingMidiTimecode the sendingMidiTimecode to set
     */
    public void setSendingMidiTimecode(boolean sendingMidiTimecode) {
        var oldControlDawTiming = this.sendingMidiTimecode;
        this.sendingMidiTimecode = sendingMidiTimecode;
        if (oldControlDawTiming != this.sendingMidiTimecode) {
            Options.getInstance().setDirty();
        }
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
        return "MidiOptions [useExternalMidi=" + useExternalMidi + ", controlDawTiming=" + sendingMidiTimecode + "]";
    }
}