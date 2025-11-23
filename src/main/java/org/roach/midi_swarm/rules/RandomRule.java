package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.messages.HeardNoteInfo;
import org.roach.midi_swarm.random.DieRoller;

/**
 * Random-note generator
 */
public class RandomRule extends MusicianRule {

	@Override
	public void calculateAction(long tick) {
		if (musician.getNotesIvePlayed() >= 5) {
			logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
			actionsToTake.add(() -> musician.playNote(REST));
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
		if (note == null || note.noteInfo().equals(REST)) {
			logger.atDebug().log("{}: heard null or rest, returning");
			return;
		}

		var r = DieRoller.rollDice("2d5");
		switch (r) {
		case 2: {
			actionsToTake.add(() -> musician
					.playNote(new NoteInfo(Math.min(127, note.noteInfo().note() + 7), note.noteInfo().velocity(), 1)));
			break;
		}
		case 3:
			actionsToTake.add(() -> musician.playNote(musician.getMyLastNote()));
			break;
		case 4: {
			var coinToss = DieRoller.rollDice("1d2");
			if (coinToss == 1) {
				var vel = Math.max(Musician.MIN_VELOCITY, note.noteInfo().velocity() - 10);
				actionsToTake.add(
						() -> musician.playNote(new NoteInfo(note.noteInfo().note(), vel, note.noteInfo().length())));
			} else {
				var vel = Math.min(Musician.MAX_VELOCITY, note.noteInfo().velocity() + 10);
				actionsToTake.add(
						() -> musician.playNote(new NoteInfo(note.noteInfo().note(), vel, note.noteInfo().length())));
			}
			break;
		}
		case 5:
			actionsToTake.add(() -> musician.playNote(new NoteInfo(Math.min(note.noteInfo().note() + 12, 127),
					note.noteInfo().velocity(), note.noteInfo().length())));
			break;
		case 6:
			actionsToTake.add(() -> musician.setMyLastNote(note.noteInfo()));
			actionsToTake.add(() -> musician.rest());
			break;
		case 7:
			actionsToTake.add(() -> musician.receiveMessage(new HeardNoteInfo(tick, note.noteInfo())));
			break;
		case 8:
			actionsToTake.add(() -> musician.playNote(new NoteInfo(Math.max(note.noteInfo().note() - 12, 0),
					note.noteInfo().velocity(), note.noteInfo().length())));
			break;
		case 9: {
			var dNote = Math.max(0, note.noteInfo().note() - 4);
			actionsToTake.add(() -> musician.playNote(new NoteInfo(dNote, note.noteInfo().velocity(), 2)));
			break;
		}
		case 10:
			actionsToTake.add(() -> musician.playNote(note.noteInfo()));
			break;
		default:
			break;
		}
	}

}
