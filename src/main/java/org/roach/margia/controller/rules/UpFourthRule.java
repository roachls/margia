package org.roach.margia.controller.rules;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.actions.PlayChord;
import org.roach.margia.actions.PlayChordUpInterval;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.params.NumericParamDescription;
import org.roach.margia.storage.params.SettableParamDescription;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * A state-machine based agent
 */
public class UpFourthRule extends AbstractMusicianRule implements ChangeListener {
    private static final String SEQUENCE_LENGTH_PROPERTY = "sequenceLength";
    private static final String INITIAL_TICK_DELAY_PROPERTY = "initialTickDelay";
    private int sequenceLength = 1;

    private int sequenceCountdown;
    private int tickCountdown;
    private int initialTickDelay;
    private final BlockingQueue<Chord> delayQueue = new LinkedBlockingQueue<>();

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
        } else {
            delayQueue.offer(Musician.REST);
        }

        var chord = delayQueue.poll();
        // never play the same note twice
        var lastChord = musician.getMyLastChord();
        if (lastChord != null) {
            while (lastChord.equals(chord)) {
                chord = delayQueue.poll();
            }
        }
        sequenceCountdown--;
        if (chord == null) {
            logger.atDebug().log("{}: note heard was null, returning", musician.getId());
            return;
        }
        tickCountdown = chord.getLength();

        if (!Musician.REST.equals(chord)) { // note a rest
            actionsToTake.add(new PlayChordUpInterval(musician, chord, 6));
        } else {
            actionsToTake.add(new PlayChord(musician, chord));
        }

        if (sequenceCountdown <= 0) {
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

    /**
     * @return the current sequence length
     */
    public int getSequenceLength() { return sequenceLength; }

    /**
     * @param sequenceLength the sequence length
     */
    public void setSequenceLength(int sequenceLength) {
        if (sequenceLength < 1)
            throw new IllegalArgumentException("Sequence length must be at least 1");
        this.sequenceLength = sequenceLength;
        Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .setRuleSpecificOption(SEQUENCE_LENGTH_PROPERTY, this.sequenceLength);
    }

    @Override
    public String getName() { return "up4th"; }

    @Override
    public void reset() {
        sequenceLength = 0;
        sequenceCountdown = 0;
        tickCountdown = 0;
        initialTickDelay = 0;
        delayQueue.clear();
    }

    /**
     * @return the initialTickDelay
     */
    public int getInitialTickDelay() { return initialTickDelay; }

    /**
     * @param initialTickDelay the initialTickDelay to set
     */
    public void setInitialTickDelay(int initialTickDelay) {
        if (initialTickDelay < 0)
            throw new IllegalArgumentException("Tick delay must be at least 0");
        this.initialTickDelay = initialTickDelay;
        this.sequenceCountdown = sequenceLength + initialTickDelay;
        Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .setRuleSpecificOption(INITIAL_TICK_DELAY_PROPERTY, this.initialTickDelay);
    }

    @Override
    public UpFourthRule copy() {
        var copy = new UpFourthRule();
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
            new NumericParamDescription(INITIAL_TICK_DELAY_PROPERTY, "Initial delay", Integer.class, 0d, 100d, 1d, 0d),
            new NumericParamDescription(SEQUENCE_LENGTH_PROPERTY, "Sequence length", Integer.class, 1d, 1000d, 1d, 1d)
            // @formatter:on
        );
    }
}
