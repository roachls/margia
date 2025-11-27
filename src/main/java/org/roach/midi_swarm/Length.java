package org.roach.midi_swarm;

/**
 * Represents a standard note length in 4/4 time
 */
public class Length {
	private Length() {
		// no instantiation
	}

	/**
	 * @param ticks number of ticks
	 * @param bpm   tempo in beats-per-minute
	 * @return number of milliseconds to play the note in the given tempo
	 */
	public static int getMillisForTempo(final int ticks, final double bpm) {
		var qpb = 60000 / bpm;
		var partOfBeat = (double) ticks / 4.0;
		return (int) (partOfBeat * qpb);
	}

}
