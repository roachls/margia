package org.roach.midi_swarm.actions;

import org.roach.midi_swarm.Musician;
import org.roach.midi_swarm.NoteInfo;

/**
 * 
 * @param musician The musician
 * @param spread 
 * @param num      number below which a random note will be played.
 */
public record PlayPseudoRandomNote(Musician musician, int spread, int num) implements MusicalAction {

	@Override
	public void perform() {
		var rand = musician.getId() + musician.getCurrentTick();
		if (musician.getMyLastNote() != null) {
			rand += musician.getMyLastNote().noteNum();
		}
		rand %= spread;
		if (rand <= num) {
			var randomNote = new NoteInfo(musician.getKey().randomNote(), Musician.START_VELOCITY, 1);
			musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), randomNote);
			musician.playNote(randomNote);
		}
	}

}
