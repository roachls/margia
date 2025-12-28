package org.roach.margia.mains.params;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@SuppressWarnings("javadoc")
@Parameters(commandDescription = "Parameters for state-based algorithm")
public class StateBasedParams extends AbstractAlgParams {
    @Parameter(names = { "-seq", "--sequenceLength" }, description = "Starting sequence length")
    public int startingSequenceLength = 20;
    @Parameter(names = { "-r", "--rows" }, description = "Number of rows in grid")
    public int numRows = 4;
    @Parameter(names = { "-c", "--cols" }, description = "Number of columns in grid")
    public int numCols = 4;
    @Parameter(names = {
            "--decrementSequenceLength" }, description = "Whether to decrement the sequence length over time")
    public boolean decrementSequenceLength = true;
    @Parameter(names = {
            "--tickDecrementLength" }, description = "How often to decrement the sequence length (in ticks)")
    public int tickDecrementCount = 100;
    @Parameter(names = "--tickDelay", description = "number of ticks to delay before starting to repeat sequence")
    public int tickDelay = 0;
}