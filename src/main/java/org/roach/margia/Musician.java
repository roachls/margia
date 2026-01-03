package org.roach.margia;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.messages.MusicianMessage;
import org.roach.margia.ui.PropertyChangeEmitter;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician implements PropertyChangeEmitter {
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
    private final int id;
    private final MidiController controller;
    private final BlockingQueue<MusicianMessage> messageQueue = new LinkedBlockingQueue<>();
    private final int channel;
    private final List<Musician> peers = new ArrayList<>();
    private final MusicianRule rule;
    private int notesIvePlayed;
    private NoteInfo myLastNote;
    private final Logger logger;
    private int rangeLow = 0;
    private int rangeHi = 127;
    private boolean muted;
    private Key key = Key.CPentatonic;
    private long currentTick;
    private final PropertyChangeSupport propertyChange;
    private boolean listening = true;

    /**
     * @return true if this musician is muted
     */
    public boolean isMuted() { return muted; }

    /**
     * @param muted true to mute this musician. A muted musician won't actually play
     *              a note to the MIDI controller, but other musicians will still
     *              hear it.
     */
    public void setMuted(boolean muted) { this.muted = muted; }

    /**
     * @param id      unique id of this {@link Musician}
     * @param channel MIDI channel
     * @param rule    The rule that governs a musician's behavior
     */
    public Musician(final int id, int channel, final MusicianRule rule) {
        this.id = id;
        this.logger = LogManager.getLogger("Musician_" + id);
        this.controller = MidiController.getInstance();
        this.channel = channel;
        this.rule = rule;
        this.rule.setMusician(this);
        propertyChange = new PropertyChangeSupport(this);
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
        if (!peers.contains(peer))
            peers.add(peer);
    }

    /**
     * @param musician peer to remove
     * @return true if peer was removed
     */
    public boolean removePeer(Musician musician) {
        return peers.remove(musician);
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
     * @return the unique ID of this {@link Musician}
     */
    public int getId() { return id; }

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
     * @return the highest note that this musician can play
     */
    public int getRangeHi() { return rangeHi; }

    /**
     * @return the key that this musician plays in
     */
    public Key getKey() { return key; }

    /**
     * @param key The key for this musician (default is {@link Key#CPentatonic})
     */
    public void setKey(Key key) {
        this.key = key;
        this.rangeLow = key.lowestNote();
        this.rangeHi = key.highestNote();
    }

    /**
     * reset the number of notes this musician has played
     */
    public void resetNotesIvePlayed() {
        this.notesIvePlayed = 0;
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
        controller.playNote(channel, MusicianRule.REST.apply(1));
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
     * Toggle the muted status of this musician
     */
    public void toggleMuted() {
        muted = !muted;
    }
}
