package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.PlayChord;
import org.roach.margia.controller.rules.states.AlwaysTransitionState;
import org.roach.margia.controller.rules.states.DelayedTransitionState;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.params.IntegerParamDescription;
import org.roach.margia.storage.params.SettableParamDescription;

/**
 * A state-machine based agent
 */
public class UpFourthRule extends AbstractMusicianRule {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";
    private static final String INITIAL_TICK_DELAY_PROPERTY = "initialTickDelay";

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

    @Override
    public List<SettableParamDescription> getSettableParameters() {
        return List.of(
        // @formatter:off
            new IntegerParamDescription(INITIAL_TICK_DELAY_PROPERTY, "Initial delay", 0, 100, 1, 0),
            new IntegerParamDescription(SEQUENCE_LENGTH_PROPERTY, "Sequence length", 1, 1000, 1, 1)
            // @formatter:on
        );
    }
}
