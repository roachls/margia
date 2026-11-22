package org.roach.margia.model;

import static org.roach.margia.model.Note.*;

/**
 * Convenience enumeration of MIDI notes as octaves. Because O8 is incomplete
 * (only 7 notes), it is not included. All octaves are from C to B
 */
public enum Octave {
    /**
     * -2 octave
     */
    O_NEG2(C_N2, B_N2),
    /**
     * -1 octave
     */
    O_NEG1(C_N1, B_N1),
    /**
    * 
    */
    O0(C0, B0),
    /**
    * 
    */
    O1(C1, B1),
    /**
    * 
    */
    O2(C2, B2),
    /**
    * 
    */
    O3(C3, B3),
    /**
    * 
    */
    O4(C4, B4),
    /**
    * 
    */
    O5(C5, B5),
    /**
    * 
    */
    O6(C6, B6),
    /**
     * 
     */
    O7(C7, B7);

    final int low;
    final int high;

    private Octave(int low, int high) {
        this.low = low;
        this.high = high;
    }

    /**
     * @return lowest note in the octave
     */
    public int getLow() { return low; }

    /**
     * @return highest note in the octave
     */
    public int getHigh() { return high; }
}
