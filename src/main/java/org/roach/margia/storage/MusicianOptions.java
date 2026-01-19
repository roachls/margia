package org.roach.margia.storage;

import java.util.ArrayList;
import java.util.List;

import org.roach.margia.Key;
import org.roach.margia.Musician;
import org.roach.margia.util.Range;

/**
 * Options for a specific {@link Musician}
 */
public class MusicianOptions {
    private int channel;
    private final List<Integer> peerIds = new ArrayList<>();
    private boolean muted;
    private boolean listening;
    private int id;
    private final RuleOptions ruleOptions = new RuleOptions();
    private String keyName = Key.Chromatic.getName();

    /**
     * @return the channel
     */
    public int getChannel() { return channel; }

    /**
     * @param channel the channel to set
     */
    public void setChannel(int channel) { this.channel = Range.check("MIDI channel", channel, 0, 16); }

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
        if (oldMuted != this.muted)
            Options.getInstance().setDirty();
    }

    /**
     * @return the listening
     */
    public boolean isListening() { return listening; }

    /**
     * @param listening the listening to set
     */
    public void setListening(boolean listening) {
        var oldListening = this.listening;
        this.listening = listening;
        if (oldListening != this.listening)
            Options.getInstance().setDirty();
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
     * @return the keyName
     */
    public String getKeyName() { return keyName; }

    /**
     * @param keyName the keyName to set
     */
    public void setKeyName(String keyName) {
        var oldKeyName = this.keyName;
        this.keyName = keyName;
        if (oldKeyName != null && !oldKeyName.equals(this.keyName))
            Options.getInstance().setDirty();
    }

    @Override
    public String toString() {
        return "MusicianOptions [channel=" + channel + ", peerIds=" + peerIds + ", muted=" + muted + ", id=" + id
                + ", ruleOptions=" + ruleOptions + ", keyName=" + keyName + "]";
    }

}