package org.roach.margia.mains.params;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * params for random-based algorithms
 */
@Parameters(commandDescription = "parameters for Random-based algorithms")
@SuppressWarnings("javadoc")
public class RandomParams extends AbstractAlgParams {
    @Parameter(names= {"-n", "--numMusicians"}, description = "number of musicians")
    public int numMusicians = 2;
}
