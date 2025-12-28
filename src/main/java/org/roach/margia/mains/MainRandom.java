package org.roach.margia.mains;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.roach.margia.*;
import org.roach.margia.mains.params.RandomParams;
import org.roach.margia.rules.RandomRule;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandom implements Algorithm<RandomParams> {

    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams margiaParams, MidiController controller) {
        var rParams = (RandomParams) margiaParams;
        var numMusicians = rParams.numMusicians;
        var musicians = new ArrayList<Musician>();
        var rand = new SecureRandom();
        rand.setSeed(mainParams.randomSeed);
        for (int i = 0; i < numMusicians; i++) {
            var m = new Musician(i, controller, i % mainParams.numChannels, new RandomRule());
            m.setMuted(rand.nextBoolean());
            m.setKey(rParams.key);
            musicians.add(m);
        }

        for (var i = 0; i < musicians.size(); i++) {
            var musician = musicians.get(i);
            var j = rand.nextInt(musicians.size());
            while (j == i) {
                j = rand.nextInt(musicians.size());
            }
            musician.addPeer(musicians.get(j));
        }
        return musicians;
    }

    @Override
    public String command() {
        return "random";
    }

    @Override
    public RandomParams createParams() {
        return new RandomParams();
    }

    @Override
    public String displayName() {
        return "Random";
    }

}
