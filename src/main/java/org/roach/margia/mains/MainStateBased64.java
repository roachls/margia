package org.roach.margia.mains;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.roach.margia.*;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;
import org.roach.margia.timing.InternalTimingSource;
import org.roach.margia.ui.MargiaWindow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * A state-based ruleset
 */
public class MainStateBased64 {
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
            controller = new MidiController("loopMIDI Port", params.tempo);
        } else {
            controller = new MidiController(MidiController.DEFAULT_SYNTH, params.tempo);
        }
        var musicians = new ArrayList<Musician>();
        var rules = new ArrayList<MusicianRule>();
        for (var x = 0; x < params.numCols; x++) {
            for (var y = 0; y < params.numRows; y++) {
                MusicianRule rule;
                if (x == 0)
                    rule = new RandomRule();
                else
                    rule = new StateBasedRule(16, params.numCols - y);
                rules.add(rule);
                var index = x * params.numRows + y;
                var musician = new Musician(index, controller, y, rule);
                if (x != 3 && x != 7)
                    musician.setMuted(true);
                musician.setKey(Key.Chromatic.of(Octave.O0.getLow(), Octave.O4.getHigh()));
                rule.setMusician(musician);
                musicians.add(musician);
            }
        }

        musicians.get(0).addPeer(musicians.get(8));
        musicians.get(1).addPeer(musicians.get(9));
        musicians.get(2).addPeer(musicians.get(10));
        musicians.get(3).addPeer(musicians.get(11));
        musicians.get(4).addPeer(musicians.get(12));
        musicians.get(5).addPeer(musicians.get(13));
        musicians.get(6).addPeer(musicians.get(14));
        musicians.get(7).addPeer(musicians.get(15));

        musicians.get(8).addPeer(musicians.get(17));
        musicians.get(9).addPeer(musicians.get(16));
        musicians.get(9).addPeer(musicians.get(18));
        musicians.get(10).addPeer(musicians.get(17));
        musicians.get(10).addPeer(musicians.get(19));
        musicians.get(11).addPeer(musicians.get(18));
        musicians.get(11).addPeer(musicians.get(20));
        musicians.get(12).addPeer(musicians.get(19));
        musicians.get(12).addPeer(musicians.get(21));
        musicians.get(13).addPeer(musicians.get(20));
        musicians.get(13).addPeer(musicians.get(22));
        musicians.get(14).addPeer(musicians.get(21));
        musicians.get(14).addPeer(musicians.get(23));
        musicians.get(15).addPeer(musicians.get(22));

        musicians.get(16).addPeer(musicians.get(23));
        musicians.get(17).addPeer(musicians.get(24));
        musicians.get(17).addPeer(musicians.get(26));
        musicians.get(18).addPeer(musicians.get(25));
        musicians.get(18).addPeer(musicians.get(27));
        musicians.get(19).addPeer(musicians.get(26));
        musicians.get(19).addPeer(musicians.get(28));
        musicians.get(20).addPeer(musicians.get(27));
        musicians.get(20).addPeer(musicians.get(29));
        musicians.get(21).addPeer(musicians.get(28));
        musicians.get(21).addPeer(musicians.get(30));
        musicians.get(22).addPeer(musicians.get(29));
        musicians.get(22).addPeer(musicians.get(31));
        musicians.get(23).addPeer(musicians.get(30));

        musicians.get(24).addPeer(musicians.get(33));
        musicians.get(25).addPeer(musicians.get(32));
        musicians.get(25).addPeer(musicians.get(34));
        musicians.get(26).addPeer(musicians.get(33));
        musicians.get(26).addPeer(musicians.get(35));
        musicians.get(27).addPeer(musicians.get(34));
        musicians.get(27).addPeer(musicians.get(36));
        musicians.get(28).addPeer(musicians.get(35));
        musicians.get(28).addPeer(musicians.get(37));
        musicians.get(29).addPeer(musicians.get(36));
        musicians.get(29).addPeer(musicians.get(38));
        musicians.get(30).addPeer(musicians.get(37));
        musicians.get(30).addPeer(musicians.get(39));
        musicians.get(31).addPeer(musicians.get(38));

        musicians.get(32).addPeer(musicians.get(41));
        musicians.get(33).addPeer(musicians.get(40));
        musicians.get(33).addPeer(musicians.get(42));
        musicians.get(34).addPeer(musicians.get(41));
        musicians.get(34).addPeer(musicians.get(43));
        musicians.get(35).addPeer(musicians.get(42));
        musicians.get(35).addPeer(musicians.get(44));
        musicians.get(36).addPeer(musicians.get(43));
        musicians.get(36).addPeer(musicians.get(45));
        musicians.get(37).addPeer(musicians.get(44));
        musicians.get(37).addPeer(musicians.get(46));
        musicians.get(38).addPeer(musicians.get(45));
        musicians.get(38).addPeer(musicians.get(47));
        musicians.get(39).addPeer(musicians.get(46));

        musicians.get(40).addPeer(musicians.get(49));
        musicians.get(41).addPeer(musicians.get(48));
        musicians.get(41).addPeer(musicians.get(50));
        musicians.get(42).addPeer(musicians.get(49));
        musicians.get(42).addPeer(musicians.get(51));
        musicians.get(43).addPeer(musicians.get(50));
        musicians.get(43).addPeer(musicians.get(52));
        musicians.get(44).addPeer(musicians.get(51));
        musicians.get(44).addPeer(musicians.get(53));
        musicians.get(45).addPeer(musicians.get(52));
        musicians.get(45).addPeer(musicians.get(54));
        musicians.get(46).addPeer(musicians.get(53));
        musicians.get(46).addPeer(musicians.get(55));
        musicians.get(47).addPeer(musicians.get(54));

        musicians.get(48).addPeer(musicians.get(57));
        musicians.get(49).addPeer(musicians.get(56));
        musicians.get(49).addPeer(musicians.get(58));
        musicians.get(50).addPeer(musicians.get(57));
        musicians.get(50).addPeer(musicians.get(59));
        musicians.get(51).addPeer(musicians.get(58));
        musicians.get(51).addPeer(musicians.get(60));
        musicians.get(52).addPeer(musicians.get(59));
        musicians.get(52).addPeer(musicians.get(61));
        musicians.get(53).addPeer(musicians.get(60));
        musicians.get(53).addPeer(musicians.get(62));
        musicians.get(54).addPeer(musicians.get(61));
        musicians.get(54).addPeer(musicians.get(63));
        musicians.get(55).addPeer(musicians.get(62));

        musicians.get(56).addPeer(musicians.get(0));
        musicians.get(57).addPeer(musicians.get(1));
        musicians.get(58).addPeer(musicians.get(2));
        musicians.get(59).addPeer(musicians.get(3));
        musicians.get(60).addPeer(musicians.get(4));
        musicians.get(61).addPeer(musicians.get(5));
        musicians.get(62).addPeer(musicians.get(6));
        musicians.get(63).addPeer(musicians.get(7));

        Key.setRandomSeed(params.randomSeed);

        var transport = new Transport(musicians, params.tempo, controller);
        transport.setControlDawTiming(params.sendExternalMidi);

        var timing = new InternalTimingSource(transport, params.tempo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timing.stop();
            transport.stop();
            controller.close();
        }));

        SwingUtilities.invokeLater(() -> {
            var ui = new MargiaWindow("State Based 64", timing, transport, musicians);
            ui.setExtendedState(JFrame.MAXIMIZED_BOTH);
            ui.setVisible(true);
        });

    }

}
