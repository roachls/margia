package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.Scanner;

import org.roach.midi_swarm.rules.StateBasedRule;

public class MainStateBasedCircle {
	/**
	 * Main entry point
	 * 
	 * @param args args[0] = number of musicians
	 */
	public static void main(String[] args) {
		if (args.length < 2)
			return;
		var numMusicians = 8;
		var tempo = Integer.parseInt(args[0]);
		var numExternalInstruments = Integer.parseInt(args[1]);
		var controller = new MidiController("loopMIDI Port", tempo);
//		var controller = new MidiController(DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		for (int i = 0; i < numMusicians; i++) {
			musicians.add(
					new Musician(i, controller, Key.CMajor, tempo, i % numExternalInstruments, new StateBasedRule()));
		}

		/*
		 * @formatter:off
		 *  0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8
		 *  8 -> 0
		 * @formatter:on
		 */
		musicians.get(0).addPeer(musicians.get(1));
		musicians.get(1).addPeer(musicians.get(2));
		musicians.get(2).addPeer(musicians.get(3));
		musicians.get(3).addPeer(musicians.get(4));
		musicians.get(4).addPeer(musicians.get(5));
		musicians.get(5).addPeer(musicians.get(6));
		musicians.get(6).addPeer(musicians.get(0));
//		musicians.get(7).addPeer(musicians.get(0));

		musicians.get(0).receiveMessage(
				new NoteInfo(Note.A, musicians.get(0).getOctave(), musicians.get(0).getVelocity(), Length.L1_16));
		musicians.get(0).receiveMessage(
				new NoteInfo(Note.C, musicians.get(0).getOctave(), musicians.get(0).getVelocity(), Length.L1_16));
		musicians.get(0).receiveMessage(
				new NoteInfo(Note.D, musicians.get(0).getOctave(), musicians.get(0).getVelocity(), Length.L1_16));
		musicians.get(0).receiveMessage(
				new NoteInfo(Note.F, musicians.get(0).getOctave(), musicians.get(0).getVelocity(), Length.L1_16));

		var transport = new Transport(musicians, tempo);
		for (var musician : musicians) {
			musician.setTransport(transport);
		}
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
