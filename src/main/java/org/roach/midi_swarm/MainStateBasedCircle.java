package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.Scanner;

import org.roach.midi_swarm.rules.StateBasedRule;

/**
 * State-based agents in a circle
 */
public class MainStateBasedCircle {
	/**
	 * Main entry point
	 * 
	 * @param args args[0] = number of musicians
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: tempo numExternalInstruments");
			return;
		}
		var numMusicians = 2;
		var tempo = Integer.parseInt(args[0]);
		var numExternalInstruments = Integer.parseInt(args[1]);
//		var controller = new MidiController(MidiController.LOOP_MIDI, tempo);
		var controller = new MidiController(MidiController.DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		var rule = new StateBasedRule(4, 5);
		var musician = new Musician(0, controller, tempo, 0, rule);
		musicians.add(musician);

		for (int i = 1; i < numMusicians; i++) {
			rule = new StateBasedRule(4, 0);
			musician = new Musician(i, controller, tempo, i % numExternalInstruments, rule);
			rule.setMusician(musician);
			musicians.add(musician);
		}
//		musicians.get(1).setMuted(true);

//		musicians.get(0).setRangeLow(48).setRangeHi(92);
//		musicians.get(1).setRangeLow(48).setRangeHi(92);
//		musicians.get(2).setRangeHi(48);
//		musicians.get(5).setRangeLow(24).setRangeHi(48);
//		musicians.get(7).setRangeLow(60).setRangeHi(60+15);
//		musicians.get(8).setRangeLow(36).setRangeHi(47);

		/*
		 * @formatter:off
		 *  0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8
		 *  8 -> 0
		 * @formatter:on
		 */
		for (var i = 0; i < numMusicians - 1; i++) {
			musicians.get(i).addPeer(musicians.get(i + 1));
		}
		musicians.get(numMusicians - 1).addPeer(musicians.get(0));

		var transport = new Transport(musicians, tempo);
		transport.addTickAction(0, () -> {
			var m = musicians.get(0);
			m.receiveMessage(new NoteInfo(-1, 0, 1));
			m.receiveMessage(new NoteInfo(-1, 0, 1));
			m.receiveMessage(new NoteInfo(-1, 0, 1));
			m.receiveMessage(new NoteInfo(-1, 0, 1));
			m.receiveMessage(new NoteInfo(-1, 0, 1));
		});
		transport.addTickAction(1, () -> musicians.get(0).playNote(new NoteInfo(50, Musician.START_VELOCITY, 2)));
		transport.addTickAction(3, () -> musicians.get(0).playNote(new NoteInfo(52, Musician.START_VELOCITY, 1)));
		transport.addTickAction(4, () -> musicians.get(0).playNote(new NoteInfo(50, Musician.START_VELOCITY, 2)));
		transport.addTickAction(6, () -> musicians.get(0).playNote(new NoteInfo(52, Musician.START_VELOCITY, 1)));
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
