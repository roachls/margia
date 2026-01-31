package org.roach.margia.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.roach.margia.controller.Musician;

/**
 * All stored properties; serves as the top-level YAML object
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class StoredOptions {
    private final MusicOptions musicOptions = new MusicOptions();
    private final MidiOptions midiOptions = new MidiOptions();
    private final UiOptions uiOptions = new UiOptions();
    private final Map<Integer, MusicianOptions> musicians = new LinkedHashMap<>();
    private long randomSeed = 101;

    /**
     * @return all music-related options
     */
    public MusicOptions getMusicOptions() { return musicOptions; }

    /**
     * @return all MIDI-related options
     */
    public MidiOptions getMidiOptions() { return midiOptions; }

    /**
     * @return all UI-related options
     */
    public UiOptions getUiOptions() { return uiOptions; }

    /**
     * @return all {@link Musician}-related options, indexed by ID
     */
    public Map<Integer, MusicianOptions> getMusicians() { return musicians; }

    @Override
    public String toString() {
        return "StoredOptions [musicOptions=" + musicOptions + ", midiOptions=" + midiOptions + ", uiOptions="
                + uiOptions + ", musicians=" + musicians + ", randomSeed=" + randomSeed + "]";
    }

    /**
     * @return the randomSeed
     */
    public long getRandomSeed() { return randomSeed; }

    /**
     * @param randomSeed the randomSeed to set
     */
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }

}