package org.roach.margia.mains;

import java.util.ArrayList;
import java.util.Scanner;

import org.roach.margia.*;
import org.roach.margia.rules.RandomRule;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandomSimple {

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
		var controller = new MidiController("loopMIDI Port", tempo);
//		var controller = new MidiController(DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		for (int i = 0; i < numMusicians; i++) {
			musicians.add(new Musician(i, controller, i % numExternalInstruments, new RandomRule()));
		}

		/*
		 * @formatter:off
		 *  0 -> 1
		 * @formatter:on
		 */
		musicians.get(0).addPeer(musicians.get(1));
		musicians.get(1).addPeer(musicians.get(0));

		var transport = new Transport(musicians, tempo, controller);
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
