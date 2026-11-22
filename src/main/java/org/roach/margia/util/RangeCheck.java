package org.roach.margia.util;

/**
 * Utility class for range checking
 */
public class RangeCheck {
    private RangeCheck() {
        /* no instantiation */ }

    /**
     * Check that the given number is within range
     * 
     * @param <T>    type of value
     * @param name   name of value
     * @param number number being checked
     * @param low    lowest value acceptable (inclusive)
     * @param high   highest value acceptable (inclusive)
     * @return the number if it is in range
     * @throws IllegalArgumentException if the number is not in range
     */
    public static <T extends Number> T check(String name, T number, T low, T high) {
        if (number.doubleValue() < low.doubleValue() || number.doubleValue() > high.doubleValue())
            throw new IllegalArgumentException(name + " must be between " + low + " and " + high);
        return number;
    }
}
