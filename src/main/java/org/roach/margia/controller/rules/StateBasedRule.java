package org.roach.margia.controller.rules;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private int sequenceLength = 1;

    private int sequenceCountdown;
    private MusicianState state;
    private final MusicianState startingState;
    private int tickCountdown;
    private int initialTickDelay;
    private final BlockingQueue<Chord> delayQueue = new LinkedBlockingQueue<>();

    /**
     * Constructor
     */
    public StateBasedRule() {
        MusicianState directRepeat = null;
        // @formatter:off
        AlwaysTransitionMusicianState upFourth = new AlwaysTransitionMusicianState("up 4th")
                .setActions(List.of(c -> new PlayChord(this.musician, c)
        ));
        var downFourth = new AlwaysTransitionMusicianState("down 4th")
                .setActions(List.of(c -> new PlayChordDownInterval(musician, c, 5)));
        var doubleSpeed = new AlwaysTransitionMusicianState("double speed")
                .setActions(List.of(c -> new PlayChordTwiceLength(musician, c)));
        var halfSpeed = new AlwaysTransitionMusicianState("half speed")
                .setActions(List.of(c -> new PlayChordHalfLength(musician, c)));
        var increaseVelocity = new AlwaysTransitionMusicianState("increase velocity")
                .setActions(List.of(c -> new PlayChordUpVelocity(musician, c, 15)));
        var decreaseVelocity = new AlwaysTransitionMusicianState("decrease velocity")
                .setActions(List.of(c -> new PlayChordDownVelocity(musician, c, 15)));
        directRepeat = new PseudoRandomMusicianState("direct repeat", 20)
                .withAction(c -> new PlayChord(musician, c))
                .withStateTransition(new NumericRange(0, 2), upFourth)
                .withStateTransition(new NumericRange(3, 5), downFourth)
                .withStateTransition(new NumericRange(6, 8), increaseVelocity)
                .withStateTransition(new NumericRange(9, 11), decreaseVelocity)
                .withStateTransition(new NumericRange(12, 14), halfSpeed)
                .withStateTransition(new NumericRange(15, 17), doubleSpeed);
        // @formatter:on
        upFourth.setToState(directRepeat);
        downFourth.setToState(directRepeat);
        increaseVelocity.setToState(directRepeat);
        decreaseVelocity.setToState(directRepeat);
        halfSpeed.setToState(directRepeat);
        doubleSpeed.setToState(directRepeat);
        this.state = directRepeat;
        this.startingState = directRepeat;
    }

    @Override
    @SuppressWarnings({ "java:S899", "java:S3776" })
    public void calculateAction(long tick) {
        if (initialTickDelay > 0) {
            initialTickDelay--;
            return;
        }
        if (tickCountdown > 0) {
            tickCountdown--;
        }
        if (tickCountdown > 0) {
            logger.atDebug().log("{}: tickCountdown={}, returning", musician.getId(), tickCountdown);
            return;
        }
        var message = musician.getNextMessageReceived();
        if (message != null && message instanceof Chord heardChord) {
            delayQueue.offer(heardChord);
        }

        var chord = delayQueue.poll();
        sequenceCountdown--;
        if (chord == null) {
            logger.atDebug().log("{}: note heard was null, returning", musician.getId());
            return;
        }
        tickCountdown = chord.getLength();

        for (var action : state.actions()) {
            logger.atDebug().log("{} ({}): {}", musician.getId(), state.name(), chord);
            actionsToTake.add(action.apply(chord));
        }
        if (sequenceCountdown <= 0) {
            MusicianState newState = state.transition(musician);
            if (!state.equals(newState))
                logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state.name(), newState.name());
            state = newState;
            sequenceCountdown = sequenceLength;
        } else {
            logger.atDebug().log("{}: sequence countdown = {}", musician.getId(), sequenceCountdown);
        }
    }

    /**
     * Decrement the current sequence length by 1, but not lower than 1
     */
    public void decrementSequenceLength() {
        this.sequenceLength--;
        if (this.sequenceLength < 1)
            this.sequenceLength = 1;
    }

    /**
     * Increment the current sequence length by 1
     */
    public void incrementSequenceLength() {
        this.sequenceLength++;
    }

    @Override
    public String getName() { return "statebased"; }

    @Override
    public void reset() {
        sequenceLength = 0;
        sequenceCountdown = 0;
        state = startingState;
        tickCountdown = 0;
        initialTickDelay = (int) Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .getRuleSpecificOptionOrDefault(INITIAL_TICK_DELAY_PROPERTY, 0);
        delayQueue.clear();
    }

    @Override
    public StateBasedRule copy() {
        var copy = new StateBasedRule();
        copy.initialTickDelay = this.initialTickDelay;
        copy.sequenceLength = this.sequenceLength;
        return copy;
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        this.initialTickDelay = (int) ruleOptions.getRuleSpecificOptionOrDefault(INITIAL_TICK_DELAY_PROPERTY, 0);
        this.sequenceLength = (int) ruleOptions.getRuleSpecificOptionOrDefault(SEQUENCE_LENGTH_PROPERTY, 1);
        ruleOptions.addChangeListener(INITIAL_TICK_DELAY_PROPERTY, this);
        ruleOptions.addChangeListener(SEQUENCE_LENGTH_PROPERTY, this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String key, Object newValue)) {
            switch (key) {
            case INITIAL_TICK_DELAY_PROPERTY:
                this.initialTickDelay = (int) newValue;
                break;
            case SEQUENCE_LENGTH_PROPERTY:
                this.sequenceLength = (int) newValue;
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
