package org.roach.midi_swarm;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician {
	/**
	 * Minimum octave a musician is allowed to go down to
	 */
	public static final int MIN_OCTAVE = -1;
	/**
	 * Maximum octave a musician is allowed to go up to
	 */
	public static final int MAX_OCTAVE = 6;
	/**
	 * Minimum velocity a note may be played at
	 */
	public static final int MIN_VELOCITY = 0;
	/**
	 * Maximum velocity a note may be played at
	 */
	public static final int MAX_VELOCITY = 127;
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
	private int octave = 4;
	private int velocity = 64;
	private Transport transport;

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
		for (var peer : peers) {
			peer.receiveMessage(note);
		}
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
	 * Performs actions after receiving a tick from the {@link Transport}
	 * 
	 * @param tick the tick number
	 */
	public void doTick(long tick) {
		logger.atDebug().log("{}: tick={}, lastTickIPlayedANote={}", id, tick, lastTickIPlayedANote);
		rule.act(tick);
	}

	/**
	 * @param message receive a message and place it on the message queue
	 */
	public void receiveMessage(NoteInfo message) {
		this.messageQueue.offer(message);
	}

	/**
	 * Decrease the velocity by the given amount but no lower than
	 * {@link #MIN_VELOCITY}
	 * 
	 * @param amount amount of decrease
	 */
	public void decreaseVelocity(int amount) {
		velocity -= amount;
		if (velocity < MIN_VELOCITY)
			velocity = MIN_VELOCITY;
		logger.atDebug().log("{}: decreased velocity to {}", id, velocity);
	}

	/**
	 * Increase velocity by the given amount but no higher than
	 * {@link #MAX_VELOCITY}
	 * 
	 * @param amount amount of increase
	 */
	public void increaseVelocity(int amount) {
		velocity += amount;
		if (velocity > MAX_VELOCITY)
			velocity = MAX_VELOCITY;
		logger.atDebug().log("{}: increased velocity to {}", id, velocity);
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
	 * @return current octave this musician is playing in
	 */
	public int getOctave() {
		return octave;
	}

	/**
	 * @return current velocity at which this musician is playing
	 */
	public int getVelocity() {
		return velocity;
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
	 * decrease the octave this musician is playing, but no lower than {@link #MIN_OCTAVE}
	 */
	public void decrementOctave() {
		octave--;
		if (octave < MIN_OCTAVE)
			octave = MIN_OCTAVE;
		logger.atDebug().log("{}: decremented octave to {}", id, octave);
	}

	/**
	 * Increase octave by 1, up to max of {@link #MAX_OCTAVE}
	 */
	public void incrementOctave() {
		octave++;
		if (octave > MAX_OCTAVE)
			octave = MAX_OCTAVE;
		logger.atDebug().log("{}: incremented octave to {}", id, octave);
	}

	/**
	 * Rest for one 16th
	 */
	public void rest() {
		controller.playNote(channel, new NoteInfo(Note.REST, 0, 0, Length.L1_16));
	}
}
