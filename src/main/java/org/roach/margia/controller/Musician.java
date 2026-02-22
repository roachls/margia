package org.roach.margia.controller;

import java.beans.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.controller.rules.*;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter.ChangeSource;
import org.roach.margia.view.PropertyChangeEmitter;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician implements PropertyChangeEmitter, PropertyChangeListener, ChangeListener {
    /**
     * the property to fire when the last note changes
     */
    public static final String LAST_CHORD_PROPERTY = "lastNote";
    /**
     * Minimum velocity a note may be played at
     */
    public static final int MIN_VELOCITY = 0;
    /**
     * Maximum velocity a note may be played at
     */
    public static final int MAX_VELOCITY = 127;
    /**
     * Medium velocity starting point
     */
    public static final int START_VELOCITY = 64;
    /**
     * a rest of 1 tick
     */
    public static final Chord REST = new Chord(Collections.emptySet(), 1, 0);
    private int id;
    private final MidiController controller;
    private final BlockingQueue<MusicianMessage> messageQueue = new LinkedBlockingQueue<>();
    private final List<Musician> peers = new ArrayList<>();
    private AbstractMusicianRule rule;
    private int chordsIvePlayed;
    private Chord myLastChord;
    private final Logger logger = LogManager.getLogger(getClass());
    private long currentTick;
    private final PropertyChangeSupport propertyChange;
    private MusicianOptions musicianOptions;

    /**
     * private constructor - may only be created with factory methods
     */
    private Musician(final int id) {
        this();
        this.id = id;
        musicianOptions = Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions());
        musicianOptions.setId(id);
        musicianOptions.getRuleOptions().addChangeListener(RuleOptions.RULE_NAME_PROPERTY, this);
    }

    private Musician(final MusicianOptions options) {
        this();
        this.musicianOptions = options;
        Options.getInstance().getMusicians().put(options.getId(), options);
        this.id = options.getId();
        musicianOptions.getRuleOptions().addChangeListener(RuleOptions.RULE_NAME_PROPERTY, this);
    }

    private Musician() {
        propertyChange = new PropertyChangeSupport(this);
        this.controller = MidiController.getInstance();

    }

    /**
     * @return a new Musician with a generated ID
     */
    public static Musician newInstance() {
        return new Musician(MusicianOptions.ID_GENERATOR.getAndIncrement());
    }

    /**
     * Repeat the last note that this musician played
     */
    public void repeatLastNote() {
        if (myLastChord != null)
            playChord(myLastChord);
    }

    /**
     * @param chord the {@link Chord} to play
     */
    public void playChord(Chord chord) {
        var adjustedNotes = new ArrayList<Integer>();
        for (var note : chord.getNotes()) {
            if (note == null || note == Note.REST)
                continue;
            if (musicianOptions.isMuted()) {
                logger.atDebug().log("{} is muted", id);
            } else {
                var range = musicianOptions.getRange();
                var adjustedNote = range.adjustToRangeByOctaves(note);
                logger.atDebug().log("{}: playing note {} on channel {}, duration {}", id, adjustedNote,
                        musicianOptions.getChannel(), chord.getLength());
                adjustedNotes.add(adjustedNote);
            }
        }
        if (id == 13) {
            System.out.println(chord);
        }
        controller.playChord(musicianOptions.getBusName(), musicianOptions.getChannel(),
                chord.withNotes(adjustedNotes));
        propertyChange.firePropertyChange(LAST_CHORD_PROPERTY, myLastChord, chord);
        myLastChord = chord;
        chordsIvePlayed++;
        // pass on actual note received, not note played
        sendMessageToPeers(chord);
    }

    /**
     * @param peer another {@link Musician} with which this one may communicate
     */
    public void addPeer(Musician peer) {
        if (!peers.contains(peer)) {
            peers.add(peer);
            var peerOpts = Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions())
                    .getPeerIds();
            if (!peerOpts.contains(peer.getId()))
                peerOpts.add(peer.getId());
        }
    }

    /**
     * @param peer peer to remove
     * @return true if peer was removed
     */
    public boolean removePeer(Musician peer) {
        var successful = peers.remove(peer);
        var peerOpts = Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions())
                .getPeerIds();
        // must cast to Integer or the overloaded remove-by-index method will be called
        peerOpts.remove((Integer) peer.getId());
        return successful;
    }

    /**
     * Calculates actions to be performed after receiving a tick from the
     * {@link Transport}
     * 
     * @param tick the tick number
     */
    public void calculateAction(long tick) {
        this.currentTick = tick;
        logger.atDebug().log("{}: tick={} calculateAction", id, tick);
        if (rule != null)
            rule.calculateAction(tick);
        else
            logger.atTrace().log("{} rule is null", id);
    }

    /**
     * Actually perform the actions calculated in {@link #calculateAction(long)}
     */
    public void doAction() {
        if (rule != null)
            rule.doAction();
    }

    /**
     * @param message receive a note and place it on the message queue
     */
    public void receiveMessage(MusicianMessage message) {
        if (!musicianOptions.isListening() && message instanceof Chord)
            return;
        var offerSuccess = this.messageQueue.offer(message);
        if (offerSuccess) {
            if (message instanceof Chord heardChord)
                logger.atDebug().log("{}: heard {}, queue size = {}", id, heardChord, messageQueue.size());
            else
                logger.atDebug().log("{}: received message: {}", id, message);
            if (messageQueue.size() > Options.getInstance().getMusicOptions().getMaxQueueSize()) {
                logger.atDebug().log("{}: pulling old message to make room for new", id);
                messageQueue.poll();
            }
        } else {
            logger.atWarn().log("{}: Unable to add note to queue (out of memory?)", id);
        }
    }

    /**
     * @param myLastChord override the last chord that this musician played
     */
    public void setMyLastChord(Chord myLastChord) { this.myLastChord = myLastChord; }

    /**
     * reset the number of notes this musician has played
     */
    public void resetChordsIvePlayed() {
        this.chordsIvePlayed = 0;
    }

    /**
     * @return the unique ID of this {@link Musician}
     */
    public int getId() { return id; }

    /**
     * @return true if this musician is muted
     */
    public boolean isMuted() { return musicianOptions.isMuted(); }

    /**
     * @return current number of notes in this {@link Musician musician's} queue
     */
    public int getQueueSize() { return this.messageQueue.size(); }

    /**
     * @return the next note in this musician's queue of heard notes
     */
    public MusicianMessage getNextMessageReceived() { return messageQueue.poll(); }

    /**
     * @return the number of notes I've played since the last reset
     */
    public int getChordsIvePlayed() { return chordsIvePlayed; }

    /**
     * @return the lowest note that this musician can play
     */
    public int getRangeLow() { return musicianOptions.getRange().low(); }

    /**
     * @return the highest note that this musician can play
     */
    public int getRangeHi() { return musicianOptions.getRange().high(); }

    /**
     * @return this {@link Musician}'s range
     */
    public NoteRange getRange() { return musicianOptions.getRange(); }

    /**
     * @return the key that this musician plays in
     */
    public Key getKey() { return Key.BUILTIN_KEYS.get(musicianOptions.getKeyName()); }

    /**
     * @return the last chord that this musician played
     */
    public Chord getMyLastChord() { return myLastChord; }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Musician other = (Musician) obj;
        return Objects.equals(id, other.id);
    }

    /**
     * Rest for one 16th
     */
    @SuppressWarnings("java:S1845")
    public void rest() {
        playChord(REST);
    }

    /**
     * @return this {@link Musician musician's logger}
     */
    public Logger getLogger() { return logger; }

    /**
     * @return the current tick number
     */
    public long getCurrentTick() { return currentTick; }

    /**
     * Pass the given message along to all peers
     * 
     * @param message the message to send
     */
    public void sendMessageToPeers(MusicianMessage message) {
        for (var peer : peers) {
            peer.receiveMessage(message);
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChange.addPropertyChangeListener(listener);
    }

    /**
     * @return a list of IDs of all peers
     */
    public List<Integer> peerIds() {
        return peers.stream().map(Musician::getId).toList();
    }

    /**
     * @return true if the musician is listening to notes (may still receive other
     *         types of messages)
     */
    public boolean isListening() { return musicianOptions.isListening(); }

    @Override
    public String toString() {
        return "Musician [id=" + id + ", options=" + musicianOptions;
    }

    /**
     * @return the rule used by this musician for generating notes
     */
    public MusicianRule getRule() { return rule; }

    /**
     * @param rule the rule this musician will use for generating notes
     */
    private void setRule(AbstractMusicianRule rule) {
        if (rule == null)
            return;
        this.rule = rule;
        this.rule.setMusician(this);
        rule.initActionsAfterMusicianAssigned();
    }

    /**
     * @return the MIDI channel this musician will send notes to
     */
    public int getChannel() { return musicianOptions.getChannel(); }

    /**
     * @param channel the MIDI channel this musician will send notes to
     */
    public void setChannel(int channel) {
        musicianOptions.setChannel(channel);
    }

    /**
     * @return this {@link Musician}'s {@link MusicianOptions}
     */
    public MusicianOptions getOptions() { return musicianOptions; }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Transport.RESET_PROPERTY.equals(evt.getPropertyName())) {
            messageQueue.clear();
            myLastChord = null;
            currentTick = 0;
            chordsIvePlayed = 0;
            musicianOptions.setListening(true);
            rule.reset();
        }
    }

    /**
     * Create a musician from saved properties
     * 
     * @param props properties loaded from save file
     * @return a new {@link Musician} with the given properties
     */
    public static Musician restoreFromStorage(MusicianOptions props) {
        Musician m = new Musician(props);
        var ruleOpts = props.getRuleOptions();
        var ruleName = ruleOpts.getName();
        findRuleFromName(m, ruleOpts, ruleName);
        return m;
    }

    /**
     * assigns the rule given by a {@link RuleOptions}
     * 
     * @param m        musician to assign rule
     * @param ruleOpts rule options
     * @param ruleName name of rule
     */
    private static void findRuleFromName(Musician m, RuleOptions ruleOpts, String ruleName) {
        if (ruleName != null) {
            var availableRules = ServiceLoader.load(MusicianRule.class);
            for (var availableRule : availableRules) {
                if (ruleName.equals(availableRule.getName())) {
                    var realRule = ((AbstractMusicianRule) availableRule).copy();
                    realRule.restoreFromStorage(ruleOpts);
                    m.setRule(realRule);
                    break;
                }
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String propertyName, Object newValue)
                && RuleOptions.RULE_NAME_PROPERTY.equals(propertyName)) {
            var ruleName = (String) newValue;
            findRuleFromName(this, this.musicianOptions.getRuleOptions(), ruleName);
        }

    }

}
