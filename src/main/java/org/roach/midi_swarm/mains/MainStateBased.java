package org.roach.midi_swarm.mains;

import java.util.ArrayList;
import java.util.Scanner;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.rules.RandomRule;
import org.roach.midi_swarm.rules.StateBasedRule;

/**
 * A state-based ruleset
 */
public class MainStateBased {
	/**
	 * Main entry point
	 * 
	 * @param args args[0] = number of musicians
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("args: tempo numExternalInstruments startingSeqLength randomSeed");
			return;
		}
		var numMusicians = 16;
		var tempo = Integer.parseInt(args[0]);
		var numExternalInstruments = Integer.parseInt(args[1]);
		var startingSequenceLength = Integer.parseInt(args[2]);
		var randomSeed = Long.parseLong(args[3]);
		var controller = new MidiController("loopMIDI Port", tempo);
//		var controller = new MidiController(DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		var rules = new ArrayList<StateBasedRule>();
		for (int i = 0; i < numMusicians; i++) {
			var rule = new StateBasedRule(startingSequenceLength, 2);
			rules.add(rule);
			var musician = new Musician(i, controller, tempo, i % numExternalInstruments, rule);
			rule.setMusician(musician);
			musicians.add(musician);
		}

		var baseKey = Key.CMajor;
		Key.setRandomSeed(randomSeed);
		musicians.get(0).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(1).setKey(baseKey.of(Octave.O2.getLow(), Octave.O4.getHigh()));
		musicians.get(2).setKey(baseKey.of(Octave.O_NEG2.getLow(), Octave.O1.getHigh()));
		musicians.get(3).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getHigh()));
		musicians.get(4).setKey(baseKey.of(Octave.O4.getLow(), Octave.O5.getHigh()));
		musicians.get(5).setKey(baseKey.of(Octave.O1.getLow(), Octave.O3.getHigh()));
		musicians.get(6).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getLow() + 2));
		musicians.get(7).setKey(Key.Chromatic.of(Octave.O1.getLow(), Octave.O1.getHigh()));
		musicians.get(0+8).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(1+8).setKey(baseKey.of(Octave.O2.getLow(), Octave.O4.getHigh()));
		musicians.get(2+8).setKey(baseKey.of(Octave.O_NEG2.getLow(), Octave.O1.getHigh()));
		musicians.get(3+8).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getHigh()));
		musicians.get(4+8).setKey(baseKey.of(Octave.O4.getLow(), Octave.O5.getHigh()));
		musicians.get(5+8).setKey(baseKey.of(Octave.O1.getLow(), Octave.O3.getHigh()));
		musicians.get(6+8).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getLow() + 2));
		musicians.get(7+8).setKey(Key.Chromatic.of(Octave.O1.getLow(), Octave.O1.getHigh()));
		
		// mute 2nd and 3rd rows
		musicians.get(4).setMuted(true);
		musicians.get(5).setMuted(true);
		musicians.get(6).setMuted(true);
		musicians.get(7).setMuted(true);
		musicians.get(8).setMuted(true);
		musicians.get(9).setMuted(true);
		musicians.get(10).setMuted(true);
		musicians.get(11).setMuted(true);

		var numCols = 4;
		var numRows = 4;
		/*
		 * @formatter:off
		 *  0  1  2  3
		 *  4  5  6  7
		 *  8  9 10 11
		 * 12 13 14 15
		 * @formatter:on
		 */
		for (var row = 0; row < numRows; row++) {
			for (var col = 0; col < numCols; col++) {
				var index = row * numCols + col;
				if (col > 0) {
					musicians.get(index).addPeer(musicians.get(index - 1));
				}
				if (col < numCols - 1) {
					musicians.get(index).addPeer(musicians.get(index + 1));
				}
				if (row > 0) {
					musicians.get(index).addPeer(musicians.get(index - numCols));
				}
				if (row < numRows - 1) {
					musicians.get(index).addPeer(musicians.get(index + numCols));
				}
			}
		}
		
		// add a single random musician that is heard only by #0 and can't hear anyone else
		var rMusician = new Musician(16, controller, tempo, 0, new RandomRule());
		rMusician.addPeer(musicians.get(0));
		musicians.get(numMusicians - 1).addPeer(rMusician);
		musicians.add(rMusician);

		var transport = new Transport(musicians, tempo, controller);
		transport.setControlDawTiming(true);
		transport.addTickAction(100, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(200, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(300, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(400, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(500, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(600, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(700, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(800, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(900, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(1000, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.addTickAction(1100, () -> rules.forEach(m -> m.decrementSequenceLength()));
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
