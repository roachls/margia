package org.roach.midi_swarm.rules;

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
	
	/**
	 * @param sequenceLength sequence length
	 */
	public StateBasedRule(final int sequenceLength) {
		this.sequenceLength = sequenceLength;
		this.sequenceCountdown = sequenceLength;
	}

	@Override
	public void calculateAction(long tick) {
		if (tickCountdown > 0) {
			tickCountdown--;
		}
		if (tickCountdown > 0) {
			logger.atDebug().log("{}: tickCountdown={}, returning", musician.getId(), tickCountdown);
			return;
		}
		var note = musician.getNextNoteHeard();
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
			var fourthUp = Math.min(127, note.note() + 5);
			var newNote = new NoteInfo(fourthUp, note.velocity(), note.length());
			logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
			actionsToTake.add(() -> musician.playNote(newNote));
			break;
		}
		case DOWN_FOURTH: {
			var fourthDown = Math.max(note.note() - 5, 0);
			var newNote = new NoteInfo(fourthDown, note.velocity(), note.length());
			logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
			actionsToTake.add(() -> musician.playNote(newNote));
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
