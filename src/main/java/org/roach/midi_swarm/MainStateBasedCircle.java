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
		var numMusicians = 16;
		var tempo = Integer.parseInt(args[0]);
		var numExternalInstruments = Integer.parseInt(args[1]);
		var controller = new MidiController("loopMIDI Port", tempo);
//		var controller = new MidiController(DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		for (int i = 0; i < numMusicians; i++) {
			var rule = new StateBasedRule(5);
			var musician = new Musician(i, controller, tempo, i % numExternalInstruments, rule);
			rule.setMusician(musician);
			musicians.add(musician);
		}

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

		musicians.get(0).receiveMessage(new NoteInfo(50, Musician.START_VELOCITY, 1));
		musicians.get(0).receiveMessage(new NoteInfo(62, Musician.START_VELOCITY, 1));
		musicians.get(0).receiveMessage(MusicianRule.REST.apply(2));
		musicians.get(0).receiveMessage(new NoteInfo(65, Musician.START_VELOCITY, 1));
		musicians.get(0).receiveMessage(new NoteInfo(57, Musician.START_VELOCITY, 2));

		var transport = new Transport(musicians, tempo);
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
