package org.roach.midi_swarm;

import java.util.*;

import org.roach.midi_swarm.messages.*;

/**
 * Run with random agent and a 4x4 grid
 */
public class TestSingleAgent {

	/**
	 * Main entry point
	 * 
	 * @param args args[0] = number of musicians
	 */
	public static void main(String[] args) {
		var tempo = 60;
		var controller = new MidiController("loopMIDI Port", tempo);
		var r1 = new SimpleRepeatRule();
		var r2 = new SimpleRepeatRule();
		var musician1 = new Musician(0, controller, Key.Chromatic, tempo, 0, r1);
		r1.setMusician(musician1);
		var musician2 = new Musician(1, controller, Key.Chromatic, tempo, 1, r2);
		r2.setMusician(musician2);
		musician1.addPeer(musician2);

		var transport = new Transport(List.of(musician1, musician2), tempo);
		musician1.setTransport(transport);
		transport.start();

		musician1.receiveMessage(new StartSequence(4, 1));
		musician1.receiveMessage(new HeardNoteInfo(0, new NoteInfo(60, 60, 1)));
		musician1.receiveMessage(new HeardNoteInfo(1, new NoteInfo(62, 127, 2)));
		musician1.receiveMessage(new HeardNoteInfo(3, new NoteInfo(63, 60, 1)));
		musician1.receiveMessage(new HeardNoteInfo(4, new NoteInfo(65, 60, 4)));

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

	static class SimpleRepeatRule extends MusicianRule {
		NoteSequence lastSequence;
		List<NoteInfo> notesInSequence = new LinkedList<>();

		@Override
		public void calculateAction(long tick) {
			if (!musician.isReceivingSequence()) {
				var seq = musician.getSequence();
				if (seq != null) {
					for (var note : seq.notes()) {
						actionsToTake.add(() -> musician.playNote(note));
					}
				}
			}
		}

	}
}
