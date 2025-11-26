package org.roach.midi_swarm.rules;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.roach.midi_swarm.MusicianRule;
import org.roach.midi_swarm.NoteInfo;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {
	private final int sequenceLength;
	private static final String DIRECT_REPEAT = "direct repeat";
	private static final String UP_MINOR_THIRD = "up m3";
	private static final String UP_FIFTH = "up p5";
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
			actionsToTake.add(() -> musician.playNote(note));
			break;
		case UP_MINOR_THIRD: {
			if (note.note() != -1) { // note a rest
				var thirdUp = musician.getKey().up(note.note(), 3);
				var newNote = note.withNote(thirdUp);
				logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
				actionsToTake.add(() -> musician.playNote(newNote));
			} else {
				actionsToTake.add(() -> musician.playNote(note));
			}
			break;
		}
		case UP_FIFTH: {
			if (note.note() != -1) { // not a rest
				var fifthUp = musician.getKey().down(note.note(), 2);
				var newNote = note.withNote(fifthUp);
				logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
				actionsToTake.add(() -> musician.playNote(newNote));
			} else {
				actionsToTake.add(() -> musician.playNote(note));
			}
			break;
		}
		default:
			throw new IllegalStateException("Bad state: " + state);
		}

		if (sequenceCountdown == 0) {
			var newState = switch (state) {
			case DIRECT_REPEAT -> UP_MINOR_THIRD;
			case UP_MINOR_THIRD -> UP_FIFTH;
			case UP_FIFTH -> UP_MINOR_THIRD;
			default -> throw new IllegalStateException("No such state: " + state);
			};
			logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state, newState);
			state = newState;
			sequenceCountdown = sequenceLength;
		}
	}

}
