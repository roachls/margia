package org.roach.midi_swarm.rules;

import org.roach.midi_swarm.*;

/**
 * A state-machine based agent
 */
public class StateBasedRule extends MusicianRule {

	private enum State {
		START, B, PLAYING;
	}

	private State state = State.START;

	@Override
	public void act(long tick) {
		var note = musician.getNextNoteHeard();
		logger.atDebug().log("{}: heard {}", musician.getId(), note);
		if (note == null)
			return;

		switch (state) {
		case START:
			if (note != null) {
				musician.playNote(note);
			}
			if (musician.getQueueSize() >= 3) {
				state = State.B;
//				act(note);
			} else {
//				musician.receiveMessage(note);
				System.out.println(musician.getId() + " put note back, queuesize=" + musician.getQueueSize());
			}
			break;
		case B:
			if (note != null) {
				state = State.PLAYING;
				act(tick);
			}
			if (musician.getQueueSize() == 0) {
				state = State.START;
			} else {
				state = State.PLAYING;
				act(tick);
			}
			break;
		case PLAYING:
			var noteUp = musician.getKey().upInterval(note.note(), 3);
			var ni = new NoteInfo(noteUp, musician.getOctave(), musician.getVelocity(), note.length());
			musician.playNote(ni);
			state = State.B;
			break;
		default:
			break;
		}
	}

}
