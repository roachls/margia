package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.PlayChord;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.storage.Options;

/**
 * A state-machine based agent
 */
public class RepeatRule extends AbstractMusicianRule {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";

    @Override
    public void initActionsAfterMusicianAssigned() {
        var sequenceLength = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 16);
        var directRepeat = new AlwaysTransitionState("direct repeat")
                .withAction(c -> List.of(new PlayChord(musician, c)));

        super.initActionsAfterMusicianAssigned();
        var sequenceCountdownState = new DelayedTransitionState("sequenceCountdown", sequenceLength)
                .withWrappedState(directRepeat);
        
        directRepeat.setToState(sequenceCountdownState);

        this.state = sequenceCountdownState;
        this.startingState = sequenceCountdownState;
    }

    @Override
    public String getName() { return "repeat"; }

    @Override
    public AbstractMusicianRule copy() {
        return new RepeatRule();
    }

}
