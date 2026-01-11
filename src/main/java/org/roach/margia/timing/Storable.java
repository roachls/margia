package org.roach.margia.timing;

import java.util.Map;

/**
 * An object that can be stored to file
 */
public interface Storable {
    /**
     * @return storable properties
     */
    Map<String, Object> storableProperties();

    /**
     * Restore this object from storage
     * 
     * @param storableProperties storable properties
     */
    void restoreFromStorage(Map<String, Object> storableProperties);
}
