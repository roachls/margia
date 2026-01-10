package org.roach.margia.mains;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.roach.margia.Musician;
import org.roach.margia.mains.params.RandomParams;
import org.roach.margia.rules.SequenceRule;
import org.roach.margia.rules.SequenceRule.StartSequenceMessage;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainSequence implements Algorithm<RandomParams> {

    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams margiaParams) {
        var rParams = (RandomParams) margiaParams;
        var numMusicians = rParams.numMusicians;
        var musicians = new ArrayList<Musician>();
        var rand = new SecureRandom();
        rand.setSeed(mainParams.randomSeed);
        for (int i = 0; i < numMusicians; i++) {
            var m = new Musician(i % mainParams.numChannels, new SequenceRule());
            m.setListening(false);
            m.setKey(rParams.key);
            musicians.add(m);
        }

        // for each musician
        for (var i = 0; i < musicians.size(); i++) {
            musicians.get(i).addPeer(musicians.get((i + 1) % numMusicians));
        }

        musicians.get(0).receiveMessage(new StartSequenceMessage());

        return musicians;
    }

    @Override
    public String command() {
        return "sequence";
    }

    @Override
    public RandomParams createParams() {
        return new RandomParams();
    }

    @Override
    public String displayName() {
        return "Sequence";
    }

}
