package org.roach.margia.view;

import java.beans.PropertyChangeListener;

/**
 * something that emits {@link java.beans.PropertyChangeEvent}s
 */
public interface PropertyChangeEmitter {

    /**
     * @param listener a listener for property changes
     */
    void addPropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * @param listener a listener to be removed
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

}