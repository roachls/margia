package org.roach.margia.rules;

import org.roach.margia.actions.*;

/**
 * Random-note generator
 */
public class RandomRule extends AbstractMusicianRule {

    @Override
    public void calculateAction(long tick) {
        if (musician.getNotesIvePlayed() >= 5) {
            logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
            actionsToTake.add(new PlayNote(musician, REST.apply(1)));
            actionsToTake.add(new ResetPlayedNotes(musician));
            return;
        }
        if (musician.getQueueSize() == 0) {
            logger.atDebug().log("{} queue is empty", musician.getId());
            actionsToTake.add(new PlayPseudoRandomNote(musician, 17, 5));
            return;
        }

        var heardNote = musician.getNextNoteHeard();
        // never play the same note twice
        var lastNote = musician.getMyLastNote();
        if (lastNote != null) {
            while (lastNote.equals(heardNote)) {
                heardNote = musician.getNextNoteHeard();
            }
        }
        var note = heardNote; // need a final version for lambdas
        logger.atDebug().log("{}: heard {}", musician.getId(), note);
        if (note == null || note.equals(REST.apply(1))) {
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
}
