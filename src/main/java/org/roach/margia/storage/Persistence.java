package org.roach.margia.storage;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handle saving things to local Java {@link Preferences} so that they get
 * remembered between sessions
 */
public class Persistence {
    private final Preferences prefs = Preferences.userNodeForPackage(Options.class);
    private static final Logger LOGGER = LogManager.getLogger(Persistence.class);

    private static Persistence INSTANCE;

    private Persistence() {
        // singleton
    }

    public static Persistence getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Persistence();
        return INSTANCE;
    }

    public String getProperty(String propertyName, String defaultValue) {
        return prefs.get(propertyName, defaultValue);
    }

    public void saveProperty(String propertyName, String value) {
        prefs.put(propertyName, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            LOGGER.atWarn().log(
                    "Unable to store {} in local preferences; while this doesn't affect current operations, it will make the UI less convenient in subsequent sessions",
                    propertyName);
        }
    }
}
