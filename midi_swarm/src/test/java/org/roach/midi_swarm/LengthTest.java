package org.roach.midi_swarm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LengthTest {

	@ParameterizedTest
	@CsvSource({"L1_4,60,1000", "L1_8,60,500", "L1_4,120,500", "L1_2,60,2000", "L1_16,100,150"})
	void testGetMillisForTempo(Length length, int tempo, int expectedMillis) {
		assertEquals(expectedMillis, length.getMillisForTempo(tempo));
	}

}
