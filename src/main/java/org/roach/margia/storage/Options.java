package org.roach.margia.storage;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeListener;

import org.roach.margia.*;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.*;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Singleton instance where all options are stored
 */
@SuppressWarnings({ "java:S3008", "java:S6548" })
public class Options {
    /**
     * Property fired to notify listeners that something has changed
     */
    public static final String DIRTY_PROPERTY = "dirty";
    private final ChangeEmitter emitter;
    private boolean dirty;
    private Path saveDir;
    private Path filename;
    private Preferences preferences;
    private static Options INSTANCE;
    private StoredOptions storedOptions;

    private Options() {
        this.emitter = new ChangeEmitter();
        this.storedOptions = new StoredOptions();
        preferences = Preferences.userNodeForPackage(getClass());
        this.saveDir = Path.of(preferences.get("saveDir", System.getProperty("user.home")));
    }

    /**
     * @return the single instance of {@link Options}
     */
    public static Options getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Options();
        return INSTANCE;
    }

    /**
     * Store current options to output stream
     * 
     * @param os output stream to write to
     * @throws IOException if there is an error writing to a file
     */
    public void store(OutputStream os) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        mapper.writeValue(os, storedOptions);
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, "false"));
    }

    /**
     * Read options from input stream
     * 
     * @param is input stream to read from
     * @throws IOException if there is an error reading from the stream
     */
    public void load(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        StoredOptions loadedOpts;
        try {
            loadedOpts = mapper.readValue(is, StoredOptions.class);
        } catch (MismatchedInputException _) {
            System.err.println("Parameter file was empty or malformed");
            return;
        }
        if (loadedOpts != null) {
            this.storedOptions = loadedOpts;
        }
        var sendExternalMidi = storedOptions.getMidiOptions().useExternalMidi;
        var tempo = storedOptions.getMusicOptions().getTempo();
        if (sendExternalMidi) {
            MidiController.init(MidiController.LOOP_MIDI, tempo);
        } else {
            MidiController.init(MidiController.DEFAULT_SYNTH, tempo);
        }
        restoreMusicians();
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, "false"));
    }

    /**
     * @return true if any property has changed since the last save/load
     */
    public boolean isDirty() { return dirty; }

    /**
     * @param property the property being listened to
     * @param listener the listener
     */
    public void addChangeListener(String property, ChangeListener listener) {
        emitter.addChangeListener(property, listener);
        storedOptions.midiOptions.emitter.addChangeListener(property, listener);
        storedOptions.uiOptions.emitter.addChangeListener(property, listener);
        storedOptions.musicOptions.emitter.addChangeListener(property, listener);
    }

    /**
     * @return the current save directory
     */
    public Path getSaveDir() { return saveDir; }

    /**
     * @param saveDir save directory
     * @throws BackingStoreException if there is an error saving the directory to
     *                               preferences
     */
    public void setSaveDir(Path saveDir) throws BackingStoreException {
        this.saveDir = saveDir;
        preferences.put("saveDir", Options.getInstance().getSaveDir().toString());
        preferences.flush();
    }

    /**
     * @return the current filename
     */
    public Path getFilename() { return filename; }

    /**
     * @param filename the current filename
     */
    public void setFilename(Path filename) { this.filename = filename; }

    /**
     * Initialize the list of musicians
     */
    private void restoreMusicians() {
        MusicianList.getInstance().restoreFromStorage(storedOptions.musicians);
    }

    /**
     * @return all music-related properties
     */
    public MusicOptions getMusicOptions() { return storedOptions.getMusicOptions(); }

    /**
     * @return all MIDI-related properties
     */
    public MidiOptions getMidiOptions() { return storedOptions.getMidiOptions(); }

    /**
     * @return all UI-related properties
     */
    public UiOptions getUiOptions() { return storedOptions.getUiOptions(); }

    /**
     * @return a map of all {@link Musician}-related properties, indexed by musician
     *         ID
     */
    public Map<Integer, MusicianOptions> getMusicians() { return storedOptions.getMusicians(); }

    /**
     * All stored properties
     */
    public static class StoredOptions {
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

    /**
     * MIDI-related options
     */
    public static class MidiOptions {
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
    }

    /**
     * Music-related options
     */
    public static class MusicOptions {
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

    }

    /**
     * UI-related options
     */
    public static class UiOptions {
        private final ChangeEmitter emitter = new ChangeEmitter();
        private boolean showNumbers = true;
        private int radius = MusicianComponent.DEFAULT_RADIUS;
        private double gravity = AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT;
        private int edgeLength = AgentPanel.DEFAULT_EDGE_LENGTH;
        private double mass = 1.0;

        /**
         * @return true if numbers of musicians should be displayed
         */
        public boolean isShowNumbers() { return showNumbers; }

        /**
         * @param showNumbers the showNumbers to set
         */
        public void setShowNumbers(boolean showNumbers) {
            var oldShowNumbers = this.showNumbers;
            this.showNumbers = showNumbers;
            if (oldShowNumbers != showNumbers)
                emitter.fireChangeEvent(MusicianComponent.SHOW_NUMBERS_PROPERTY,
                        new ChangeSource(this, MusicianComponent.SHOW_NUMBERS_PROPERTY, this.showNumbers));
        }

        /**
         * @return the radius
         */
        public int getRadius() { return radius; }

        /**
         * @param radius the radius to set
         */
        public void setRadius(int radius) {
            var oldRadius = this.radius;
            this.radius = radius;
            if (oldRadius != radius)
                emitter.fireChangeEvent(MusicianComponent.RADIUS_PROPERTY,
                        new ChangeSource(this, MusicianComponent.RADIUS_PROPERTY, this.radius));
        }

        /**
         * @return the gravity
         */
        public double getGravity() { return gravity; }

        /**
         * @param gravity the gravity to set
         */
        public void setGravity(double gravity) {
            var oldGravity = this.gravity;
            this.gravity = gravity;
            if (oldGravity != gravity)
                emitter.fireChangeEvent(AgentPanel.GRAVITY_PROPERTY,
                        new ChangeSource(this, AgentPanel.GRAVITY_PROPERTY, this.gravity));
        }

        /**
         * @return the edgeLength
         */
        public int getEdgeLength() { return edgeLength; }

        /**
         * @param edgeLength the edgeLength to set
         */
        public void setEdgeLength(int edgeLength) {
            var oldEdgeLength = this.edgeLength;
            this.edgeLength = edgeLength;
            if (oldEdgeLength != edgeLength)
                emitter.fireChangeEvent(AgentPanel.EDGE_LENGTH_PROPERTY,
                        new ChangeSource(this, AgentPanel.EDGE_LENGTH_PROPERTY, this.edgeLength));
        }

        /**
         * @return the mass
         */
        public double getMass() { return mass; }

        /**
         * @param mass the mass to set
         */
        public void setMass(double mass) { this.mass = mass; }

    }

    /**
     * Options for a specific {@link Musician}
     */
    public static class MusicianOptions {
        private final ChangeEmitter emitter = new ChangeEmitter();
        private int channel;
        private final List<Integer> peerIds = new ArrayList<>();
        private boolean muted;
        private int id;
        private final RuleOptions ruleOptions = new RuleOptions();

        /**
         * @return the channel
         */
        public int getChannel() { return channel; }

        /**
         * @param channel the channel to set
         */
        public void setChannel(int channel) {
            var oldChannel = this.channel;
            this.channel = channel;
            if (oldChannel != channel)
                emitter.fireChangeEvent(Musician.CHANNEL_PROPERTY,
                        new ChangeSource(this, Musician.CHANNEL_PROPERTY, this.channel));
        }

        /**
         * @return the peerIds
         */
        public List<Integer> getPeerIds() { return peerIds; }

        /**
         * @return the muted
         */
        public boolean isMuted() { return muted; }

        /**
         * @param muted the muted to set
         */
        public void setMuted(boolean muted) {
            var oldMuted = this.muted;
            this.muted = muted;
            if (oldMuted != muted)
                emitter.fireChangeEvent(Musician.MUTED_PROPERTY,
                        new ChangeSource(this, Musician.MUTED_PROPERTY, this.muted));
        }

        /**
         * @return the id
         */
        public int getId() { return id; }

        /**
         * @param id the id to set
         */
        public void setId(int id) { this.id = id; }

        /**
         * @return the ruleOptions
         */
        public RuleOptions getRuleOptions() { return ruleOptions; }

        /**
         * @param key      key being listened to
         * @param listener listener
         */
        public void addChangeListener(String key, ChangeListener listener) {
            emitter.addChangeListener(key, listener);
        }

    }

    /**
     * Options related to a {@link MusicianRule}
     */
    public static class RuleOptions {
        private String name;
        private final Map<String, Object> ruleSpecificOptions = new LinkedHashMap<>();
        private final ChangeEmitter emitter = new ChangeEmitter();

        /**
         * @return the name
         */
        public String getName() { return name; }

        /**
         * @param name the name to set
         */
        public void setName(String name) { this.name = name; }

        /**
         * @return the ruleSpecificOptions
         */
        public Map<String, Object> getRuleSpecificOptions() { return ruleSpecificOptions; }

        /**
         * @param key      key being listened to
         * @param listener listener
         */
        public void addChangeListener(String key, ChangeListener listener) {
            this.emitter.addChangeListener(key, listener);
        }

    }
}
