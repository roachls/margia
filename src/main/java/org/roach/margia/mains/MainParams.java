package org.roach.margia.mains;

import java.nio.file.Path;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Parameters used by all algorithms
 */
@Parameters
public class MainParams {
    @Parameter(names = { "-channels", "--numChannels" }, description = "Number of MIDI channels")
    int numChannels = 8;
    @Parameter(names = { "-seed", "--randomSeed" }, description = "Random seed")
    long randomSeed = 101;
    @Parameter(names = { "-f", "--file" }, description = "Initial file to load")
    Path file;
    @Parameter(names = { "-ui" }, description = "type of UI to use")
    UiType ui = UiType.SWING;

    enum UiType {
        SWING, NONE;
    }
}