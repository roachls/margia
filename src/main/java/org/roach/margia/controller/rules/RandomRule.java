package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.states.NeverTransitionState;
import org.roach.margia.model.*;

/**
 * Random-note generator
 */
public class RandomRule extends AbstractMusicianRule {
    private int maxChordStringLength = 5;
    private int restsBetweenChordStrings = 1;
    private int chordSize = 1;

    private class RandomTransition implements Function<MusicianMessage, List<MusicalAction>> {

        @Override
        public List<MusicalAction> apply(MusicianMessage t) {
            var list = new ArrayList<MusicalAction>();
            if (musician.getChordsIvePlayed() >= maxChordStringLength) {
                logger.atDebug().setMessage("{}: resting because I've played {} notes").addArgument(musician.getId())
                        .addArgument(musician.getChordsIvePlayed()).log();
                for (var i = 0; i < restsBetweenChordStrings; i++) {
                    list.add(new RestOneTick(musician));
                }
                list.add(new ResetPlayedChords(musician));
                return list;
            }
            if (musician.getQueueSize() == 0) {
                logger.atDebug().setMessage("{} queue is empty").addArgument(musician.getId()).log();
                list.add(new PlayPseudoRandomChord(musician, 17, 15, chordSize));
                return list;
            }

            var heardNote = musician.getNextMessageReceived(true);
            logger.atDebug().log("{}: heard {}", musician.getId(), heardNote);
            if (heardNote == null || heardNote instanceof Chord chord && Musician.REST.equals(chord)) {
                logger.atDebug().log("{}: heard null or rest, returning");
            }
            return Collections.emptyList();
        }

    }

    @Override
    public void initActionsAfterMusicianAssigned() {
        super.initActionsAfterMusicianAssigned();
        var randomState = new NeverTransitionState("random").withAction(new RandomTransition());
        this.startingState = this.state = randomState;
    }

    @Override
    public String getName() { return "random"; }

    @Override
    public RandomRule copy() {
        return new RandomRule();
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        maxChordStringLength = (int) ruleOptions.getRuleSpecificOptionOrDefault("maxChordStringLength", 5);
        restsBetweenChordStrings = (int) ruleOptions.getRuleSpecificOptionOrDefault("restsBetweenChordStrings", 1);
        chordSize = (int) ruleOptions.getRuleSpecificOptionOrDefault("chordSize", 1);
    }
}
