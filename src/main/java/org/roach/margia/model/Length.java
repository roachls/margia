package org.roach.margia.model;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.quantity.time.TimeQuantities;

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
    public static Quantity<Time> getMillisForTempo(final int ticks, final double bpm) {
        var qpb = 60000 / bpm;
        var partOfBeat = ticks / 4.0;
        return Quantities.getQuantity((int) (partOfBeat * qpb), TimeQuantities.MILLISECOND);
    }

}
