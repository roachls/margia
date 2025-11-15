package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Musician implements Runnable {
    public static final String PLAY_RANDOM_NOTE = "play a random note";
    private static final Pattern PLAY_RANDOM_NOTE_PATTERN = Pattern.compile(PLAY_RANDOM_NOTE);
    public static final String PLAY_INTERVAL = "play interval %s by %d";
    private static final Pattern PLAY_INTERVAL_PATTERN = Pattern.compile("play interval (up|down) by (\\d+)");
    private final int id;
    private final SimpleMidiController controller;
    private final BlockingQueue<MusicalMessage> messageQueue = new LinkedBlockingQueue<>();
    private final Key key;
    private final int channel;
    private final int tempo;
    private final List<Musician> peers = new ArrayList<>();

    public Musician(final int id, final SimpleMidiController controller, Key key, int tempo, int channel) {
	this.id = id;
	this.controller = controller;
	this.key = key;
	this.tempo = tempo;
	this.channel = channel;
    }

    public void addPeer(Musician peer) {
	if (!peers.contains(peer))
	    peers.add(peer);
    }

    @Override
    public void run() {
	while (true) {
	    try {
		var message = messageQueue.take();
		var matcher = PLAY_RANDOM_NOTE_PATTERN.matcher(message.message());
		if (matcher.matches()) {
		    var note = key.randomNote();
		    System.out.println(id + " playing random note " + note);
		    controller.playNotes(channel, List.of(note), 4, 90, Length.L1_4.getMillisForTempo(tempo));
		    pause(Length.L1_8);
		    for (var peer : peers) {
			var peerMessage = new MusicalMessage(PLAY_INTERVAL.formatted("up", 4), List.of(note));
			System.out.println(id + " sending " + peerMessage + " to " + peer.id);
			peer.receiveMessage(peerMessage);
		    }
		}
		matcher = PLAY_INTERVAL_PATTERN.matcher(message.message());
		if (matcher.matches()) {
		    var direction = matcher.group(1);
		    var distance = Integer.parseInt(matcher.group(2));
		    System.out.println(id + " got message to play " + direction + ":" + distance);
		    var endNotes = message.myNotes()
					  .stream()
					  .map(n -> "up".equals(direction) ? key.upInterval(n, distance)
						  : key.downInterval(n, distance))
					  .toList();
		    controller.playNotes(channel, endNotes, 4, 90, Length.L1_8.getMillisForTempo(tempo));
		    pause(Length.L1_16);
		    for (var peer : peers) {
			var peerInterval = peer.id % 2 == 0 ? 2 : 6;
			var peerMessage = new MusicalMessage(PLAY_INTERVAL.formatted("down", peerInterval), endNotes);
			System.out.println(id + " sending " + peerMessage + " to " + peer.id);
			peer.receiveMessage(peerMessage);
		    }
		}
	    } catch (InterruptedException e) {
		Thread.currentThread()
		      .interrupt();
	    }
	}
    }

    private void pause(Length length) {
	try {
	    TimeUnit.MILLISECONDS.sleep(length.getMillisForTempo(tempo));
	} catch (InterruptedException e) {
	    Thread.currentThread()
		  .interrupt();
	}
    }

    public void receiveMessage(MusicalMessage message) {
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
