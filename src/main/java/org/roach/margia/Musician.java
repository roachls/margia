package org.roach.margia;

import java.beans.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.messages.MusicianMessage;
import org.roach.margia.rules.AbstractMusicianRule;
import org.roach.margia.rules.MusicianRule;
import org.roach.margia.storage.MusicianOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;
import org.roach.margia.ui.PropertyChangeEmitter;
import org.roach.margia.util.Range;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician implements PropertyChangeEmitter, PropertyChangeListener, ChangeListener {
    /**
     * property for storing the channel
     */
    public static final String CHANNEL_PROPERTY = "channel";
    /**
     * property for storing the peer IDs
     */
    public static final String PEER_IDS_PROPERTY = "peerIds";
    /**
     * property used for storing the muted field
     */
    public static final String MUTED_PROPERTY = "muted";
    /**
     * the property to fire when the last note changes
     */
    public static final String LAST_NOTE_PROPERTY = "lastNote";
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
     * maximum size the queue is allowed to reach before new notes are ignored
     */
    public static final int MAX_QUEUE_SIZE = 12;
    /**
     * property for storing the id
     */
    public static final String ID_PROPERTY = "id";
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    private final int id;
    private final MidiController controller;
    private final BlockingQueue<MusicianMessage> messageQueue = new LinkedBlockingQueue<>();
    private int channel;
    private final List<Musician> peers = new ArrayList<>();
    private AbstractMusicianRule rule;
    private int notesIvePlayed;
    private NoteInfo myLastNote;
    private final Logger logger;
    private int rangeLow = 0;
    private int rangeHi = 127;
    private boolean muted;
    private Key key = Key.Chromatic;
    private long currentTick;
    private final PropertyChangeSupport propertyChange;
    private boolean listening = true;

    /**
     * private constructor - may only be created with factory methods
     */
    private Musician(final int id) {
        this.id = id;
        Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions()).setId(id);
        this.logger = LogManager.getLogger("Musician_" + id);
        this.controller = MidiController.getInstance();
        propertyChange = new PropertyChangeSupport(this);
        Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions())
                .addChangeListener(MUTED_PROPERTY, this);
        Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions())
                .addChangeListener(CHANNEL_PROPERTY, this);
    }

    /**
     * @return a new Musician with a generated ID
     */
    public static Musician newInstance() {
        return new Musician(ID_GENERATOR.getAndIncrement());
    }

    /**
     * Repeat the last note that this musician played
     */
    public void repeatLastNote() {
        if (myLastNote != null)
            playNote(myLastNote);
    }

    /**
     * @param note the note to play
     */
    public void playNote(NoteInfo note) {
        if (note == null || note.noteNum() == Note.REST)
            return;
        if (muted) {
            logger.atDebug().log("{} is muted", id);
        } else {
            var adjustedNote = note.withNote(key.adjustToKeyByOctaves(note.noteNum()));
            logger.atDebug().log("{}: playing note {} on channel {}", id, adjustedNote, channel);
            controller.playNote(channel, adjustedNote);
        }
        propertyChange.firePropertyChange(LAST_NOTE_PROPERTY, myLastNote, note);
        myLastNote = note;
        notesIvePlayed++;
        // pass on actual note received, not note played
        sendMessageToPeers(note);
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
        peerOpts.remove(peer.getId());
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
        rule.calculateAction(tick);
    }

    /**
     * Actually perform the actions calculaated in {@link #calculateAction(long)}
     */
    public void doAction() {
        rule.doAction();
    }

    /**
     * @param message receive a note and place it on the message queue
     */
    public void receiveMessage(MusicianMessage message) {
        if (!listening && message instanceof NoteInfo)
            return;
        var offerSuccess = this.messageQueue.offer(message);
        if (offerSuccess) {
            if (message instanceof NoteInfo heardNote)
                logger.atDebug().log("{}: heard {}, queue size = {}", id, heardNote, messageQueue.size());
            else
                logger.atDebug().log("{}: received message: {}", id, message);
            if (messageQueue.size() > MAX_QUEUE_SIZE) {
                logger.atDebug().log("{}: pulling old message to make room for new", id);
                messageQueue.poll();
            }
        } else {
            logger.atWarn().log("{}: Unable to add note to queue (out of memory?)", id);
        }
    }

    /**
     * @param myLastNote the last note that this musician played
     */
    public void setMyLastNote(NoteInfo myLastNote) { this.myLastNote = myLastNote; }

    /**
     * reset the number of notes this musician has played
     */
    public void resetNotesIvePlayed() {
        this.notesIvePlayed = 0;
    }

    /**
     * @return the unique ID of this {@link Musician}
     */
    public int getId() { return id; }

    /**
     * @return true if this musician is muted
     */
    public boolean isMuted() { return muted; }

    /**
     * @param muted true to mute this musician. A muted musician won't actually play
     *              a note to the MIDI controller, but other musicians will still
     *              hear it.
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
        Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions()).setMuted(muted);
    }

    /**
     * @return current number of notes in this {@link Musician musician's} queue
     */
    public int getQueueSize() { return this.messageQueue.size(); }

    /**
     * @return the next note in this musician's queue of heard notes
     */
    public MusicianMessage getNextNoteHeard() { return messageQueue.poll(); }

    /**
     * @return the number of notes I've played since the last reset
     */
    public int getNotesIvePlayed() { return notesIvePlayed; }

    /**
     * @return the lowest note that this musician can play
     */
    public int getRangeLow() { return rangeLow; }

    /**
     * @param rangeLow the lowest note that this musician can play
     */
    public void setRangeLow(int rangeLow) {
        this.rangeLow = Range.check("rangeLow", rangeLow, 0, 127);
        this.key = this.key.of(rangeLow, rangeHi);
    }

    /**
     * @return the highest note that this musician can play
     */
    public int getRangeHi() { return rangeHi; }

    /**
     * @param rangeHi the lowest note that this musician can play
     */
    public void setRangeHi(int rangeHi) {
        this.rangeHi = Range.check("rangeHi", rangeHi, 0, 127);
        this.key = this.key.of(rangeLow, rangeHi);
    }

    /**
     * @return the key that this musician plays in
     */
    public Key getKey() { return key; }

    /**
     * @param key The key for this musician (default is {@link Key#CPentatonic})
     */
    public void setKey(Key key) {
        if (key == null)
            return;
        this.key = key;
        this.rangeLow = key.lowestNote();
        this.rangeHi = key.highestNote();
    }

    /**
     * @return the last note that this musician played
     */
    public NoteInfo getMyLastNote() { return myLastNote; }

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
    public void rest() {
        controller.playNote(channel, AbstractMusicianRule.REST.apply(1));
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
     * @param noteNum note to check
     * @return a number from 0.0 to 1.0, based on where the given noteNum falls in
     *         relation to this {@link Musician}'s range
     */
    public float noteToRange(int noteNum) {
        return key.noteToRange(noteNum);
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
    public boolean isListening() { return listening; }

    /**
     * @param listening set to false to have musician ignore incoming notes (may
     *                  still receive other types of messages
     */
    public void setListening(boolean listening) { this.listening = listening; }

    @Override
    public String toString() {
        return "Musician [id=" + id + ", channel=" + channel + ", rule=" + rule + ", muted=" + muted + ", listening="
                + listening + "]";
    }

    /**
     * @return the rule used by this musician for generating notes
     */
    public MusicianRule getRule() { return rule; }

    /**
     * @param rule the rule this musician will use for generating notes
     */
    public void setRule(AbstractMusicianRule rule) {
        if (rule == null)
            return;
        this.rule = rule;
        this.rule.setMusician(this);
        Options.getInstance().getMusicians().computeIfAbsent(id, _ -> new MusicianOptions()).getRuleOptions()
                .setName(rule.getName());
    }

    /**
     * @return the MIDI channel this musician will send notes to
     */
    public int getChannel() { return this.channel; }

    /**
     * @param channel the MIDI channel this musician will send notes to
     */
    public void setChannel(int channel) {
        this.channel = Range.check(CHANNEL_PROPERTY, channel, 0, 16);
        Options.getInstance().getMusicians().get(id).setChannel(channel);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Transport.RESET_PROPERTY.equals(evt.getPropertyName())) {
            messageQueue.clear();
            myLastNote = null;
            currentTick = 0;
            notesIvePlayed = 0;
            listening = true;
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
        Musician m = new Musician(props.getId());
        m.setChannel(props.getChannel());
        m.setMuted(props.isMuted());
        var ruleOpts = props.getRuleOptions();
        var ruleName = ruleOpts.getName();
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
        return m;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource cs) {
            switch (cs.key()) {
            case MUTED_PROPERTY:
                this.muted = (boolean) cs.newValue();
                break;
            case CHANNEL_PROPERTY:
                this.channel = (int) cs.newValue();
                break;
            default:
                break;
            }
        }
    }

}
