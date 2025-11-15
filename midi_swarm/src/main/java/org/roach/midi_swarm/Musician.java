package org.roach.midi_swarm;

import java.util.*;
import java.util.concurrent.*;

/**
 * A {@link Musician} is the core class of the application. It continuously
 * polls its own message queue and responds to any messages it receives in the
 * order they were received.
 */
public class Musician implements Runnable {
	private final int id;
	private final SimpleMidiController controller;
	private final BlockingQueue<Note> messageQueue = new LinkedBlockingQueue<>();
	private final Key key;
	private final int channel;
	private final int tempo;
	private final List<Musician> peers = new ArrayList<>();
	private final ExecutorService scheduler = Executors.newVirtualThreadPerTaskExecutor();
	private long lastTimeIPlayedANote;
	private List<MusicianRule> rules = new ArrayList<>();

	/**
	 * @param id         unique id of this {@link Musician}
	 * @param controller MIDI controller that will actually play the notes
	 * @param key        key to use for generating notes
	 * @param tempo      tempo to play at
	 * @param channel    MIDI channel
	 */
	public Musician(final int id, final SimpleMidiController controller, Key key, int tempo, int channel) {
		this.id = id;
		this.controller = controller;
		this.key = key;
		this.tempo = tempo;
		this.channel = channel;
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
			if (timeSinceLastNote < Length.L1_16.getMillisForTempo(tempo))
				continue;
			if (messageQueue.isEmpty()) {
				var randomNote = key.randomNote();
				controller.playNotes(channel, List.of(randomNote), 4, 90, Length.L1_16.getMillisForTempo(tempo));
				for (var peer : peers) {
					peer.receiveMessage(new MusicalMessage(List.of(randomNote)));
				}
			}
			var noteIHeard = messageQueue.poll();
			for (var rule : rules) {
				rule.act(noteIHeard, this);
			}
		}
	}

	private void pause(Length length) {
		try {
			TimeUnit.MILLISECONDS.sleep(length.getMillisForTempo(tempo));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * @param message receive a message and place it on the message queue
	 */
	public void receiveMessage(MusicalMessage message) {
		for (var note : message.myNotes()) {
			this.messageQueue.offer(note);
		}
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
