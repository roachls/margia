package org.roach.margia.storage;

import org.roach.margia.ui.*;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * UI-related options
 */
public class UiOptions {
    final ChangeEmitter emitter = new ChangeEmitter();
    private boolean showNumbers = true;
    private int radius = MusicianComponent.DEFAULT_RADIUS;
    private double gravity = AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT;
    private int edgeLength = AgentPanel.DEFAULT_EDGE_LENGTH;
    private double mass = 1.0;

    /**
     * @return true if numbers of musicians should be displayed
     */
    public boolean isShowNumbers() { return showNumbers; }

    /**
     * @param showNumbers the showNumbers to set
     */
    public void setShowNumbers(boolean showNumbers) {
        var oldShowNumbers = this.showNumbers;
        this.showNumbers = showNumbers;
        if (oldShowNumbers != showNumbers)
            emitter.fireChangeEvent(MusicianComponent.SHOW_NUMBERS_PROPERTY,
                    new ChangeSource(MusicianComponent.SHOW_NUMBERS_PROPERTY, this.showNumbers));
    }

    /**
     * @return the radius
     */
    public int getRadius() { return radius; }

    /**
     * @param radius the radius to set
     */
    public void setRadius(int radius) {
        var oldRadius = this.radius;
        this.radius = radius;
        if (oldRadius != radius)
            emitter.fireChangeEvent(MusicianComponent.RADIUS_PROPERTY,
                    new ChangeSource(MusicianComponent.RADIUS_PROPERTY, this.radius));
    }

    /**
     * @return the gravity
     */
    public double getGravity() { return gravity; }

    /**
     * @param gravity the gravity to set
     */
    public void setGravity(double gravity) { this.gravity = gravity; }

    /**
     * @return the edgeLength
     */
    public int getEdgeLength() { return edgeLength; }

    /**
     * @param edgeLength the edgeLength to set
     */
    public void setEdgeLength(int edgeLength) {
        var oldEdgeLength = this.edgeLength;
        this.edgeLength = edgeLength;
        if (oldEdgeLength != edgeLength)
            emitter.fireChangeEvent(AgentPanel.EDGE_LENGTH_PROPERTY,
                    new ChangeSource(AgentPanel.EDGE_LENGTH_PROPERTY, this.edgeLength));
    }

    /**
     * @return the mass
     */
    public double getMass() { return mass; }

    /**
     * @param mass the mass to set
     */
    public void setMass(double mass) { this.mass = mass; }

    @Override
    public String toString() {
        return "UiOptions [showNumbers=" + showNumbers + ", radius=" + radius + ", gravity=" + gravity + ", edgeLength="
                + edgeLength + ", mass=" + mass + "]";
    }

}