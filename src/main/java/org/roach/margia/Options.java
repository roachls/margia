package org.roach.margia;

import java.io.*;
import java.util.Map.Entry;
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
    public static final String DIRTY_PROPERTY = "dirty";
    private final ChangeEmitter emitter;
    private boolean dirty;

    private Options() {
        this.opts = new Properties();
        this.emitter = new ChangeEmitter();
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
        if (!oldValue.equals(value)) {
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
    }

    /**
     * Read options from input stream
     * 
     * @param is input stream to read from
     * @throws IOException if there is an error reading from the stream
     */
    public void load(InputStream is) throws IOException {
        opts.load(is);
        System.out.println("loaded options: " + opts);
        emitter.fireChangeEvent(DIRTY_PROPERTY, new ChangeSource(this, DIRTY_PROPERTY, true));
        for (var propEntry : opts.entrySet()) {
            System.out.println("options firing " + propEntry.getKey() + "/" + propEntry.getValue());
            emitter.fireChangeEvent(propEntry.getKey().toString(),
                    new ChangeSource(this, propEntry.getKey().toString(), propEntry.getValue()));
        }
        this.dirty = false;
    }

    /**
     * @return options
     */
    public Set<Entry<Object, Object>> entrySet() {
        return opts.entrySet();
    }

    public String getOrDefault(String propertyName, String defValue) {
        return opts.getOrDefault(propertyName, defValue).toString();
    }

    public double getOrDefaultAsDouble(String propertyName, double defValue) {
        if (opts.containsKey(propertyName))
            return Double.parseDouble(opts.getProperty(propertyName));
        return defValue;
    }

    public int getOrDefaultAsInt(String propertyName, int defValue) {
        if (opts.containsKey(propertyName))
            return Integer.parseInt(opts.getProperty(propertyName));
        return defValue;
    }

    public boolean getOrDefaultAsBoolean(String propertyName, boolean defValue) {
        if (opts.containsKey(propertyName))
            return Boolean.parseBoolean(opts.getProperty(propertyName));
        return defValue;
    }

    public boolean isDirty() { return dirty; }

    public void addChangeListener(String property, ChangeListener listener) {
        emitter.addChangeListener(property, listener);
    }
}
