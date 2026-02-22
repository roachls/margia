package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.states.NeverTransitionState;
import org.roach.margia.model.*;
import org.roach.margia.storage.params.IntegerParamDescription;
import org.roach.margia.storage.params.SettableParamDescription;

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
                list.add(new PlayPseudoRandomChord(musician, 17, 15, chordSize));
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
    public String getName() { return "random"; }

    @Override
    public RandomRule copy() {
        return new RandomRule();
    }

    @Override
    public List<SettableParamDescription> getSettableParameters() {
        return List.of(
                new IntegerParamDescription("maxChordStringLength", "Max chords to play before resting", 1,
                        Integer.MAX_VALUE, 1, 5),
                new IntegerParamDescription("restsBetweenChordStrings", "Rest between chord strings", 0, 1000, 1, 1),
                new IntegerParamDescription("chordSize", "Number of notes per chord", 1, 5, 1, 1));
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        maxChordStringLength = (int) ruleOptions.getRuleSpecificOptionOrDefault("maxChordStringLength", 5);
        restsBetweenChordStrings = (int) ruleOptions.getRuleSpecificOptionOrDefault("restsBetweenChordStrings", 1);
        chordSize = (int) ruleOptions.getRuleSpecificOptionOrDefault("chordSize", 1);
    }
}
