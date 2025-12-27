package org.roach.margia.mains;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Parameters for state-based algorithm")
class StateBasedParams implements MargiaParams {
    @Parameter(names = { "-seq", "--sequenceLength" }, description = "Starting sequence length")
    int startingSequenceLength = 20;
    @Parameter(names = { "-r", "--rows" }, description = "Number of rows in grid")
    int numRows = 4;
    @Parameter(names = { "-c", "--cols" }, description = "Number of columns in grid")
    int numCols = 4;
    @Parameter(names = {
            "--decrementSequenceLength" }, description = "Whether to decrement the sequence length over time")
    boolean decrementSequenceLength = true;
    @Parameter(names = {
            "--tickDecrementLength" }, description = "How often to decrement the sequence length (in ticks)")
    int tickDecrementCount = 100;
}