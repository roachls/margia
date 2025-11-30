package org.roach.midi_swarm.mains;

import java.util.*;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.random.DieRoller;
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
		var numMusicians = 8;
		var tempo = Integer.parseInt(args[0]);
		var numExternalInstruments = Integer.parseInt(args[1]);
		var controller = new MidiController(MidiController.LOOP_MIDI, tempo);
//		var controller = new MidiController(MidiController.DEFAULT_SYNTH, tempo);
		var musicians = new ArrayList<Musician>();
		var rule = new StateBasedRule(4, 7);
		var musician = new Musician(0, controller, tempo, 0, rule);
		musicians.add(musician);

		for (int i = 1; i < numMusicians; i++) {
			rule = new StateBasedRule(8, 0);
			musician = new Musician(i, controller, tempo, i % numExternalInstruments, rule);
			rule.setMusician(musician);
			musicians.add(musician);
		}
		var baseKey = Key.CPentatonic;
		musicians.get(0).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(1).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(2).setKey(baseKey.of(Octave.O_NEG2.getLow(), Octave.O1.getHigh()));
		musicians.get(3).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getHigh()));
		musicians.get(4).setKey(baseKey.of(Octave.O4.getLow(), Octave.O5.getHigh()));
		musicians.get(5).setKey(baseKey.of(Octave.O1.getLow(), Octave.O3.getHigh()));
		musicians.get(6).setKey(baseKey);
		musicians.get(7).setKey(Key.Chromatic.of(Octave.O1.getLow(), Octave.O1.getHigh()));

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

		var transport = new Transport(musicians, tempo, controller);
		transport.addTickAction(0, () -> {
			var m = musicians.get(0);
			// number of ticks in starting sequence -1
			var n = 16 - 1;
			for (var i = 0; i < n; i++) {
				m.receiveMessage(new NoteInfo(-1, 0, 1));
			}
		});
		transport.addTickAction(1, () -> musicians.get(0).playNote(new NoteInfo(60, Musician.START_VELOCITY, 2)));
		transport.addTickAction(3, () -> musicians.get(0).playNote(new NoteInfo(62, Musician.START_VELOCITY, 4)));
		transport.addTickAction(7, () -> musicians.get(0).playNote(new NoteInfo(64, Musician.START_VELOCITY, 2)));
		transport.addTickAction(9, () -> musicians.get(0).playNote(new NoteInfo(65, Musician.START_VELOCITY, 2)));
		transport.addTickAction(11, () -> musicians.get(0).playNote(new NoteInfo(67, Musician.START_VELOCITY, 2)));
		transport.addTickAction(13, () -> musicians.get(0).playNote(new NoteInfo(69, Musician.START_VELOCITY, 2)));
		transport.addTickAction(15, () -> musicians.get(0).playNote(new NoteInfo(67, Musician.START_VELOCITY, 2)));
		transport.setControlDawTiming(true);
		DieRoller.setSeed(1);
		transport.start();

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
