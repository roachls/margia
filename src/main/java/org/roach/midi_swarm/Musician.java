package org.roach.midi_swarm;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.midi_swarm.messages.*;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician {
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
	private final int id;
	private final MidiController controller;
	private final BlockingQueue<NoteInfo> messageQueue = new LinkedBlockingQueue<>();
	private final Key key;
	private final int channel;
	private final List<Musician> peers = new ArrayList<>();
	private long lastTickIPlayedANote;
	private final MusicianRule rule;
	private int notesIvePlayed;
	private NoteInfo myLastNote;
	private final Logger logger;
	private Transport transport;
	private int numNotesRemainingInSequence;
	private NoteSequence sequence;
	private final List<NoteInfo> notesToAddToSequence = new LinkedList<>();
	private long repeatSequenceAtTick;

	/**
	 * @param id         unique id of this {@link Musician}
	 * @param controller MIDI controller that will actually play the notes
	 * @param key        key to use for generating notes
	 * @param tempo      tempo to play at
	 * @param channel    MIDI channel
	 * @param rule       The rule that governs a musician's behavior
	 */
	public Musician(final int id, final MidiController controller, Key key, int tempo, int channel,
			final MusicianRule rule) {
		this.id = id;
		this.logger = LogManager.getLogger("Musician_" + id);
		this.controller = controller;
		this.key = key;
		this.channel = channel;
		this.rule = rule;
		this.rule.setMusician(this);
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
		logger.atDebug().log("{}: playing note {} on channel {}", id, note, channel);
		controller.playNote(channel, note);
		myLastNote = note;
		notesIvePlayed++;
		sendMessageToPeers(new HeardNoteInfo(transport.getTick(), note));
		this.lastTickIPlayedANote = transport.getTick();
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
		logger.atDebug().log("{}: tick={}, lastTickIPlayedANote={}", id, tick, lastTickIPlayedANote);
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
		if (message instanceof HeardNoteInfo heardNote) {
			if (isReceivingSequence()) {
				notesToAddToSequence.add(heardNote.noteInfo());
				numNotesRemainingInSequence--;
				if (numNotesRemainingInSequence == 0) {
					this.sequence = new NoteSequence(new ArrayList<>(notesToAddToSequence));
				}
			} else {
				this.messageQueue.offer(heardNote.noteInfo());
			}
		}
		if (message instanceof StartSequence startSequence) {
			this.numNotesRemainingInSequence = startSequence.numNotesInSequence();
		}
	}

	/**
	 * @param myLastNote the last note that this musician played
	 */
	public void setMyLastNote(NoteInfo myLastNote) {
		this.myLastNote = myLastNote;
	}

	/**
	 * @param transport the transport that controls timing
	 */
	public void setTransport(Transport transport) {
		this.transport = transport;
	}
	
	public Transport getTransport() {
		return this.transport;
	}

	/**
	 * @return the unique ID of this {@link Musician}
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the key in which this {@link Musician} is playing
	 */
	public Key getKey() {
		return key;
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
	 * @return the tick number of the last time this musician played a note
	 */
	public long getLastTickIPlayedANote() {
		return lastTickIPlayedANote;
	}

	/**
	 * @return the number of notes I've played since the last reset
	 */
	public int getNotesIvePlayed() {
		return notesIvePlayed;
	}

	/**
	 * reset the number of notes this musician has played
	 */
	public void resetNotesIvePlayed() {
		this.notesIvePlayed = 0;
	}

	/**
	 * @param lastTickIPlayedANote the tick number of the last time this musician
	 *                             played a note
	 */
	public void setLastTickIPlayedANote(long lastTickIPlayedANote) {
		this.lastTickIPlayedANote = lastTickIPlayedANote;
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
		controller.playNote(channel, MusicianRule.REST);
	}
	
	public boolean isReceivingSequence() {
		return numNotesRemainingInSequence > 0;
	}
	
	public NoteSequence getSequence() {
		return this.sequence;
	}
	
	public void sendMessageToPeers(MusicianMessage message) {
		for (var peer : peers) {
			peer.receiveMessage(message);
		}
	}
}
