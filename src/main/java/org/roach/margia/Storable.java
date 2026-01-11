package org.roach.margia;

/**
 * An object that can be stored to file
 * 
 */
public interface Storable {
    /**
     * @return storable properties
     */
    Object storableProperties();

    /**
     * Restore this object from storage
     * 
     * @param storableProperties storable properties
     */
    void restoreFromStorage(Object storableProperties);
}
