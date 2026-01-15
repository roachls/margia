package org.roach.margia.storage;

import java.util.*;

import org.roach.margia.Musician;

/**
 * All stored properties
 */
public class StoredOptions {
    private final MusicOptions musicOptions = new MusicOptions();
    private final MidiOptions midiOptions = new MidiOptions();
    private final UiOptions uiOptions = new UiOptions();
    private final Map<Integer, MusicianOptions> musicians = new LinkedHashMap<>();

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

}