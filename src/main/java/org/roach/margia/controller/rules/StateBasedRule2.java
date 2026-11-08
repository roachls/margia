package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.storage.Options;

/**
 * A state-machine based agent
 */
public class StateBasedRule2 extends AbstractMusicianRule {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";
    private static final String INITIAL_TICK_DELAY_PROPERTY = "initialTickDelay";

    @Override
    public void initActionsAfterMusicianAssigned() {
        var sequenceLength = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 16);
        var upThird = new PseudoRandomState("up 3rd", 100).withAction(c -> List.of(new PlayChordUpInterval(musician, c, 3)));
        var upThirdDelayed = new DelayedTransitionState("up 3rd delayed", sequenceLength).withWrappedState(upThird);
        var downFourth = new AlwaysTransitionState("down 4th")
                .withAction(c -> List.of(new PlayChordDownInterval(musician, c, 4)));
        var downFourthDelayed = new DelayedTransitionState("down 4th delayed", sequenceLength)
                .withWrappedState(downFourth);
        var doubleSpeed = new AlwaysTransitionState("double speed")
                .withAction(c -> List.of(new PlayChordTwiceLength(musician, c)));
        var doubleSpeedDelayed = new DelayedTransitionState("double speed delayed", sequenceLength)
                .withWrappedState(doubleSpeed);
        var halfSpeed = new AlwaysTransitionState("half speed")
                .withAction(c -> List.of(new PlayChordHalfLength(musician, c)));
        var halfSpeedDelayed = new DelayedTransitionState("half speed delayed", sequenceLength)
                .withWrappedState(halfSpeed);

        super.initActionsAfterMusicianAssigned();
        var sequenceCountdownState = new DelayedTransitionState("sequenceCountdown", sequenceLength)
                .withWrappedState(upThird);
        var tickCountdownState = new CountdownState("tickDelay", (int) Options.getInstance().getMusicians()
                .get(musician.getId()).getRuleOptions().getRuleSpecificOptionOrDefault(INITIAL_TICK_DELAY_PROPERTY, 0),
                sequenceCountdownState);
        downFourth.setToState(sequenceCountdownState);
        halfSpeed.setToState(sequenceCountdownState);
        doubleSpeed.setToState(sequenceCountdownState);

        upThird = upThird.withStateTransition(new NumericRange(0, 25), upThirdDelayed)
                .withStateTransition(new NumericRange(26, 50), downFourthDelayed)
                .withStateTransition(new NumericRange(51, 75), halfSpeedDelayed)
                .withStateTransition(new NumericRange(76, 99), doubleSpeedDelayed);
        this.state = tickCountdownState;
        this.startingState = tickCountdownState;
    }

    @Override
    public String getName() { return "statebased2"; }

    @Override
    public AbstractMusicianRule copy() {
        return new StateBasedRule2();
    }

}
