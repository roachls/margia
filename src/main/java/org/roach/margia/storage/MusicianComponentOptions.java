package org.roach.margia.storage;

import java.awt.geom.Point2D;

import org.roach.margia.ui.MusicianComponent;
import org.roach.margia.util.RangeCheck;

/**
 * UI Options for {@link MusicianComponent}s
 */
public class MusicianComponentOptions {
    private boolean locked;
    private final Point2D.Double position = new Point2D.Double();
    private double mass = 1.0;
    private int radius = MusicianComponent.DEFAULT_RADIUS;

    /**
     * @return the position
     */
    public Point2D.Double getPosition() { return position; }

    /**
     * @return the locked
     */
    public boolean isLocked() { return locked; }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked) { this.locked = locked; }

    /**
     * @return the mass
     */
    public double getMass() { return mass; }

    /**
     * @param mass the mass to set
     */
    public void setMass(double mass) { this.mass = RangeCheck.check("mass", mass, 0.1, 100.0); }

    @Override
    public String toString() {
        return "MusicianComponentOptions [locked=" + locked + ", position=" + position + ", mass=" + mass + ", radius="
                + radius + "]";
    }

    /**
     * @return the radius
     */
    public int getRadius() { return radius; }

    /**
     * @param radius the radius to set
     */
    public void setRadius(int radius) { this.radius = radius; }
}
