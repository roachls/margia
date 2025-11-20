package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.random.DieRoller;

/**
 * Random-note generator
 */
public class RandomRule extends MusicianRule {

	@Override
	public void act(long tick) {
		if (musician.getLastTickIPlayedANote() == tick) {
			logger.atDebug().log("{}: I already played a note this tick", musician.getId());
			return;
		}
		if (musician.getNotesIvePlayed() >= 5) {
			logger.atDebug().log("{}: resting because I've played 5 notes", musician.getId());
			musician.rest();
			musician.resetNotesIvePlayed();
			return;
		}
		if (musician.getQueueSize() == 0) {
			logger.atDebug().log("{} queue is empty", musician.getId());
			var rand = DieRoller.rollDice("2d6");
			if (rand <= 4) {
				var randomNote = new NoteInfo(musician.getKey().randomNote(), Musician.START_VELOCITY, Length.L1_16);
				logger.atDebug().log("{}: playing {}", musician.getId(), randomNote);
				musician.playNote(randomNote);
				musician.setLastTickIPlayedANote(tick);
			}
		}

		var note = musician.getNextNoteHeard();
		logger.atDebug().log("{}: heard {}", musician.getId(), note);
		if (note == null)
			return;

		var r = DieRoller.rollDice("2d5");
		switch (r) {
		case 2: {
			var ni = new NoteInfo(musician.getKey().upInterval(note.note(), 4), note.velocity(), Length.L1_16);
			musician.playNote(ni);
			break;
		}
		case 3:
			musician.repeatLastNote();
			break;
		case 4: {
			var coinToss = DieRoller.rollDice("1d2");
			if (coinToss == 1) {
				// TODO decrease velocity of played note
			} else {
				// TODO increase velocity of played note
			}
			break;
		}
		case 5:
			// TODO what to do here?
//			musician.decrementOctave();
			break;
		case 6:
			musician.setMyLastNote(note);
			musician.rest();
			break;
		case 7:
			musician.receiveMessage(note);
			break;
		case 8:
			// TODO what to do here?
//			musician.incrementOctave();
			break;
		case 9: {
			var ni = new NoteInfo(musician.getKey().downInterval(note.note(), 4), note.velocity(), Length.L1_8);
			musician.playNote(ni);
			break;
		}
		case 10:
			musician.playNote(note);
			break;
		default:
			break;
		}
	}

}
