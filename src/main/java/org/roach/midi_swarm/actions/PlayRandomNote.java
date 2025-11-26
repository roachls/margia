package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;
import org.roach.midi_swarm.random.DieRoller;

public record PlayRandomNote(Musician musician, String dice, int num) implements MusicalAction {

	@Override
	public void perform() {
		var rand = DieRoller.rollDice(dice);
		if (rand <= num) {
			var randomNote = new NoteInfo(musician.getKey().randomNote(), Musician.START_VELOCITY, 1);
			musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), randomNote);
			musician.playNote(randomNote);
		}
	}

}
