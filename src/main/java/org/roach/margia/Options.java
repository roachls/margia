package org.roach.margia;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Singleton instance where all options are stored
 */
@SuppressWarnings({ "java:S3008", "java:S6548" })
public class Options {
    private Map<String, Object> opts;
    /**
     * Property fired when any option has changed
     */
    public static final String DIRTY_PROPERTY = "dirty";
    private final ChangeEmitter emitter;
    private boolean dirty;
    private Path saveDir;
    private Path filename;
    private Preferences preferences;
    private static Options INSTANCE;
    private DumperOptions dumperOptions;
    private Yaml yaml;

    private Options() {
        this.opts = new HashMap<>();
        this.emitter = new ChangeEmitter();
        preferences = Preferences.userNodeForPackage(getClass());
        this.saveDir = Path.of(preferences.get("saveDir", System.getProperty("user.home")));
        dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // Use block style for readability
        dumperOptions.setPrettyFlow(true);

        yaml = new Yaml(dumperOptions);
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
     * clear existing options
     */
    public void clear() {
        opts.clear();
    }

    /**
     * @param propertyName name of property to set
     * @param value        value of property to set
     */
    public void put(String propertyName, Object value) {
        _put(propertyName, value, opts, propertyName);
    }

    @SuppressWarnings({ "unchecked", "java:S100" })
    private void _put(String propertyName, Object value, Map<String, Object> map, String fullPropertyName) {
        var indexOfPeriod = propertyName.indexOf('.');
        if (indexOfPeriod == -1) {
            var oldValue = map.put(propertyName, value);
            if (!value.equals(oldValue)) {
                this.dirty = true;
                emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, "true"));
                emitter.fireChangeEvent(propertyName, new ChangeSource(this, fullPropertyName, value));
            }
        } else {
            var mapKey = propertyName.substring(0, indexOfPeriod);
            var cdrKey = propertyName.substring(indexOfPeriod + 1);
            var obj = map.computeIfAbsent(mapKey, _ -> new HashMap<String, Object>());
            _put(cdrKey, value, (Map<String, Object>) obj, fullPropertyName);
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
        try (Writer writer = new OutputStreamWriter(os)) {
            yaml.dump(opts, writer);
        }
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
        opts = yaml.load(is);
        for (var propEntry : opts.entrySet()) {
            emitter.fireChangeEvent(propEntry.getKey(),
                    new ChangeSource(this, propEntry.getKey(), propEntry.getValue().toString()));
        }
        this.dirty = false;
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, "false"));
    }

    /**
     * @return options
     */
    public Set<Entry<String, Object>> entrySet() {
        return opts.entrySet();
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found
     */
    public String getOrDefault(String propertyName, String defValue) {
        return _getOrDefault(propertyName, defValue, opts);
    }

    @SuppressWarnings({ "unchecked", "java:S100" })
    private String _getOrDefault(String propertyName, String defValue, Map<String, Object> map) {
        var indexOfPeriod = propertyName.indexOf('.');
        if (indexOfPeriod == -1)
            return map.getOrDefault(propertyName, defValue).toString();
        var mapKey = propertyName.substring(0, indexOfPeriod);
        var cdrKey = propertyName.substring(indexOfPeriod + 1);
        var obj = map.get(mapKey);
        if (obj instanceof Map<?, ?> submap) {
            return _getOrDefault(cdrKey, defValue, (Map<String, Object>) submap);
        }
        return defValue;

    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as a double
     */
    public double getOrDefaultAsDouble(String propertyName, double defValue) {
        var str = _getOrDefault(propertyName, Double.toString(defValue), opts);
        return Double.parseDouble(str);
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as an int
     */
    public int getOrDefaultAsInt(String propertyName, int defValue) {
        var str = _getOrDefault(propertyName, Integer.toString(defValue), opts);
        return Integer.parseInt(str);
    }

    /**
     * @param propertyName name of property
     * @param defValue     default value
     * @return the named property or the default value if not found, as a boolean
     */
    public boolean getOrDefaultAsBoolean(String propertyName, boolean defValue) {
        var str = _getOrDefault(propertyName, Boolean.toString(defValue), opts);
        return Boolean.parseBoolean(str);
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
