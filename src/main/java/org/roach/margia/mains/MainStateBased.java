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
            var musician = new Musician(i % mainParams.numChannels, rule);
            rule.setMusician(musician);
            musicians.add(musician);
        }

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
        var rMusician = new Musician(0, new RandomRule());
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
