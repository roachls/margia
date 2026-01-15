package org.roach.margia.storage;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * Options for a specific {@link Musician}
 */
public class MusicianOptions {
    private final ChangeEmitter emitter = new ChangeEmitter();
    private int channel;
    private final List<Integer> peerIds = new ArrayList<>();
    private boolean muted;
    private int id;
    private final RuleOptions ruleOptions = new RuleOptions();
    private KeyOptions keyOptions = new KeyOptions();

    /**
     * @return the channel
     */
    public int getChannel() { return channel; }

    /**
     * @param channel the channel to set
     */
    public void setChannel(int channel) {
        var oldChannel = this.channel;
        this.channel = channel;
        if (oldChannel != channel)
            emitter.fireChangeEvent(Musician.CHANNEL_PROPERTY,
                    new ChangeSource(this, Musician.CHANNEL_PROPERTY, this.channel));
    }

    /**
     * @return the peerIds
     */
    public List<Integer> getPeerIds() { return peerIds; }

    /**
     * @return the muted
     */
    public boolean isMuted() { return muted; }

    /**
     * @param muted the muted to set
     */
    public void setMuted(boolean muted) {
        var oldMuted = this.muted;
        this.muted = muted;
        if (oldMuted != muted)
            emitter.fireChangeEvent(Musician.MUTED_PROPERTY,
                    new ChangeSource(this, Musician.MUTED_PROPERTY, this.muted));
    }

    /**
     * @return the id
     */
    public int getId() { return id; }

    /**
     * @param id the id to set
     */
    public void setId(int id) { this.id = id; }

    /**
     * @return the ruleOptions
     */
    public RuleOptions getRuleOptions() { return ruleOptions; }

    /**
     * @param key      key being listened to
     * @param listener listener
     */
    public void addChangeListener(String key, ChangeListener listener) {
        emitter.addChangeListener(key, listener);
    }

    /**
     * @return the keyOptions
     */
    public KeyOptions getKeyOptions() { return keyOptions; }

    /**
     * @param myKeyOpts the {@link KeyOptions} to set
     */
    public void setKeyOptions(KeyOptions myKeyOpts) {
        this.keyOptions = myKeyOpts;
    }

}