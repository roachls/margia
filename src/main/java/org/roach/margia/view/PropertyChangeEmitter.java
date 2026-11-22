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

}