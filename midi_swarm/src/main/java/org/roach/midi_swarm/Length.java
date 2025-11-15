package org.roach.midi_swarm;

public enum Length {
    L1_16(1), L1_8(2), L3_16(3), L1_4(4), L5_16(5), L3_8(6), L7_8(7), L1_2(8);

    private final double partOfBeat;

    Length(final int portion) {
	partOfBeat = portion / 4.0;
    }

    public int getMillisForTempo(final double bpm) {
	var qpb = 60.0 / bpm;
	return (int) (partOfBeat * qpb * 1000.0);
    }

}
