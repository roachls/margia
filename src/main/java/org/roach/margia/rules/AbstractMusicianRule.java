package org.roach.margia.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.*;
import org.roach.margia.actions.MusicalAction;

/**
 * Abstract implementation of {@link MusicianRule}
 */
public abstract non-sealed class AbstractMusicianRule implements MusicianRule {

    protected Musician musician;
    protected final Logger logger = LogManager.getLogger(getClass());
    protected List<MusicalAction> actionsToTake = new ArrayList<>();
    /**
     * Generates a rest of the given length in ticks
     */
    public static final IntFunction<NoteInfo> REST = l -> new NoteInfo(Note.REST, 0, l);

    /**
     * @param musician the {@link Musician} that this rule applies to
     */
    public void setMusician(Musician musician) { this.musician = musician; }

    /**
     * Do the actions previously calculated for a tick
     */
    public void doAction() {
        if (actionsToTake.isEmpty()) {
            logger.atDebug().log("{}: no actions to take", musician.getId());
            return;
        }
        for (var action : actionsToTake) {
            action.perform();
        }
        actionsToTake.clear();
    }
}
