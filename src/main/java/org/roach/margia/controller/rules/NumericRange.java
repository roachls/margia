package org.roach.margia.controller.rules;

/**
 * A range of integers
 * 
 * @param min minimum value
 * @param max maximum value
 */
public record NumericRange(int min, int max) {
    /**
     * Validate that min &lt;= max
     * 
     * @param min minimum
     * @param max maximum
     * @throws IllegalArgumentException if min > max
     */
    public NumericRange {
        if (min > max)
            throw new IllegalArgumentException(
                    "Invalid NumericRange, min (%d) must be <= max (%d)".formatted(min, max));
    }

    /**
     * @param num number to check
     * @return {@code true} if number is within the inclusive range, false otherwise
     */
    public boolean isInRange(int num) {
        return num >= min && num <= max;
    }
}