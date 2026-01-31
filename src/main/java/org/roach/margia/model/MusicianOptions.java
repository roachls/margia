package org.roach.margia.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.roach.margia.controller.Musician;
import org.roach.margia.storage.Options;
import org.roach.margia.util.RangeCheck;

/**
 * Options for a specific {@link Musician}
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class MusicianOptions {
    @Override
    public int hashCode() {
        return Objects.hash(busName, channel, id, keyName, listening, muted, peerIds, range, ruleOptions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MusicianOptions other = (MusicianOptions) obj;
        return Objects.equals(busName, other.busName) && channel == other.channel && id == other.id
                && Objects.equals(keyName, other.keyName) && listening == other.listening && muted == other.muted
                && Objects.equals(peerIds, other.peerIds) && Objects.equals(range, other.range)
                && Objects.equals(ruleOptions, other.ruleOptions);
    }

    private String busName = ALL_BUSSES;
    private int channel;
    private final List<Integer> peerIds = new ArrayList<>();
    private boolean muted;
    private boolean listening = true;
    private int id;
    private RuleOptions ruleOptions = new RuleOptions();
    private String keyName = Key.Chromatic.getName();
    private NoteRange range = new NoteRange(48, 92);
    /**
     * property for storing the peer IDs
     */
    public static final String PEER_IDS_PROPERTY = "peerIds";
    /**
     * {@link AtomicInteger} that is used to generate the ID of the next musician
     */
    public static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    /**
     * special value of busName to indicate all/any available busses
     */
    public static final String ALL_BUSSES = "All available outputs";

    /**
     * @return the busName
     */
    public String getBusName() { return busName; }

    /**
     * @param busName the busName to set
     */
    public void setBusName(String busName) {
        if (busName == null)
            return;
        var oldBusName = this.busName;
        this.busName = busName;
        if (oldBusName != null && !oldBusName.equals(this.busName))
            Options.getInstance().setDirty();
    }

    /**
     * @return the channel
     */
    public int getChannel() { return channel; }

    /**
     * @param channel the channel to set
     */
    public void setChannel(int channel) {
        var oldChannel = this.channel;
        this.channel = RangeCheck.check("MIDI channel", channel, 0, 16);
        if (oldChannel != this.channel)
            Options.getInstance().setDirty();
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

    /**
     * @return the range
     */
    public NoteRange getRange() { return range; }

    /**
     * @param range the range to set
     */
    public void setRange(NoteRange range) { this.range = range; }

    @Override
    public String toString() {
        return "MusicianOptions [id=" + id + ", busName=" + busName + ", channel=" + channel + ", peerIds=" + peerIds
                + ", muted=" + muted + ", listening=" + listening + ", ruleOptions=" + ruleOptions + ", keyName="
                + keyName + ", range=" + range + "]";
    }

    /**
     * Make a copy of these options but with a different ID. PeerIds are
     * intentionally not copied.
     * 
     * @param newId The ID of the copy
     * @return a new {@link MusicianOptions} instance that is a copy of this one,
     *         <i>except</i> that the ID will be the given ID
     */
    public MusicianOptions copy(int newId) {
        var copy = new MusicianOptions();
        copy.channel = channel;
        copy.muted = muted;
        copy.listening = listening;
        copy.ruleOptions = ruleOptions.copy();
        copy.keyName = keyName;
        copy.range = range.copy();
        copy.id = newId;
        copy.busName = busName;
        return copy;
    }

    /**
     * copies the settings (except ID and peerIds) of this {@link MusicianOptions}
     * into {@code target}
     * 
     * @param target target options
     */
    public void copyInto(MusicianOptions target) {
        target.setChannel(this.channel);
        target.setKeyName(this.keyName);
        target.setListening(this.listening);
        target.setMuted(this.muted);
        target.setRange(this.range.copy());
        this.ruleOptions.copyInto(target.ruleOptions);
    }

}