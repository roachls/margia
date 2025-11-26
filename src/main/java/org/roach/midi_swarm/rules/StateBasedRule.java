package org.roach.midi_swarm.rules;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.roach.midi_swarm.MusicianRule;
import org.roach.midi_swarm.NoteInfo;
import org.roach.midi_swarm.actions.PlayNote;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {
	private final int sequenceLength;
	private static final String DIRECT_REPEAT = "direct repeat";
	private static final String UP_THIRD = "up third";
	private static final String DOWN_SECOND = "down second";
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
		var heardNote = musician.getNextNoteHeard();
		if (heardNote != null)
			delayQueue.offer(heardNote);

		var note = delayQueue.poll();
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
		case UP_THIRD: {
			if (note.noteNum() != -1) { // note a rest
				var thirdUp = musician.getKey().up(note.noteNum(), 3);
				var newNote = note.withNote(thirdUp);
				logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
				actionsToTake.add(new PlayNote(musician, newNote));
			} else {
				actionsToTake.add(new PlayNote(musician, note));
			}
			break;
		}
		case DOWN_SECOND: {
			if (note.noteNum() != -1) { // not a rest
				var fifthUp = musician.getKey().down(note.noteNum(), 2);
				var newNote = note.withNote(fifthUp);
				logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
				actionsToTake.add(new PlayNote(musician, newNote));
			} else {
				actionsToTake.add(new PlayNote(musician, note));
			}
			break;
		}
		default:
			throw new IllegalStateException("Bad state: " + state);
		}

		if (sequenceCountdown == 0) {
			var newState = switch (state) {
			case DIRECT_REPEAT -> UP_THIRD;
			case UP_THIRD -> DOWN_SECOND;
			case DOWN_SECOND -> UP_THIRD;
			default -> throw new IllegalStateException("No such state: " + state);
			};
			logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state, newState);
			state = newState;
			sequenceCountdown = sequenceLength;
		}
	}

}
