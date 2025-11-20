package org.roach.midi_swarm;

/**
 * Represents a standard note length in 4/4 time
 */
public enum Length {
	/**
	 * 1/16th note
	 */
	L1_16(1),
	/**
	 * 1/8th note
	 */
	L1_8(2),
	/**
	 * dotted-eigth note
	 */
	L3_16(3),
	/**
	 * quarter note
	 */
	L1_4(4),
	/**
	 * 5/16th note
	 */
	L5_16(5),
	/**
	 * dotted quarter note
	 */
	L3_8(6),
	/**
	 * double-dotted quater note
	 */
	L7_8(7),
	/**
	 * half note
	 */
	L1_2(8);

	private final double partOfBeat;

	Length(final int portion) {
		partOfBeat = portion / 4.0;
	}

	/**
	 * @param bpm tempo in beats-per-minute
	 * @return number of milliseconds to play the note in that tempo
	 */
	public int getMillisForTempo(final double bpm) {
		var qpb = 60.0 / bpm;
		return (int) (partOfBeat * qpb * 1000.0);
	}

}
