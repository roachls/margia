package org.roach.margia.mains.params;

import com.beust.jcommander.*;

@SuppressWarnings("javadoc")
@Parameters(commandDescription = "Parameters for circle state-based algorithm")
public class CircleParams extends AbstractAlgParams {
    @Parameter(names = { "-seq", "--sequenceLength" }, description = "Starting sequence length")
    public int startingSequenceLength = 20;
    @Parameter(names = {
            "--decrementSequenceLength" }, description = "Whether to decrement the sequence length over time")
    public boolean decrementSequenceLength = true;
    @Parameter(names = {
            "--tickDecrementLength" }, description = "How often to decrement the sequence length (in ticks)")
    public int tickDecrementCount = 100;
    @Parameter(names = { "-n", "--numMusicians" }, description = "number of musicians")
    public int numMusicians = 8;
    @Parameter(names = "--tickDelay", description = "number of ticks to delay before starting to repeat sequence")
    public int tickDelay = 0;
}