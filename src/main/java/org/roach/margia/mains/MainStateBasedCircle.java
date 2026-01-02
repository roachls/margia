package org.roach.margia.mains;

import java.util.ArrayList;
import java.util.List;

import org.roach.margia.*;
import org.roach.margia.mains.params.CircleParams;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;

/**
 * State-based agents in a circle
 */
public class MainStateBasedCircle implements Algorithm<CircleParams> {
    @SuppressWarnings("java:S106")
    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams margiaParams) {
        var algParams = (CircleParams) margiaParams;
        var musicians = new ArrayList<Musician>();
        MusicianRule rule = new RandomRule();
        var musician = new Musician(0, 0, rule);
        musicians.add(musician);

        for (int i = 1; i < algParams.numMusicians; i++) {
            rule = new StateBasedRule(algParams.startingSequenceLength, algParams.tickDelay);
            musician = new Musician(i, i % mainParams.numChannels, rule);
            musician.setKey(algParams.key);
            rule.setMusician(musician);
            musicians.add(musician);
        }

        /*
		 * @formatter:off
		 *  0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8
		 *  8 -> 0
		 * @formatter:on
		 */
        for (var i = 0; i < algParams.numMusicians - 1; i++) {
            musicians.get(i).addPeer(musicians.get(i + 1));
        }
        musicians.get(algParams.numMusicians - 1).addPeer(musicians.get(0));

        return musicians;
    }

    @Override
    public String command() { return "circle"; }

    @Override
    public CircleParams createParams() {
        return new CircleParams();
    }

    @Override
    public String displayName() {
        return "State-based circle";
    }

}
