package org.roach.margia.storage;

import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * Options related to a {@link MusicianRule}
 */
public class KeyOptions {
    private String name;
    private String basis;
    private final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * @return the name
     */
    public String getName() { return name; }

    /**
     * @param name the name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * @return the intervalic basis
     */
    public String getBasis() { return basis; }

    /**
     * @param basis the basis to set
     */
    public void setBasis(String basis) {
        var oldBasis = this.basis;
        this.basis = basis;
        if (oldBasis != null && !oldBasis.equals(basis))
            emitter.fireChangeEvent(Musician.KEY_PROPERTY, new ChangeSource(this, Musician.KEY_PROPERTY, this));
    }

    /**
     * @param key      key being listened to
     * @param listener listener
     */
    public void addChangeListener(String key, ChangeListener listener) {
        this.emitter.addChangeListener(key, listener);
    }

}