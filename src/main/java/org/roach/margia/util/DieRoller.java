package org.roach.margia.util;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * Rolls dice to get a random number
 */
public class DieRoller {
    private static final Pattern REGEX = Pattern.compile("\\d+d\\d+");
    private static Random random = new Random();
    private static long seed;

    private static record DiceNums(int numDice, int numSides) {
    }

    private DieRoller() {
        // no instantiation
    }

    /**
     * @param seed the seed for the random generator
     */
    public static void setSeed(long seed) {
        DieRoller.seed = seed;
        random.setSeed(seed);
    }

    /**
     * @param diceDescription description of dice in the form xny, where x is number
     *                        of dice, and y is number of sides per die
     * @return results of throwing the dice
     */
    public static int rollDice(final String diceDescription) {
        var nums = parseString(diceDescription);
        int num = 0;
        for (var i = 0; i < nums.numDice; i++) {
            num += random.nextInt(nums.numSides) + 1;
        }
        return num;
    }

    private static DiceNums parseString(String diceDescription) {
        var matcher = REGEX.matcher(diceDescription);
        if (!matcher.matches())
            throw new IllegalArgumentException("Illegal dice: " + diceDescription
                    + "; must match pattern xdy, where x is the number of dice thrown and y is the number of sides on one die");
        var split = diceDescription.split("d");
        var numDice = Integer.parseInt(split[0]);
        var numSides = Integer.parseInt(split[1]);
        return new DiceNums(numDice, numSides);
    }

    /**
     * @param diceDescription description of dice to roll
     * @return the minimum value possible for these dice
     */
    public static int getMin(final String diceDescription) {
        var nums = parseString(diceDescription);
        return nums.numDice;
    }

    /**
     * @param diceDescription description of dice to roll
     * @return the maximum value possible for these dice
     */
    public static int getMax(final String diceDescription) {
        var nums = parseString(diceDescription);
        return nums.numDice * nums.numSides;
    }

    /**
     * reset the random number generator to its original state
     */
    public static void reset() {
        random = new Random();
        random.setSeed(seed);
    }
}
