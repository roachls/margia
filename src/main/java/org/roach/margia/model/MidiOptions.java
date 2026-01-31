package org.roach.margia.model;

import javax.swing.event.ChangeListener;

import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * MIDI-related options
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class MidiOptions {
    private boolean useExternalMidi;
    private boolean sendingMidiTimecode;
    private boolean autoStartOnNoteOn = true;
    private int tempoController;
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
     * @return the autoStartOnNoteOn
     */
    public boolean isAutoStartOnNoteOn() { return autoStartOnNoteOn; }

    /**
     * @param autoStartOnNoteOn the autoStartOnNoteOn to set
     */
    public void setAutoStartOnNoteOn(boolean autoStartOnNoteOn) {
        var oldAutoStartOnNoteOn = this.autoStartOnNoteOn;
        this.autoStartOnNoteOn = autoStartOnNoteOn;
        if (oldAutoStartOnNoteOn != this.autoStartOnNoteOn)
            Options.getInstance().setDirty();
    }

    /**
     * @return the tempoController
     */
    public int getTempoController() { return tempoController; }

    /**
     * @param tempoController the tempoController to set
     */
    public void setTempoController(int tempoController) { this.tempoController = tempoController; }

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