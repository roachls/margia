package org.roach.margia.ui;

import java.util.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A generic (non-swing) emitter of {@link ChangeEvent}s
 */
public class ChangeEmitter {
    private final Map<String, List<ChangeListener>> listeners = new HashMap<>();

    /**
     * @param key      key of property being subscribed to
     * @param listener listener
     */
    public void addChangeListener(String key, ChangeListener listener) {
        listeners.putIfAbsent(key, new ArrayList<>());
        listeners.get(key).add(listener);
    }

    /**
     * @param key    key of property
     * @param source source of property
     */
    public void fireChangeEvent(String key, ChangeSource source) {
        if (listeners.containsKey(key)) {
            var listenerList = listeners.get(key);
            var event = new ChangeEvent(source);
            for (var listener : listenerList) {
                listener.stateChanged(event);
            }
        }
    }

    /**
     * A change source
     * 
     * @param source   source of change
     * @param key      key of property that changed
     * @param newValue new value of property
     */
    public static record ChangeSource(Object source, String key, String newValue) {
        // nothing
    }
}
