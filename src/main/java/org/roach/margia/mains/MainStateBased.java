package org.roach.margia.mains;

import java.util.ArrayList;
import java.util.List;

import org.roach.margia.*;
import org.roach.margia.mains.params.StateBasedParams;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;

/**
 * A state-based ruleset
 */
public class MainStateBased implements Algorithm<StateBasedParams> {

    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams margiaParams) {
        var sbParams = (StateBasedParams) margiaParams;
        var numMusicians = sbParams.numRows * sbParams.numCols;
        var musicians = new ArrayList<Musician>();
        for (int i = 0; i < numMusicians; i++) {
            var rule = new StateBasedRule(sbParams.startingSequenceLength, 2);
            var musician = new Musician(i, i % mainParams.numChannels, rule);
            rule.setMusician(musician);
            musicians.add(musician);
        }

        var baseKey = sbParams.key;
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

        for (var row = 0; row < sbParams.numRows; row++) {
            for (var col = 0; col < sbParams.numCols; col++) {
                var index = row * sbParams.numCols + col;
                if (col > 0) {
                    musicians.get(index).addPeer(musicians.get(index - 1));
                }
                if (col < sbParams.numCols - 1) {
                    musicians.get(index).addPeer(musicians.get(index + 1));
                }
                if (row > 0) {
                    musicians.get(index).addPeer(musicians.get(index - sbParams.numCols));
                }
                if (row < sbParams.numCols - 1) {
                    musicians.get(index).addPeer(musicians.get(index + sbParams.numCols));
                }
            }
        }

        // add a single random musician that is heard only by #0 and can't hear anyone
        // else
        var rMusician = new Musician(musicians.size(), 0, new RandomRule());
        rMusician.addPeer(musicians.get(0));
        musicians.get(numMusicians - 1).addPeer(rMusician);
        musicians.add(rMusician);

        return musicians;
    }

    @Override
    public String command() {
        return "statebased";
    }

    @Override
    public StateBasedParams createParams() {
        return new StateBasedParams();
    }

    @Override
    public String displayName() {
        return "State-based";
    }

}
