package org.roach.midi_swarm.random;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

public class DieRoller {
    private static final Pattern REGEX = Pattern.compile("\\d+d\\d+");
    private static final Random RANDOM = new SecureRandom();

    private DieRoller() {
	// no instantiation
    }

    public static int rollDice(final String diceDescription) {
	var matcher = REGEX.matcher(diceDescription);
	if (!matcher.matches())
	    throw new IllegalArgumentException("Illegal dice: " + diceDescription
		    + "; must match pattern xdy, where x is the number of dice thrown and y is the number of sides on one die");
	var split = diceDescription.split("d");
	var numDice = Integer.parseInt(split[0]);
	var numSides = Integer.parseInt(split[1]);
	int num = 0;
	for (var i = 0; i < numDice; i++) {
	    num += RANDOM.nextInt(numSides) + 1;
	}
	return num;
    }
    
}
