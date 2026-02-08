package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.*;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.params.IntegerParamDescription;
import org.roach.margia.storage.params.SettableParamDescription;

/**
 * Random-note generator
 */
public class RandomRule extends AbstractMusicianRule {
    private int maxChordStringLength = 5;
    private int restsBetweenChordStrings = 1;

    @Override
    public void calculateAction(long tick) {
        if (musician.getChordsIvePlayed() >= maxChordStringLength) {
            logger.atDebug().log("{}: resting because I've played {} notes", musician.getId(),
                    musician.getChordsIvePlayed());
            for (int i = 0; i < restsBetweenChordStrings; i++) {
                actionsToTake.add(new RestOneTick(musician));
            }
            actionsToTake.add(new ResetPlayedChords(musician));
            return;
        }
        if (musician.getQueueSize() == 0) {
            logger.atDebug().log("{} queue is empty", musician.getId());
            actionsToTake.add(new PlayPseudoRandomChord(musician, 17, 15, 1));
            return;
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

    }

    @Override
    public String getName() { return "random"; }

    @Override
    public void reset() {
        // nothing to do
    }

    @Override
    public RandomRule copy() {
        return new RandomRule();
    }

    @Override
    public List<SettableParamDescription> getSettableParameters() {
        return List.of(
                new IntegerParamDescription("maxChordStringLength", "Max chords to play before resting", 1,
                        Integer.MAX_VALUE, 1, 5),
                new IntegerParamDescription("restsBetweenChordStrings", "Rest between chord strings", 0, 1000, 1, 1));
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        maxChordStringLength = (int) ruleOptions.getRuleSpecificOptionOrDefault("maxChordStringLength", 5);
        restsBetweenChordStrings = (int) ruleOptions.getRuleSpecificOptionOrDefault("restsBetweenChordStrings", 1);
    }
}
