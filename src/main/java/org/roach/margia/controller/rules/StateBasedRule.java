package org.roach.margia.controller.rules;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.model.Chord;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.params.IntegerParamDescription;
import org.roach.margia.storage.params.SettableParamDescription;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends AbstractMusicianRule implements ChangeListener {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";
    private static final String INITIAL_TICK_DELAY_PROPERTY = "initialTickDelay";
    private MusicianState state;
    private MusicianState startingState;

    @Override
    public void initActionsAfterMusicianAssigned() {
        var sequenceLength = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 1);
        var upFourth = new AlwaysTransitionMusicianState("up 4th")
                .setActions(List.of(c -> new PlayChord(this.musician, c)));
        var upFourthDelayed = new DelayedTransitionState("up 4th delayed", sequenceLength, upFourth);
        var downFourth = new AlwaysTransitionMusicianState("down 4th")
                .setActions(List.of(c -> new PlayChordDownInterval(musician, c, 5)));
        var downFourthDelayed = new DelayedTransitionState("down 4th delayed", sequenceLength, downFourth);
        var doubleSpeed = new AlwaysTransitionMusicianState("double speed")
                .setActions(List.of(c -> new PlayChordTwiceLength(musician, c)));
        var doubleSpeedDelayed = new DelayedTransitionState("double speed delayed", sequenceLength, doubleSpeed);
        var halfSpeed = new AlwaysTransitionMusicianState("half speed")
                .setActions(List.of(c -> new PlayChordHalfLength(musician, c)));
        var halfSpeedDelayed = new DelayedTransitionState("half speed delayed", sequenceLength, halfSpeed);
        var increaseVelocity = new AlwaysTransitionMusicianState("increase velocity")
                .setActions(List.of(c -> new PlayChordUpVelocity(musician, c, 15)));
        var increaseVelocityDelayed = new DelayedTransitionState("increase velocity delayed", sequenceLength,
                increaseVelocity);
        var decreaseVelocity = new AlwaysTransitionMusicianState("decrease velocity")
                .setActions(List.of(c -> new PlayChordDownVelocity(musician, c, 15)));
        var decreaseVelocityDelayed = new DelayedTransitionState("decrease velocity delayed", sequenceLength,
                decreaseVelocity);
        var directRepeat = new PseudoRandomMusicianState("direct repeat", 30)
                .withAction(c -> new PlayChord(musician, c));

        super.initActionsAfterMusicianAssigned();
        var sequenceCountdownState = new DelayedTransitionState("sequenceCountdown", sequenceLength, directRepeat);
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
        if (message instanceof Chord chord)
            state.doActions(musician, this, chord);
        MusicianState newState = state.transition(musician);
        if (!state.equals(newState)) {
            logger.atInfo().log("{} ({}): switching to {}", musician.getId(), state.name(), newState.name());
            if (musician.getId() == 0)
                System.out.printf("Switching to %s%n", newState.name());
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

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        ruleOptions.addChangeListener(INITIAL_TICK_DELAY_PROPERTY, this);
        ruleOptions.addChangeListener(SEQUENCE_LENGTH_PROPERTY, this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String key, Object newValue)) {
            switch (key) {
            case INITIAL_TICK_DELAY_PROPERTY:
//                this.initialTickDelay = (int) newValue;
                break;
            case SEQUENCE_LENGTH_PROPERTY:
//                this.sequenceLength = (int) newValue;
                break;
            default:
                break;
            }
        }

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
