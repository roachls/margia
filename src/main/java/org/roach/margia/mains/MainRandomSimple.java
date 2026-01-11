package org.roach.margia.mains;

import org.roach.margia.mains.params.RandomParams;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandomSimple implements Algorithm<RandomParams> {

    @Override
    public String command() {
        return "random_simple";
    }

    @Override
    public RandomParams createParams() {
        return new RandomParams();
    }

    @Override
    public String displayName() {
        return "Random Simple";
    }
}
