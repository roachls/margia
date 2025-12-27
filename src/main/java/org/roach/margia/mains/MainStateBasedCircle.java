package org.roach.margia.mains;

import java.util.ArrayList;
import java.util.List;

import org.roach.margia.*;
import org.roach.margia.rules.RandomRule;
import org.roach.margia.rules.StateBasedRule;

/**
 * State-based agents in a circle
 */
public class MainStateBasedCircle implements Algorithm<StateBasedParams> {
    @SuppressWarnings("java:S106")
    @Override
    public List<Musician> initMusicians(MainParams mainParams, MargiaParams margiaParams, MidiController controller) {
        var sbParams = (StateBasedParams) margiaParams;
        var musicians = new ArrayList<Musician>();
        MusicianRule rule = new RandomRule();
        var musician = new Musician(0, controller, 0, rule);
        musicians.add(musician);

        var numMusicians = sbParams.numCols * sbParams.numCols;
        for (int i = 1; i < numMusicians; i++) {
            rule = new StateBasedRule(8, 0);
            musician = new Musician(i, controller, i % mainParams.numChannels, rule);
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

        return musicians;
    }

    @Override
    public String command() { return "circle"; }

    @Override
    public StateBasedParams createParams() {
        return new StateBasedParams();
    }

    @Override
    public String displayName() {
        return "State-based circle";
    }

}
