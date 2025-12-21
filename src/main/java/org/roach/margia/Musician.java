package org.roach.margia;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.messages.MusicianMessage;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician {
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
	private final BlockingQueue<NoteInfo> messageQueue = new LinkedBlockingQueue<>();
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

	/**
	 * @return true if this musician is muted
	 */
	public boolean isMuted() {
		return muted;
	}

	/**
	 * @param muted true to mute this musician. A muted musician won't actually play
	 *              a note to the MIDI controller, but other musicians will still
	 *              hear it.
	 */
	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	/**
	 * @param id         unique id of this {@link Musician}
	 * @param controller MIDI controller that will actually play the notes
	 * @param tempo      tempo to play at
	 * @param channel    MIDI channel
	 * @param rule       The rule that governs a musician's behavior
	 */
	public Musician(final int id, final MidiController controller, int tempo, int channel, final MusicianRule rule) {
		this.id = id;
		this.logger = LogManager.getLogger("Musician_" + id);
		this.controller = controller;
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
		if (note == null)
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
	 * 
	 * @param tick tick number
	 */
	public void doAction(long tick) {
		rule.doAction(tick);
	}

	/**
	 * @param message receive a note and place it on the message queue
	 */
	public void receiveMessage(MusicianMessage message) {
		if (message instanceof NoteInfo heardNote) {
			this.messageQueue.offer(heardNote);
			logger.atDebug().log("{}: heard {}, queue size = {}", id, heardNote, messageQueue.size());
			if (messageQueue.size() > MAX_QUEUE_SIZE) {
				logger.atDebug().log("{}: pulling old message to make room for new", id);
				messageQueue.poll();
			}
		}
	}

	/**
	 * @param myLastNote the last note that this musician played
	 */
	public void setMyLastNote(NoteInfo myLastNote) {
		this.myLastNote = myLastNote;
	}

	/**
	 * @return the unique ID of this {@link Musician}
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return current number of notes in this {@link Musician musician's} queue
	 */
	public int getQueueSize() {
		return this.messageQueue.size();
	}

	/**
	 * @return the next note in this musician's queue of heard notes
	 */
	public NoteInfo getNextNoteHeard() {
		return messageQueue.poll();
	}

	/**
	 * @return the number of notes I've played since the last reset
	 */
	public int getNotesIvePlayed() {
		return notesIvePlayed;
	}

	/**
	 * @param rangeLow the lowest note that this musician can (default is 0)
	 * @return the musician
	 */
//	public Musician setRangeLow(int rangeLow) {
//		if (rangeLow < 0 || rangeLow > 127)
//			throw new IllegalArgumentException("range low must be between 0 and 127");
//		this.rangeLow = rangeLow;
//		return this;
//	}

	/**
	 * @param rangeHi the highest note that this musician can play (default is 127)
	 * @return the musician
	 */
//	public Musician setRangeHi(int rangeHi) {
//		if (rangeHi < 0 || rangeHi > 127)
//			throw new IllegalArgumentException("range high must be between 0 and 127");
//		this.rangeHi = rangeHi;
//		return this;
//	}

	/**
	 * @return the lowest note that this musician can play
	 */
	public int getRangeLow() {
		return rangeLow;
	}

	/**
	 * @return the highest note that this musician can play
	 */
	public int getRangeHi() {
		return rangeHi;
	}

	/**
	 * @return the key that this musician plays in
	 */
	public Key getKey() {
		return key;
	}

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
	public NoteInfo getMyLastNote() {
		return myLastNote;
	}

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
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * @return the current tick number
	 */
	public long getCurrentTick() {
		return currentTick;
	}

	private void sendMessageToPeers(MusicianMessage message) {
		for (var peer : peers) {
			peer.receiveMessage(message);
		}
	}

	/**
	 * @param listener a listener for property changes
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChange.addPropertyChangeListener(listener);
	}

	public float noteToRange(int noteNum) {
		return key.noteToRange(noteNum);
	}
}
