package org.roach.margia.controller.rules;

import java.util.List;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.storage.Options;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends AbstractMusicianRule {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";
    private static final String INITIAL_TICK_DELAY_PROPERTY = "initialTickDelay";
    private MusicianState state;
    private MusicianState startingState;

    @Override
    public void initActionsAfterMusicianAssigned() {
        var sequenceLength = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 1);
        var upFourth = new AlwaysTransitionState("up 4th").withAction(c -> List.of(new PlayChord(this.musician, c)));
        var upFourthDelayed = new DelayedTransitionState("up 4th delayed", sequenceLength).withWrappedState(upFourth);
        var downFourth = new AlwaysTransitionState("down 4th")
                .withAction(c -> List.of(new PlayChordDownInterval(musician, c, 5)));
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
        var increaseVelocity = new AlwaysTransitionState("increase velocity")
                .withAction(c -> List.of(new PlayChordUpVelocity(musician, c, 15)));
        var increaseVelocityDelayed = new DelayedTransitionState("increase velocity delayed", sequenceLength)
                .withWrappedState(increaseVelocity);
        var decreaseVelocity = new AlwaysTransitionState("decrease velocity")
                .withAction(c -> List.of(new PlayChordDownVelocity(musician, c, 15)));
        var decreaseVelocityDelayed = new DelayedTransitionState("decrease velocity delayed", sequenceLength)
                .withWrappedState(decreaseVelocity);
        var directRepeat = new PseudoRandomState("direct repeat", 30)
                .withAction(c -> List.of(new PlayChord(musician, c)));

        super.initActionsAfterMusicianAssigned();
        var sequenceCountdownState = new DelayedTransitionState("sequenceCountdown", sequenceLength)
                .withWrappedState(directRepeat);
        var tickCountdownState = new CountdownState("tickDelay", (int) Options.getInstance().getMusicians()
                .get(musician.getId()).getRuleOptions().getRuleSpecificOptionOrDefault(INITIAL_TICK_DELAY_PROPERTY, 0),
                sequenceCountdownState);
        upFourth.setToState(sequenceCountdownState);
        downFourth.setToState(sequenceCountdownState);
        increaseVelocity.setToState(sequenceCountdownState);
        decreaseVelocity.setToState(sequenceCountdownState);
        halfSpeed.setToState(sequenceCountdownState);
        doubleSpeed.setToState(sequenceCountdownState);

        directRepeat = directRepeat.withStateTransition(new NumericRange(1, 3), upFourthDelayed)
                .withStateTransition(new NumericRange(4, 6), downFourthDelayed)
                .withStateTransition(new NumericRange(7, 8), increaseVelocityDelayed)
                .withStateTransition(new NumericRange(9, 10), decreaseVelocityDelayed)
                .withStateTransition(new NumericRange(11, 12), halfSpeedDelayed)
                .withStateTransition(new NumericRange(13, 14), doubleSpeedDelayed);
        this.state = tickCountdownState;
        this.startingState = tickCountdownState;
    }

    @Override
    @SuppressWarnings({ "java:S899", "java:S3776" })
    public void calculateAction(long tick) {
        var message = musician.getNextMessageReceived();
        state.doActions(musician, this, message);
        MusicianState newState = state.transition(musician);
        if (!state.equals(newState)) {
            logger.atInfo().log("{} ({}): switching to {}", musician.getId(), state.name(), newState.name());
        }
        state = newState;
    }

    @Override
    public String getName() { return "statebased"; }

    @Override
    public void reset() {
        state = startingState;
    }

    @Override
    public StateBasedRule copy() {
        return new StateBasedRule();
    }

}
