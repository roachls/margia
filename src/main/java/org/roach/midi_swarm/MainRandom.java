package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.Scanner;

import org.roach.midi_swarm.rules.RandomRule;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandom {

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
//		var controller = new MidiController(MidiController.LOOP_MIDI, tempo);
		var controller = new MidiController(MidiController.DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		for (int i = 0; i < numMusicians; i++) {
			musicians.add(new Musician(i, controller, tempo, i % numExternalInstruments, new RandomRule()));
		}

		/*
		 * @formatter:off
		 *  0  1  2  3
		 *  4  5  6  7
		 *  8  9 10 11
		 * 12 13 14 15
		 * @formatter:on
		 */
		musicians.get(0).addPeer(musicians.get(1));
		musicians.get(0).addPeer(musicians.get(4));

		musicians.get(1).addPeer(musicians.get(0));
		musicians.get(1).addPeer(musicians.get(2));
		musicians.get(1).addPeer(musicians.get(5));

		musicians.get(2).addPeer(musicians.get(1));
		musicians.get(2).addPeer(musicians.get(3));
		musicians.get(2).addPeer(musicians.get(6));

		musicians.get(3).addPeer(musicians.get(2));
		musicians.get(3).addPeer(musicians.get(7));

		musicians.get(4).addPeer(musicians.get(0));
		musicians.get(4).addPeer(musicians.get(5));
		musicians.get(4).addPeer(musicians.get(8));

		musicians.get(5).addPeer(musicians.get(1));
		musicians.get(5).addPeer(musicians.get(4));
		musicians.get(5).addPeer(musicians.get(6));
		musicians.get(5).addPeer(musicians.get(9));

		musicians.get(6).addPeer(musicians.get(2));
		musicians.get(6).addPeer(musicians.get(5));
		musicians.get(6).addPeer(musicians.get(7));
		musicians.get(6).addPeer(musicians.get(10));

		musicians.get(7).addPeer(musicians.get(3));
		musicians.get(7).addPeer(musicians.get(6));
		musicians.get(7).addPeer(musicians.get(11));

		musicians.get(8).addPeer(musicians.get(4));
		musicians.get(8).addPeer(musicians.get(9));
		musicians.get(8).addPeer(musicians.get(12));

		musicians.get(9).addPeer(musicians.get(5));
		musicians.get(9).addPeer(musicians.get(8));
		musicians.get(9).addPeer(musicians.get(10));
		musicians.get(9).addPeer(musicians.get(13));

		musicians.get(10).addPeer(musicians.get(6));
		musicians.get(10).addPeer(musicians.get(9));
		musicians.get(10).addPeer(musicians.get(11));
		musicians.get(10).addPeer(musicians.get(14));

		musicians.get(11).addPeer(musicians.get(7));
		musicians.get(11).addPeer(musicians.get(10));
		musicians.get(11).addPeer(musicians.get(15));

		musicians.get(12).addPeer(musicians.get(8));
		musicians.get(12).addPeer(musicians.get(13));

		musicians.get(13).addPeer(musicians.get(9));
		musicians.get(13).addPeer(musicians.get(12));
		musicians.get(13).addPeer(musicians.get(14));

		musicians.get(14).addPeer(musicians.get(10));
		musicians.get(14).addPeer(musicians.get(13));
		musicians.get(14).addPeer(musicians.get(15));

		musicians.get(15).addPeer(musicians.get(14));
		musicians.get(15).addPeer(musicians.get(11));
		musicians.get(15).addPeer(musicians.get(0));

		var transport = new Transport(musicians, tempo, controller);
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
