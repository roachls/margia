package org.roach.margia;

/**
 * Convenience enumeration of MIDI notes as octaves. Because O8 is incomplete
 * (only 7 notes), it is not included. All octaves are from C to B
 */
public enum Octave {
    /**
     * -2 octave
     */
    O_NEG2(0, 11),
    /**
     * -1 octave
     */
    O_NEG1(12, 23),
    /**
    * 
    */
    O0(24, 35),
    /**
    * 
    */
    O1(36, 47),
    /**
    * 
    */
    O2(48, 59),
    /**
    * 
    */
    O3(60, 71),
    /**
    * 
    */
    O4(72, 83),
    /**
    * 
    */
    O5(84, 95),
    /**
    * 
    */
    O6(96, 107),
    /**
     * 
     */
    O7(108, 119);

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
