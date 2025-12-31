package org.roach.margia.rules;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.roach.margia.MusicianRule;
import org.roach.margia.NoteInfo;
import org.roach.margia.actions.*;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {
    private int sequenceLength;
    private static final String DIRECT_REPEAT = "direct repeat";
    private static final String UP_FOURTH = "up 4th";
    private static final String DOWN_FOURTH = "down 4th";
    private static final String DOUBLE_SPEED = "double speed";
    private static final String HALF_SPEED = "half speed";
    private static final String INCREASE_VELOCITY = "increase velocity";
    private static final String DECREASE_VELOCITY = "decrease velocity";

    private int sequenceCountdown;
    private String state = DIRECT_REPEAT;
    private int tickCountdown;
    private int initialTickDelay;
    private final BlockingQueue<NoteInfo> delayQueue = new LinkedBlockingQueue<>();

    /**
     * @param sequenceLength sequence length
     * @param tickDelay      number of ticks to delay before repeating a sequence
     */
    public StateBasedRule(final int sequenceLength, final int tickDelay) {
        if (sequenceLength < 1)
            throw new IllegalArgumentException("Sequence length must be at least 1");
        if (tickDelay < 0)
            throw new IllegalArgumentException("Tick delay must be at least 0");
        this.sequenceLength = sequenceLength;
        this.sequenceCountdown = sequenceLength + tickDelay;
        this.initialTickDelay = tickDelay;
    }

    @Override
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
        var message = musician.getNextNoteHeard();
        if (message != null && message instanceof NoteInfo heardNote) {
            delayQueue.offer(heardNote);
        } else {
            delayQueue.offer(REST.apply(1));
        }

        var note = delayQueue.poll();
        // never play the same note twice
        var lastNote = musician.getMyLastNote();
        if (lastNote != null) {
            while (lastNote.equals(note)) {
                note = delayQueue.poll();
            }
        }
        sequenceCountdown--;
        if (note == null) {
            logger.atDebug().log("{}: note heard was null, returning", musician.getId());
            return;
        }
        tickCountdown = note.length();

        switch (state) {
        case DIRECT_REPEAT:
            logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, note);
            actionsToTake.add(new PlayNote(musician, note));
            break;
        case UP_FOURTH: {
            if (note.noteNum() != -1) { // note a rest
                actionsToTake.add(new PlayNoteUpInterval(musician, note, 5));
            } else {
                actionsToTake.add(new PlayNote(musician, note));
            }
            break;
        }
        case DOWN_FOURTH: {
            if (note.noteNum() != -1) { // not a rest
                actionsToTake.add(new PlayNoteDownInterval(musician, note, 5));
            } else {
                actionsToTake.add(new PlayNote(musician, note));
            }
            break;
        }
        case HALF_SPEED: {
            logger.atDebug().log("{} ({}): playing note half length {}", musician.getId(), state, note);
            actionsToTake.add(new PlayNoteHalfLength(musician, note));
            break;
        }
        case DOUBLE_SPEED: {
            logger.atDebug().log("{} ({}): playing note double length {}", musician.getId(), state, note);
            actionsToTake.add(new PlayNoteTwiceLength(musician, note));
            break;
        }
        case INCREASE_VELOCITY: {
            logger.atDebug().log("{} ({}): playing note with increased velocity {}", musician.getId(), state, note);
            actionsToTake.add(new PlayNoteUpVelocity(musician, note, 15));
            break;
        }
        case DECREASE_VELOCITY: {
            logger.atDebug().log("{} ({}): playing note with decreased velocity {}", musician.getId(), state, note);
            actionsToTake.add(new PlayNoteDownVelocity(musician, note, 15));
            break;
        }
        default:
            throw new IllegalStateException("Bad state: " + state);
        }

        if (sequenceCountdown <= 0) {
            var newState = switch (state) {
            case DIRECT_REPEAT -> {
                var rand = tick + musician.getId();
                if (lastNote != null) {
                    rand += lastNote.noteNum();
                }
                rand %= 30;
                logger.atDebug().log("{}: 'random' number: {}", musician.getId(), rand);
                if (rand >= 1 && rand <= 3)
                    yield UP_FOURTH;
                else if (rand >= 4 && rand <= 6)
                    yield DOWN_FOURTH;
                else if (rand >= 7 && rand <= 8)
                    yield HALF_SPEED;
                else if (rand >= 9 && rand <= 10)
                    yield DOUBLE_SPEED;
                else if (rand >= 11 && rand <= 12)
                    yield INCREASE_VELOCITY;
                else if (rand >= 13 && rand <= 14)
                    yield DECREASE_VELOCITY;
                else
                    yield DIRECT_REPEAT;
            }
            case UP_FOURTH, DOWN_FOURTH, HALF_SPEED, DOUBLE_SPEED, INCREASE_VELOCITY, DECREASE_VELOCITY ->
                DIRECT_REPEAT;
            default -> throw new IllegalStateException("No such state: " + state);
            };
            if (!state.equals(newState))
                logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state, newState);
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

    /**
     * @return the current sequence length
     */
    public int getSequenceLength() { return sequenceLength; }
}
