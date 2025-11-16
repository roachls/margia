package org.roach.midi_swarm;

import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.midi_swarm.random.DieRoller;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician implements Runnable {
	private final int id;
	private final SimpleMidiController controller;
	private final BlockingQueue<NoteInfo> messageQueue = new LinkedBlockingQueue<>();
	private final Key key;
	private final int channel;
	private final int tempo;
	private final List<Musician> peers = new ArrayList<>();
	private long lastTimeIPlayedANote;
	private long timeMyNoteWillBeDone;
	private List<MusicianRule> rules = new ArrayList<>();
	private int notesIvePlayed;
	private NoteInfo myLastNote;
	private final Logger logger;
	private int octave = 4;
	private int velocity = 64;

	/**
	 * @param id         unique id of this {@link Musician}
	 * @param controller MIDI controller that will actually play the notes
	 * @param key        key to use for generating notes
	 * @param tempo      tempo to play at
	 * @param channel    MIDI channel
	 */
	public Musician(final int id, final SimpleMidiController controller, Key key, int tempo, int channel) {
		this.id = id;
		this.logger = LogManager.getLogger("Musician_" + id);
		this.controller = controller;
		this.key = key;
		this.tempo = tempo;
		this.channel = channel;

		this.rules.add(n -> {
			var r = DieRoller.rollDice("1d10");
			switch (r) {
			case 1:
				playNote(n);
				break;
			case 2:
				if (myLastNote != null)
					playNote(myLastNote);
				break;
			case 3:
				rest(Length.L1_16);
				myLastNote = n;
				break;
			case 4: {
				var ni = new NoteInfo(key.upInterval(n.note(), 5), n.octave(), n.velocity(), Length.L1_16);
				playNote(ni);
				break;
			}
			case 5: {
				var ni = new NoteInfo(key.upInterval(n.note(), 8), n.octave(), n.velocity(), Length.L1_8);
				playNote(ni);
				break;
			}
			case 6:
//				rest(Length.L1_8);
//				receiveMessage(n);
				break;
			case 7:
				octave++;
				if (octave > 6)
					octave = 6;
				logger.atDebug().log("{}: increased octave to {}", id, octave);
				break;
			case 8:
				octave--;
				if (octave < 1)
					octave = 1;
				logger.atDebug().log("{}: decreased octave to {}", id, octave);
				break;
			case 9:
				velocity = (int)((double)velocity * 1.1);
				if (velocity > 127)
					velocity = 127;
				logger.atDebug().log("{}: increased velocity to {}", id, velocity);
				break;
			case 10:
				velocity = (int)((double)velocity * 0.9);
				if (velocity < 0)
					velocity = 0;
				logger.atDebug().log("{}: decreased velocity to {}", id, velocity);
				break;
			default:
				break;
			}
		});
	}

	private void playNote(NoteInfo note) {
		if (note == null)
			return;
		logger.atDebug().log("{}: playing note {} on channel {}", id, note, channel);
		controller.playNote(channel, note);
		rest(note.length());
		lastTimeIPlayedANote = System.currentTimeMillis();
		timeMyNoteWillBeDone = lastTimeIPlayedANote + note.length().getMillisForTempo(tempo);
		myLastNote = note;
		notesIvePlayed++;
		for (var peer : peers) {
			peer.receiveMessage(note);
		}
	}

	/**
	 * @param peer another {@link Musician} with which this one may communicate
	 */
	public void addPeer(Musician peer) {
		if (!peers.contains(peer))
			peers.add(peer);
	}

	@Override
	public void run() {
		while (true) {
			var now = System.currentTimeMillis();
			var timeSinceLastNote = now - lastTimeIPlayedANote;
			logger.atDebug().log("{}: now={}, lastNote={}", id, now, timeSinceLastNote);
			if (timeSinceLastNote == 0) {
				rest(Length.L1_16);
				continue;
			}
			if (timeSinceLastNote < Length.L1_16.getMillisForTempo(tempo) || timeMyNoteWillBeDone > now) {
				logger.atDebug().log(
						"{}: Doing nothing because it hasn't been long enough (lastTimeIPlayedANote={}, now={}, timeSinceLastNote={}, timeMyNoteWillBeDone={})",
						id, lastTimeIPlayedANote, now, timeSinceLastNote, timeMyNoteWillBeDone);
				rest(Length.L1_16);
				continue;
			}
			if (notesIvePlayed >= 5) {
				logger.atDebug().log("{}: resting because I've played 5 notes", id);
				notesIvePlayed = 0;
				rest(Length.L1_16);
				timeMyNoteWillBeDone = 0;
				continue;
			}
			timeMyNoteWillBeDone = 0;
			if (messageQueue.isEmpty()) {
				logger.atDebug().log("{} queue is empty", id);
				rest(Length.L1_16);
				var rand = DieRoller.rollDice("2d6");
				if (rand <= 4) {
					var randomNote = new NoteInfo(key.randomNote(), octave, velocity, Length.L1_16);
					logger.atDebug().log("{}: playing {}", id, randomNote);
					playNote(randomNote);
				}
			}
			var noteIHeard = messageQueue.poll();
			logger.atDebug().log("{}: heard {}", id, noteIHeard);
			if (noteIHeard == null)
				continue;
			for (var rule : rules) {
				rule.act(noteIHeard);
			}
		}
	}

	private void rest(Length length) {
		try {
			TimeUnit.MILLISECONDS.sleep(length.getMillisForTempo(tempo));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * @param message receive a message and place it on the message queue
	 */
	public void receiveMessage(NoteInfo message) {
		this.messageQueue.offer(message);
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
}
