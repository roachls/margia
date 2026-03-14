package org.roach.margia.controller.rules;

import java.util.ArrayList;
import java.util.List;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.states.MusicianState;
import org.roach.margia.model.RuleOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link MusicianRule}
 */
public abstract non-sealed class AbstractMusicianRule implements MusicianRule {

    protected Musician musician;
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected List<MusicalAction> actionsToTake = new ArrayList<>();
    protected MusicianState state;
    protected MusicianState startingState;

    /**
     * @param musician the {@link Musician} that this rule applies to
     */
    public void setMusician(Musician musician) { this.musician = musician; }

    /**
     * Do the actions previously calculated for a tick
     */
    public void doAction() {
        if (actionsToTake.isEmpty()) {
            logger.atDebug().setMessage("{}: no actions to take").addArgument(musician.getId()).log();
            return;
        }
        for (var action : actionsToTake) {
            logger.atDebug().setMessage("{}: performing action {}").addArgument(musician.getId())
                    .addArgument(() -> action.getClass().getSimpleName()).log();
            action.perform();
        }
        actionsToTake.clear();
    }

    /**
     * @return a copy of this rule
     */
    public abstract AbstractMusicianRule copy();

    /**
     * Subclasses should override in order to apply specific parameters
     * 
     * @param ruleOptions rule options
     */
    public void restoreFromStorage(RuleOptions ruleOptions) {
        // nothing here
    }

    @Override
    public void initActionsAfterMusicianAssigned() {
        // to be implemented by children
    }

    /**
     * @param action a {@link MusicalAction} to add to the list of actions to take
     *               this tick
     */
    public void addActionToTake(MusicalAction action) {
        actionsToTake.add(action);
    }

    @Override
    public void reset() {
        state = startingState;
    }

    @Override
    @SuppressWarnings({ "java:S899", "java:S3776" })
    public void calculateAction(long tick) {
        var message = musician.getNextMessageReceived();
        state.doActions(musician, this, message);
        MusicianState newState = state.transition(musician);
        if (!state.equals(newState)) {
            logger.atInfo().setMessage("{} ({}): switching to {}").addArgument(musician.getId())
                    .addArgument(state.name()).addArgument(newState.name()).log();
        }
        state = newState;
    }

}
