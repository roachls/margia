package org.roach.margia.mains;

import java.util.*;

import org.roach.margia.*;
import org.roach.margia.mains.params.RandomParams;
import org.roach.margia.rules.RandomRule;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandomSimple implements Algorithm<RandomParams> {

    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams algParams) {
        var rParams = (RandomParams) algParams;
        var musicians = new ArrayList<Musician>();
        for (int i = 0; i < rParams.numMusicians; i++) {
            var m = new Musician(i % mainParams.numChannels, new RandomRule());
            m.setKey(rParams.key);
            musicians.add(m);
        }

        /*
         * @formatter:off
         *  0 -> 1
         * @formatter:on
         */
        musicians.get(0).addPeer(musicians.get(1));
        musicians.get(1).addPeer(musicians.get(0));

        return musicians;
    }

    @Override
    public String command() {
        return "random_simple";
    }

    @Override
    public RandomParams createParams() {
        return new RandomParams();
    }

    @Override
    public String displayName() {
        return "Random Simple";
    }
}
