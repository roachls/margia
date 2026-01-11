package org.roach.margia.mains;

import org.roach.margia.mains.params.RandomParams;

/**
 * Run with random agent and a 4x4 grid
 */
public class MainRandom implements Algorithm<RandomParams> {

    @Override
    public String command() {
        return "random";
    }

    @Override
    public RandomParams createParams() {
        return new RandomParams();
    }

    @Override
    public String displayName() {
        return "Random";
    }

}
