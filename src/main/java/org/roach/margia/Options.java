package org.roach.margia;

import java.io.*;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.Properties;
import java.util.Set;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * Singleton instance where all options are stored
 */
@SuppressWarnings({ "java:S3008", "java:S6548" })
public class Options {
    private Properties opts;
    /**
     * Property fired when any option has changed
     */
    public static final String DIRTY_PROPERTY = "dirty";
    private final ChangeEmitter emitter;
    private boolean dirty;
    private Path saveDir;
    private Path filename;
    private Preferences preferences;

    private Options() {
        this.opts = new Properties();
        this.emitter = new ChangeEmitter();
        preferences = Preferences.userNodeForPackage(getClass());
        this.saveDir = Path.of(preferences.get("saveDir", System.getProperty("user.home")));
    }

    private static Options INSTANCE;

    /**
     * @return the single instance of {@link Options}
     */
    public static Options getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Options();
        return INSTANCE;
    }

    /**
     * clear existing options
     */
    public void clear() {
        opts.clear();
    }

    /**
     * @param propertyName name of property to set
     * @param value        value of property to set
     */
    public void put(String propertyName, String value) {
        var oldValue = opts.put(propertyName, value);
        if (!value.equals(oldValue)) {
            this.dirty = true;
            emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, true));
            emitter.fireChangeEvent(propertyName, new ChangeSource(this, propertyName, value));
        }
    }

    /**
     * Store current options to output stream
     * 
     * @param os       output stream to write to
     * @param comments comments for file
     * @throws IOException if there is an error writing to a file
     */
    public void store(OutputStream os, String comments) throws IOException {
        opts.store(os, comments);
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, false));
    }

    /**
     * Read options from input stream
     * 
     * @param is input stream to read from
     * @throws IOException if there is an error reading from the stream
     */
    public void load(InputStream is) throws IOException {
        opts.load(is);
        for (var propEntry : opts.entrySet()) {
            emitter.fireChangeEvent(propEntry.getKey().toString(),
                    new ChangeSource(this, propEntry.getKey().toString(), propEntry.getValue()));
        }
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, false));
    }

    /**
     * @return options
     */
    public Set<Entry<Object, Object>> entrySet() {
        return opts.entrySet();
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found
     */
    public String getOrDefault(String propertyName, String defValue) {
        return opts.getOrDefault(propertyName, defValue).toString();
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as a double
     */
    public double getOrDefaultAsDouble(String propertyName, double defValue) {
        if (opts.containsKey(propertyName))
            return Double.parseDouble(opts.getProperty(propertyName));
        return defValue;
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as an int
     */
    public int getOrDefaultAsInt(String propertyName, int defValue) {
        if (opts.containsKey(propertyName))
            return Integer.parseInt(opts.getProperty(propertyName));
        return defValue;
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as a boolean
     */
    public boolean getOrDefaultAsBoolean(String propertyName, boolean defValue) {
        if (opts.containsKey(propertyName))
            return Boolean.parseBoolean(opts.getProperty(propertyName));
        return defValue;
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
}
