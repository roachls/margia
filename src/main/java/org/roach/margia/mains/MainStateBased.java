package org.roach.margia.mains;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.roach.margia.*;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;
import org.roach.margia.timing.InternalTimingSource;
import org.roach.margia.ui.MargiaWindow;

import com.beust.jcommander.*;

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
		var params = new StateBasedParams();
		var jCommander = new JCommander(params);
		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jCommander.usage();
			return;
		}
		var numMusicians = params.numRows * params.numCols;
		MidiController controller;
		if (params.sendExternalMidi) {
			controller = new MidiController("loopMIDI Port", params.tempo);
		} else {
			controller = new MidiController(MidiController.DEFAULT_SYNTH, params.tempo);
		}
		var musicians = new ArrayList<Musician>();
		var rules = new ArrayList<StateBasedRule>();
		for (int i = 0; i < numMusicians; i++) {
			var rule = new StateBasedRule(params.startingSequenceLength, 2);
			rules.add(rule);
			var musician = new Musician(i, controller, params.tempo, i % params.numChannels, rule);
			rule.setMusician(musician);
			musicians.add(musician);
		}

		var baseKey = Key.CMajor;
		Key.setRandomSeed(params.randomSeed);
		musicians.get(0).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(1).setKey(baseKey.of(Octave.O2.getLow(), Octave.O4.getHigh()));
		musicians.get(2).setKey(baseKey.of(Octave.O_NEG2.getLow(), Octave.O1.getHigh()));
		musicians.get(3).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getHigh()));
		musicians.get(4).setKey(baseKey.of(Octave.O4.getLow(), Octave.O5.getHigh()));
		musicians.get(5).setKey(baseKey.of(Octave.O1.getLow(), Octave.O3.getHigh()));
		musicians.get(6).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getLow() + 2));
		musicians.get(7).setKey(Key.Chromatic.of(Octave.O1.getLow(), Octave.O1.getHigh()));
		musicians.get(0 + 8).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
		musicians.get(1 + 8).setKey(baseKey.of(Octave.O2.getLow(), Octave.O4.getHigh()));
		musicians.get(2 + 8).setKey(baseKey.of(Octave.O_NEG2.getLow(), Octave.O1.getHigh()));
		musicians.get(3 + 8).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getHigh()));
		musicians.get(4 + 8).setKey(baseKey.of(Octave.O4.getLow(), Octave.O5.getHigh()));
		musicians.get(5 + 8).setKey(baseKey.of(Octave.O1.getLow(), Octave.O3.getHigh()));
		musicians.get(6 + 8).setKey(baseKey.of(Octave.O3.getLow(), Octave.O4.getLow() + 2));
		musicians.get(7 + 8).setKey(Key.Chromatic.of(Octave.O1.getLow(), Octave.O1.getHigh()));

		// mute 2nd and 3rd rows
		musicians.get(4).setMuted(true);
		musicians.get(5).setMuted(true);
		musicians.get(6).setMuted(true);
		musicians.get(7).setMuted(true);
		musicians.get(8).setMuted(true);
		musicians.get(9).setMuted(true);
		musicians.get(10).setMuted(true);
		musicians.get(11).setMuted(true);

		for (var row = 0; row < params.numRows; row++) {
			for (var col = 0; col < params.numCols; col++) {
				var index = row * params.numCols + col;
				if (col > 0) {
					musicians.get(index).addPeer(musicians.get(index - 1));
				}
				if (col < params.numCols - 1) {
					musicians.get(index).addPeer(musicians.get(index + 1));
				}
				if (row > 0) {
					musicians.get(index).addPeer(musicians.get(index - params.numCols));
				}
				if (row < params.numCols - 1) {
					musicians.get(index).addPeer(musicians.get(index + params.numCols));
				}
			}
		}

		// add a single random musician that is heard only by #0 and can't hear anyone
		// else
		var rMusician = new Musician(16, controller, params.tempo, 0, new RandomRule());
		rMusician.addPeer(musicians.get(0));
		musicians.get(numMusicians - 1).addPeer(rMusician);
		musicians.add(rMusician);

		var transport = new Transport(musicians, params.tempo, controller);
		transport.setControlDawTiming(true);
		var timing = new InternalTimingSource(transport, params.tempo);
		if (params.decrementSequenceLength) {
			for (int x = 1; x < params.startingSequenceLength; x++) {
				transport.addTickAction(x * params.tickDecrementCount,
						() -> rules.forEach(m -> m.decrementSequenceLength()));
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			timing.stop();
			transport.stop();
			controller.close();
		}));

		SwingUtilities.invokeLater(() -> {
			var ui = new MargiaWindow("State Based", timing, transport, musicians);
			ui.setVisible(true);
		});

	}

}
