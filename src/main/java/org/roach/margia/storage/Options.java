package org.roach.margia.storage;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeListener;

import org.roach.margia.*;
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
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(DIRTY_PROPERTY, false));
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
        } catch (MismatchedInputException e) {
            System.err.println("Parameter file was empty or malformed");
            e.printStackTrace();
            return;
        }
        if (loadedOpts != null) {
            this.storedOptions = loadedOpts;
        }
        MidiController.getInstance().setMidiDevice(storedOptions.getMidiOptions().isUseExternalMidi());
        restoreMusicians();
        MusicianList.getInstance().getMusicians().keySet().stream().max(Integer::compare)
                .ifPresent(maxId -> Musician.ID_GENERATOR.set(maxId + 1));
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(DIRTY_PROPERTY, true));
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
        storedOptions.getMidiOptions().addChangeListener(property, listener);
        storedOptions.getUiOptions().emitter.addChangeListener(property, listener);
        storedOptions.getMusicOptions().addChangeListener(property, listener);
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
        MusicianList.getInstance().restoreFromStorage(storedOptions.getMusicians());
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
     * @return random seed
     */
    public long getRandomSeed() { return storedOptions.getRandomSeed(); }
}
