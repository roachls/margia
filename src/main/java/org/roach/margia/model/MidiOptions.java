package org.roach.margia.model;

import java.util.Objects;

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
    /**
     * default pan controller
     */
    public static final int DEFAULT_PAN_CONTROLLER = 77;
    /**
     * default vertical pan controller
     */
    public static final int DEFAULT_VERTICAL_PAN_CONTROLLER = 78;

    private boolean usingExternalMidi;
    private boolean sendingMidiTimecode;
    private boolean autoStartOnNoteOn = true;
    private int tempoController;
    private boolean sendPanMessage = true;
    private int panController = DEFAULT_PAN_CONTROLLER;
    private boolean panWithRelativeLocations = true;
    private boolean sendVerticalPanMessage = true;
    private int verticalPanController = DEFAULT_VERTICAL_PAN_CONTROLLER;
    private boolean verticalPanWithRelativeLocations = true;
    private boolean usingExternalTiming;
    private final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * property fired when useExternalMidi changes
     */
    public static final String EXTERNAL_MIDI_PROPERTY = "useExternalMidi";

    /**
     * @return whether to use external MIDI devices
     */
    public boolean isUsingExternalMidi() { return usingExternalMidi; }

    /**
     * @param usingExternalMidi whether to use external MIDI devices
     */
    public void setUsingExternalMidi(boolean usingExternalMidi) {
        var oldUsingExternalMidi = this.usingExternalMidi;
        this.usingExternalMidi = usingExternalMidi;
        if (oldUsingExternalMidi != this.usingExternalMidi) {
            emitter.fireChangeEvent(EXTERNAL_MIDI_PROPERTY,
                    new ChangeSource(EXTERNAL_MIDI_PROPERTY, this.usingExternalMidi));
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
        if (!usingExternalMidi && sendingMidiTimecode)
            return; // can't send timecodes if not using external MIDI
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
        if (!usingExternalMidi && autoStartOnNoteOn)
            return; // can't receive notes if not using external MIDI
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
    public void setTempoController(int tempoController) {
        var oldTempoController = this.tempoController;
        this.tempoController = tempoController;
        if (oldTempoController != this.tempoController)
            Options.getInstance().setDirty();
    }

    /**
     * @return the sendPanMessage
     */
    public boolean isSendPanMessage() { return sendPanMessage; }

    /**
     * @param sendPanMessage the sendPanMessage to set
     */
    public void setSendPanMessage(boolean sendPanMessage) {
        var oldSendPanMessage = this.sendPanMessage;
        this.sendPanMessage = sendPanMessage;
        if (oldSendPanMessage != this.sendPanMessage)
            Options.getInstance().setDirty();
    }

    /**
     * @return the panController
     */
    public int getPanController() { return panController; }

    /**
     * @param panController the panController to set
     */
    public void setPanController(int panController) {
        var oldPanController = this.panController;
        this.panController = panController;
        if (oldPanController != this.panController)
            Options.getInstance().setDirty();
    }

    /**
     * @return the panWithRelativeLocations
     */
    public boolean isPanWithRelativeLocations() { return panWithRelativeLocations; }

    /**
     * @param panWithRelativeLocations the panWithRelativeLocations to set
     */
    public void setPanWithRelativeLocations(boolean panWithRelativeLocations) {
        var oldPanWithRelativeLocations = this.panWithRelativeLocations;
        this.panWithRelativeLocations = panWithRelativeLocations;
        if (oldPanWithRelativeLocations != this.panWithRelativeLocations)
            Options.getInstance().setDirty();
    }

    /**
     * @return the sendVerticalPanMessage
     */
    public boolean isSendVerticalPanMessage() { return sendVerticalPanMessage; }

    /**
     * @param sendVerticalPanMessage the sendVerticalPanMessage to set
     */
    public void setSendVerticalPanMessage(boolean sendVerticalPanMessage) {
        var oldSendVerticalPanMessage = this.sendVerticalPanMessage;
        this.sendVerticalPanMessage = sendVerticalPanMessage;
        if (oldSendVerticalPanMessage != this.sendVerticalPanMessage)
            Options.getInstance().setDirty();
    }

    /**
     * @return the verticalPanController
     */
    public int getVerticalPanController() { return verticalPanController; }

    /**
     * @param verticalPanController the panController to set
     */
    public void setVerticalPanController(int verticalPanController) {
        this.verticalPanController = verticalPanController;
    }

    /**
     * @return the verticalPanWithRelativeLocations
     */
    public boolean isVerticalPanWithRelativeLocations() {
        return verticalPanWithRelativeLocations;
    }

    /**
     * @param verticalPanWithRelativeLocations the panWithRelativeLocations to set
     */
    public void setVerticalPanWithRelativeLocations(boolean verticalPanWithRelativeLocations) {
        var old = this.verticalPanWithRelativeLocations;
        this.verticalPanWithRelativeLocations = verticalPanWithRelativeLocations;
        if (old != this.verticalPanWithRelativeLocations)
            Options.getInstance().setDirty();
    }

    /**
     * @return the usingExternalTiming
     */
    public boolean isUsingExternalTiming() { return usingExternalMidi && usingExternalTiming; }

    /**
     * @param usingExternalTiming the usingExternalTiming to set
     */
    public void setUsingExternalTiming(boolean usingExternalTiming) {
        if (!usingExternalMidi && usingExternalTiming)
            return;
        var oldUsingExternalTiming = this.usingExternalTiming;
        this.usingExternalTiming = usingExternalTiming;
        if (oldUsingExternalTiming != this.usingExternalTiming) {
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
        return "MidiOptions [useExternalMidi=" + usingExternalMidi + ", sendingMidiTimecode=" + sendingMidiTimecode
                + ", autoStartOnNoteOn=" + autoStartOnNoteOn + ", tempoController=" + tempoController + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoStartOnNoteOn, sendingMidiTimecode, tempoController, usingExternalMidi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MidiOptions other = (MidiOptions) obj;
        return autoStartOnNoteOn == other.autoStartOnNoteOn && sendingMidiTimecode == other.sendingMidiTimecode
                && tempoController == other.tempoController && usingExternalMidi == other.usingExternalMidi;
    }
}