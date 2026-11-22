package org.roach.margia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.quantity.time.TimeQuantities;

class LengthTest {

    @ParameterizedTest
    @CsvSource({ "4,60,1000", "2,60,500", "4,120,500", "8,60,2000", "1,100,150" })
    void testGetMillisForTempo(int length, int tempo, int expectedMillis) {
        assertEquals(Quantities.getQuantity(expectedMillis, TimeQuantities.MILLISECOND),
                Length.getMillisForTempo(length, tempo));
    }

}
