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

    private boolean useExternalMidi;
    private boolean sendingMidiTimecode;
    private boolean autoStartOnNoteOn = true;
    private int tempoController;
    private boolean sendPanMessage = true;
    private int panController = DEFAULT_PAN_CONTROLLER;
    private boolean panWithRelativeLocations = true;
    private boolean sendVerticalPanMessage = true;
    private int verticalPanController = DEFAULT_VERTICAL_PAN_CONTROLLER;
    private boolean verticalPanWithRelativeLocations = true;
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
     * @return the sendPanMessage
     */
    public boolean isSendPanMessage() { return sendPanMessage; }

    /**
     * @param sendPanMessage the sendPanMessage to set
     */
    public void setSendPanMessage(boolean sendPanMessage) { this.sendPanMessage = sendPanMessage; }

    /**
     * @return the panController
     */
    public int getPanController() { return panController; }

    /**
     * @param panController the panController to set
     */
    public void setPanController(int panController) { this.panController = panController; }

    /**
     * @return the panWithRelativeLocations
     */
    public boolean isPanWithRelativeLocations() { return panWithRelativeLocations; }

    /**
     * @param panWithRelativeLocations the panWithRelativeLocations to set
     */
    public void setPanWithRelativeLocations(boolean panWithRelativeLocations) {
        this.panWithRelativeLocations = panWithRelativeLocations;
    }

    /**
     * @return the sendVerticalPanMessage
     */
    public boolean isSendVerticalPanMessage() { return sendVerticalPanMessage; }

    /**
     * @param sendVerticalPanMessage the sendVerticalPanMessage to set
     */
    public void setSendVerticalPanMessage(boolean sendVerticalPanMessage) {
        this.sendVerticalPanMessage = sendVerticalPanMessage;
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
    public boolean isVerticalPanWithRelativeLocations() { return verticalPanWithRelativeLocations; }

    /**
     * @param verticalPanWithRelativeLocations the panWithRelativeLocations to set
     */
    public void setVerticalPanWithRelativeLocations(boolean verticalPanWithRelativeLocations) {
        this.verticalPanWithRelativeLocations = verticalPanWithRelativeLocations;
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
        return "MidiOptions [useExternalMidi=" + useExternalMidi + ", sendingMidiTimecode=" + sendingMidiTimecode
                + ", autoStartOnNoteOn=" + autoStartOnNoteOn + ", tempoController=" + tempoController + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoStartOnNoteOn, sendingMidiTimecode, tempoController, useExternalMidi);
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
                && tempoController == other.tempoController && useExternalMidi == other.useExternalMidi;
    }
}