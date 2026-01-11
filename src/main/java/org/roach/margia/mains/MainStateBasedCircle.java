package org.roach.margia.mains;

import org.roach.margia.mains.params.CircleParams;

/**
 * State-based agents in a circle
 */
public class MainStateBasedCircle implements Algorithm<CircleParams> {

    @Override
    public String command() {
        return "circle";
    }

    @Override
    public CircleParams createParams() {
        return new CircleParams();
    }

    @Override
    public String displayName() {
        return "State-based circle";
    }

}
