package org.roach.midi_swarm.rules;

import java.awt.image.DirectColorModel;

import org.roach.midi_swarm.MusicianRule;
import org.roach.midi_swarm.NoteInfo;
import org.roach.midi_swarm.random.DieRoller;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {
	private static final int SEQUENCE_LENGTH = 4;
	private static final String DIRECT_REPEAT = "direct repeat";
	private static final String UP_FOURTH = "up a 4th";
	private static final String DOWN_THIRD = "down a third";
	private int sequenceCountdown = SEQUENCE_LENGTH;
	private String state = DIRECT_REPEAT;
	private int tickCountdown;

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
		if (note == null) {
			logger.atDebug().log("{}: note heard was null, returning", musician.getId());
			return;
		}
		tickCountdown = note.noteInfo().length();
		sequenceCountdown--;

		switch (state) {
		case DIRECT_REPEAT:
			logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, note.noteInfo());
			actionsToTake.add(() -> musician.playNote(note.noteInfo()));
			break;
		case UP_FOURTH: {
			var fourthUp = Math.min(127, note.noteInfo().note() + 5);
			var newNote = new NoteInfo(fourthUp, note.noteInfo().velocity(), note.noteInfo().length());
			logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
			actionsToTake.add(() -> musician.playNote(newNote));
			break;
		}
		case DOWN_THIRD: {
			var thirdDown = Math.max(note.noteInfo().note() - 3, 0);
			var newNote = new NoteInfo(thirdDown, note.noteInfo().velocity(), note.noteInfo().length());
			logger.atDebug().log("{} ({}): playing note {}", musician.getId(), state, newNote);
			actionsToTake.add(() -> musician.playNote(newNote));
			break;
		}
		default:
			throw new IllegalStateException("Bad state: " + state);
		}

		if (sequenceCountdown == 0) {
			var newState = DieRoller.rollDice("1d2") == 1 ? UP_FOURTH : DOWN_THIRD;
//			var newState = (tick + musician.getMyLastNote().note() % 2 + musician.getId() == 0) ? UP_FOURTH : DOWN_THIRD;
			logger.atDebug().log("{} ({}): switching to {}", musician.getId(), state, newState);
			state = newState;
			sequenceCountdown = SEQUENCE_LENGTH;
		}
	}

}
