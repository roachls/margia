package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.PlayChord;
import org.roach.margia.controller.rules.states.AlwaysTransitionState;
import org.roach.margia.controller.rules.states.DelayedTransitionState;
import org.roach.margia.storage.Options;

/**
 * A state-machine based agent
 */
public class UpFourthRule extends AbstractMusicianRule {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";

    @Override
    public void initActionsAfterMusicianAssigned() {
        var sequenceLength = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 1);
        var upFourth = new AlwaysTransitionState("up 4th").withAction(c -> List.of(new PlayChord(this.musician, c)));
        var upFourthDelayed = new DelayedTransitionState("up 4th delayed", sequenceLength).withWrappedState(upFourth);
        upFourth.setToState(upFourthDelayed);
        this.startingState = state = upFourthDelayed;
    }

    @Override
    public String getName() { return "up4th"; }

    @Override
    public UpFourthRule copy() {
        return new UpFourthRule();
    }

}
