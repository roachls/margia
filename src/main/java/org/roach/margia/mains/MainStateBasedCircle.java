package org.roach.margia.mains;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.roach.margia.*;
import org.roach.margia.random.DieRoller;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;
import org.roach.margia.timing.InternalTimingSource;
import org.roach.margia.ui.MargiaWindow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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
        var params = new StateBasedParams();
        var jCommander = new JCommander(params);
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
            return;
        }
        MidiController controller;
        if (params.sendExternalMidi) {
            controller = new MidiController(MidiController.LOOP_MIDI, params.tempo);
        } else {
            controller = new MidiController(MidiController.DEFAULT_SYNTH, params.tempo);
        }
        var musicians = new ArrayList<Musician>();
        MusicianRule rule = new RandomRule();
        var musician = new Musician(0, controller, 0, rule);
        musicians.add(musician);

        var numMusicians = params.numCols * params.numCols;
        for (int i = 1; i < numMusicians; i++) {
            rule = new StateBasedRule(8, 0);
            musician = new Musician(i, controller, i % params.numChannels, rule);
            rule.setMusician(musician);
            musicians.add(musician);
        }
        var baseKey = Key.CMajor;
        musicians.get(0).setKey(baseKey.of(Octave.O2.getLow(), Octave.O5.getHigh()));
        musicians.get(1).setKey(baseKey.of(Octave.O2.getLow(), Octave.O4.getHigh()));
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

        var transport = new Transport(musicians, params.tempo, controller);
        transport.setControlDawTiming(params.sendExternalMidi);
        DieRoller.setSeed(params.randomSeed);

        var timing = new InternalTimingSource(transport, params.tempo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            controller.close();
        }));

        SwingUtilities.invokeLater(() -> {
            var ui = new MargiaWindow("State Based Circle", timing, transport, musicians);
//			ui.setExtendedState(JFrame.MAXIMIZED_BOTH);
            ui.setVisible(true);
        });

    }

}
