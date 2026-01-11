package org.roach.margia.mains;

import org.roach.margia.mains.params.StateBasedParams;

/**
 * A state-based ruleset
 */
public class MainStateBased implements Algorithm<StateBasedParams> {

    @Override
    public String command() {
        return "statebased";
    }

    @Override
    public StateBasedParams createParams() {
        return new StateBasedParams();
    }

    @Override
    public String displayName() {
        return "State-based";
    }

}
