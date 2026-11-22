package org.roach.margia.storage;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle saving things to local Java {@link Preferences} so that they get
 * remembered between sessions
 */
@SuppressWarnings("java:S6548")
public class Persistence {
    private final Preferences prefs = Preferences.userNodeForPackage(Options.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(Persistence.class);

    private static Persistence instance;

    private Persistence() {
        // singleton
    }

    /**
     * @return the singleton instance of {@link Persistence}
     */
    public static Persistence getInstance() {
        if (instance == null)
            instance = new Persistence();
        return instance;
    }

    /**
     * @param propertyName name of property to retrieve
     * @param defaultValue default value to return if not found
     * @return the value associated with the given property, or {@code defaultValue}
     *         if not found
     */
    public String getString(String propertyName, String defaultValue) {
        return prefs.get(propertyName, defaultValue);
    }

    /**
     * @param propertyName name of property to retrieve
     * @param defaultValue default value to return if not found
     * @return the value associated with the given property, or {@code defaultValue}
     *         if not found
     */
    public int getInt(String propertyName, int defaultValue) {
        return prefs.getInt(propertyName, defaultValue);
    }

    /**
     * @param propertyName name of property to retrieve
     * @param defaultValue default value to return if not found
     * @return the value associated with the given property, or {@code defaultValue}
     *         if not found
     */
    public boolean getBoolean(String propertyName, boolean defaultValue) {
        return prefs.getBoolean(propertyName, defaultValue);
    }

    /**
     * @param propertyName name of property to save
     * @param value        value to save
     */
    public void saveProperty(String propertyName, String value) {
        prefs.put(propertyName, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            LOGGER.atWarn().setMessage(
                    "Unable to store {} in local preferences; while this doesn't affect current operations, it will make the UI less convenient in subsequent sessions")
                    .addArgument(propertyName).log();
            LOGGER.atDebug().setCause(e).log();
        }
    }
}
