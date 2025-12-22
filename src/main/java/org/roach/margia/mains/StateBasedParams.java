package org.roach.margia.mains;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters class StateBasedParams {
	@Parameter(names = { "-t", "--tempo" }, description = "Tempo of song in beats-per-minute")
	int tempo = 60;
	@Parameter(names = { "-channels", "--numChannels" }, description = "Number of MIDI channels")
	int numChannels = 8;
	@Parameter(names = { "-seq", "--sequenceLength" }, description = "Starting sequence length")
	int startingSequenceLength = 20;
	@Parameter(names = { "-seed", "--randomSeed" }, description = "Random seed")
	long randomSeed = 101;
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
	@Parameter(names = { "--sendExternalMidi",
			"-ext" }, description = "Set to true to send over external MIDI to a DAW")
	boolean sendExternalMidi;
}