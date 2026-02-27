package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.states.NeverTransitionState;
import org.roach.margia.model.*;
import org.roach.margia.view.ChordFlavor;

/**
 * Random-note generator
 */
public class RandomFlavorRule extends AbstractMusicianRule {
    private static final String FLAVOR_PROPERTY = "flavor";
    private ChordFlavor flavor = ChordFlavor.MAJOR;
    private int maxChordStringLength = 5;
    private int restsBetweenChordStrings = 1;

    private class RandomTransition implements Function<MusicianMessage, List<MusicalAction>> {

        @Override
        public List<MusicalAction> apply(MusicianMessage t) {
            var list = new ArrayList<MusicalAction>();
            if (musician.getChordsIvePlayed() >= maxChordStringLength) {
                logger.atDebug().log("{}: resting because I've played {} notes", musician.getId(),
                        musician.getChordsIvePlayed());
                for (var i = 0; i < restsBetweenChordStrings; i++) {
                    list.add(new RestOneTick(musician));
                }
                list.add(new ResetPlayedChords(musician));
                return list;
            }
            if (musician.getQueueSize() == 0) {
                logger.atDebug().log("{} queue is empty", musician.getId());
                list.add(new PlayPseudoRandomChordFlavor(musician, 17, 15, flavor));
                return list;
            }

            var heardNote = musician.getNextMessageReceived();
            // never play the same note twice
            var lastNote = musician.getMyLastChord();
            if (lastNote != null) {
                while (lastNote.equals(heardNote)) {
                    heardNote = musician.getNextMessageReceived();
                }
            }
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
    public String getName() { return "randomFlavor"; }

    @Override
    public RandomFlavorRule copy() {
        return new RandomFlavorRule();
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        this.flavor = ChordFlavor
                .valueOf(ruleOptions.getRuleSpecificOptionOrDefault(FLAVOR_PROPERTY, ChordFlavor.MAJOR).toString());
    }
}
