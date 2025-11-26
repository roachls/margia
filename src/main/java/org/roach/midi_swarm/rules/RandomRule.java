package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.random.DieRoller;

/**
 * Random-note generator
 */
public class RandomRule extends MusicianRule {

	@Override
	public void calculateAction(long tick) {
		if (musician.getNotesIvePlayed() >= 5) {
			logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
			actionsToTake.add(() -> musician.playNote(REST.apply(1)));
			actionsToTake.add(() -> musician.resetNotesIvePlayed());
			return;
		}
		if (musician.getQueueSize() == 0) {
			logger.atDebug().log("{} queue is empty", musician.getId());
			if (musician.getId() == 0) {
				var rand = DieRoller.rollDice("2d6");
				if (rand <= 4) {
					var randomNoteNum = DieRoller.rollDice("8d16") - 1;
					var randomNote = new NoteInfo(randomNoteNum, Musician.START_VELOCITY, 1);
					logger.atDebug().log("{}: playing {}", musician.getId(), randomNote);
					actionsToTake.add(() -> musician.playNote(randomNote));
				}
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
			actionsToTake.add(() -> musician
					.playNote(new NoteInfo(Math.min(127, note.noteNum() + 7), note.velocity(), 1)));
			break;
		}
		case 3:
			actionsToTake.add(() -> musician.playNote(musician.getMyLastNote()));
			break;
		case 4: {
			var coinToss = DieRoller.rollDice("1d2");
			if (coinToss == 1) {
				var vel = Math.max(Musician.MIN_VELOCITY, note.velocity() - 10);
				actionsToTake.add(
						() -> musician.playNote(new NoteInfo(note.noteNum(), vel, note.length())));
			} else {
				var vel = Math.min(Musician.MAX_VELOCITY, note.velocity() + 10);
				actionsToTake.add(
						() -> musician.playNote(new NoteInfo(note.noteNum(), vel, note.length())));
			}
			break;
		}
		case 5:
			actionsToTake.add(() -> musician.playNote(new NoteInfo(Math.min(note.noteNum() + 12, 127),
					note.velocity(), note.length())));
			break;
		case 6:
			actionsToTake.add(() -> musician.setMyLastNote(note));
			actionsToTake.add(() -> musician.rest());
			break;
		case 7:
			actionsToTake.add(() -> musician.receiveMessage(note));
			break;
		case 8:
			actionsToTake.add(() -> musician.playNote(new NoteInfo(Math.max(note.noteNum() - 12, 0),
					note.velocity(), note.length())));
			break;
		case 9: {
			var dNote = Math.max(0, note.noteNum() - 4);
			actionsToTake.add(() -> musician.playNote(new NoteInfo(dNote, note.velocity(), 2)));
			break;
		}
		case 10:
			actionsToTake.add(() -> musician.playNote(note));
			break;
		default:
			break;
		}
	}

}
