package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.actions.*;
import org.roach.midi_swarm.random.DieRoller;

/**
 * Random-note generator
 */
public class RandomRule extends MusicianRule {

	@Override
	public void calculateAction(long tick) {
		if (musician.getNotesIvePlayed() >= 5) {
			logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
			actionsToTake.add(new PlayNote(musician, REST.apply(1)));
			actionsToTake.add(new ResetPlayedNotes(musician));
			return;
		}
		if (musician.getQueueSize() == 0) {
			logger.atDebug().log("{} queue is empty", musician.getId());
			if (musician.getId() == 0) {
				actionsToTake.add(new PlayRandomNote(musician, "2d6", 4));
			}
			return;
		}

		var note = musician.getNextNoteHeard();
		logger.atDebug().log("{}: heard {}", musician.getId(), note);
		if (note == null || note.equals(REST.apply(1))) {
			logger.atDebug().log("{}: heard null or rest, returning");
			return;
		}

		var r = DieRoller.rollDice("2d5");
		switch (r) {
		case 2: {
			actionsToTake.add(new PlayNoteUpInterval(musician, note, 5));
			break;
		}
		case 3:
			actionsToTake.add(new RepeatLastNote(musician));
			break;
		case 4: {
			var coinToss = DieRoller.rollDice("1d2");
			if (coinToss == 1) {
				actionsToTake.add(new PlayNoteDownVelocity(musician, note, 10));
			} else {
				actionsToTake.add(new PlayNoteUpVelocity(musician, note, 10));
			}
			break;
		}
		case 5:
			actionsToTake.add(new PlayNoteUpOctave(musician, note));
			break;
		case 6:
			actionsToTake.add(new SetLastNote(musician, note));
			actionsToTake.add(new RestOneTick(musician));
			break;
		case 7:
			actionsToTake.add(() -> musician.receiveMessage(note));
			break;
		case 8:
			actionsToTake.add(new PlayNoteDownOctave(musician, note));
			break;
		case 9: {
			actionsToTake.add(new PlayNoteDownInterval(musician, note, 3));
			break;
		}
		case 10:
			actionsToTake.add(new PlayNote(musician, note));
			break;
		default:
			break;
		}
	}

}
