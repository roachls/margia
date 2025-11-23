package org.roach.midi_swarm.rules;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.roach.midi_swarm.MusicianRule;
import org.roach.midi_swarm.NoteInfo;
import org.roach.midi_swarm.random.DieRoller;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {
	private final int sequenceLength;
	private static final String DIRECT_REPEAT = "direct repeat";
	private static final String UP_FOURTH = "up a 4th";
	private static final String DOWN_FOURTH = "down a third";
	private int sequenceCountdown;
	private String state = DIRECT_REPEAT;
	private int tickCountdown;
	private int initialTickDelay;
	private final BlockingQueue<NoteInfo> delayQueue = new LinkedBlockingQueue<>();

	/**
	 * @param sequenceLength sequence length
	 * @param tickDelay number of ticks to delay before repeating a sequence
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
		case UP_FOURTH: {
			if (note.note() != -1) { // note a rest
				var fourthUp = note.note() + 5;
				if (fourthUp > 127)
					fourthUp -= 128;
				var newNote = new NoteInfo(fourthUp, note.velocity(), note.length());
				logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
				actionsToTake.add(() -> musician.playNote(newNote));
			} else {
				actionsToTake.add(() -> musician.playNote(note));
			}
			break;
		}
		case DOWN_FOURTH: {
			if (note.note() != -1) { // not a rest
				var fourthDown = note.note() - 5;
				if (fourthDown < 0)
					fourthDown += 128;
				var newNote = new NoteInfo(fourthDown, note.velocity(), note.length());
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
			var newState = DieRoller.rollDice("1d2") == 1 ? UP_FOURTH : DOWN_FOURTH;
			logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state, newState);
			state = newState;
			sequenceCountdown = sequenceLength;
		}
	}

}
