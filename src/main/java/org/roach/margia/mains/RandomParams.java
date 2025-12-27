package org.roach.margia.mains;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * params for random-based algorithms
 */
@Parameters(commandDescription = "parameters for Random-based algorithms")
public class RandomParams implements MargiaParams {
    @Parameter(names= {"-n", "--numMusicians"}, description = "number of musicians")
    int numMusicians = 2;
}
