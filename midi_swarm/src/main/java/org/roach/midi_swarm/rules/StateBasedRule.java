package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {

	private enum State {
		START, PLAYING_A, PLAYING_B;
	}

	private State state = State.START;

	@Override
	public void act(long tick) {
		var note = musician.getNextNoteHeard();
		logger.atDebug().log("{}: ({}) heard {}, queue size={}", musician.getId(), state, note,
				musician.getQueueSize());
		if (note == null) {
			logger.atDebug().log("{} heard nothing, returning", musician.getId());
			return;
		}

		switch (state) {
		case START:
			if (note != null) {
				musician.playNote(note);
			}
			if (musician.getQueueSize() >= 3) {
				if ((tick + note.note()) % 2 == 0)
					state = State.PLAYING_A;
				else
					state = State.PLAYING_B;
				musician.receiveMessage(note);
			} else {
				musician.receiveMessage(note);
				System.out.println(musician.getId() + " put note back, queuesize=" + musician.getQueueSize());
			}
			break;
		case PLAYING_A:
			if (note.note() != -1) {
				var noteUp = musician.getKey().upInterval(note.note(), 3);
				var ni = new NoteInfo(noteUp, musician.getVelocity(), note.length());
				musician.playNote(ni);
			} else
				musician.playNote(note);
//			if (musician.getQueueSize() == 0)
			state = State.START;
			break;
		case PLAYING_B:
			if (note.note() != -1) {
				var noteDown = musician.getKey().downInterval(note.note(), 4);
				var ni = new NoteInfo(noteDown, musician.getVelocity(), note.length());
				musician.playNote(ni);
			} else
				musician.playNote(note);
//			if (musician.getQueueSize() == 0)
			state = State.START;
			break;
		default:
			break;
		}
	}

}
