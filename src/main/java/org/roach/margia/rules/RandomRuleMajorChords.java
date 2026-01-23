package org.roach.margia.rules;

import java.util.List;

import org.roach.margia.Chord;
import org.roach.margia.Musician;
import org.roach.margia.actions.*;
import org.roach.margia.storage.RuleOptions;
import org.roach.margia.ui.ChordFlavor;

/**
 * Random-note generator
 */
public class RandomRuleMajorChords extends AbstractMusicianRule {
    private static final String FLAVOR_PROPERTY = "flavor";
    private ChordFlavor flavor = ChordFlavor.MAJOR;

    @Override
    public void calculateAction(long tick) {
        if (musician.getChordsIvePlayed() >= 5) {
            logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
            actionsToTake.add(new RestOneTick(musician));
            actionsToTake.add(new ResetPlayedChords(musician));
            return;
        }
        if (musician.getQueueSize() == 0) {
            logger.atDebug().log("{} queue is empty", musician.getId());
            actionsToTake.add(new PlayPseudoRandomChordFlavor(musician, 17, 15, flavor));
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
    public String getName() { return "randomFlavor"; }

    @Override
    public void reset() {
        // nothing to do
    }

    @Override
    public RandomRuleMajorChords copy() {
        return new RandomRuleMajorChords();
    }

    @Override
    public List<SettableParamDescription<?>> getSettableParameters() {
        return List.of(
        // @formatter:off
            new SettableParamDescription<ChordFlavor>(FLAVOR_PROPERTY, "Chord flavor", ChordFlavor.class, 0d, 100d, 1d)
            // @formatter:on
        );
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        var ruleOpts = ruleOptions.getRuleSpecificOptions();
        if (ruleOpts.containsKey(FLAVOR_PROPERTY))
            this.flavor = ChordFlavor.valueOf(ruleOptions.getRuleSpecificOptions().get(FLAVOR_PROPERTY).toString());

    }
}
