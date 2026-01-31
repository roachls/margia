package org.roach.margia.storage;

import java.awt.geom.Point2D;

import javax.swing.event.ChangeListener;

import org.roach.margia.ui.ChangeEmitter;
import org.roach.margia.ui.MusicianComponent;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;
import org.roach.margia.util.RangeCheck;

/**
 * UI Options for {@link MusicianComponent}s
 */
public class MusicianComponentOptions {
    private boolean locked;
    private final Point2D.Double position = new Point2D.Double();
    private double mass = 1.0;
    private int radius = MusicianComponentOptions.DEFAULT_RADIUS;
    private final ChangeEmitter emitter = new ChangeEmitter();
    /**
     * property to update position
     */
    public static final String POSITION_PROPERTY = "position";
    /**
     * property name of mass spinner
     */
    public static final String MASS_PROPERTY = "mass";
    /**
     * property name of radius spinner
     */
    public static final String RADIUS_PROPERTY = "ui.radius";
    /**
     * default radius of musician components
     */
    public static final int DEFAULT_RADIUS = 20;

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
    public void setMass(double mass) {
        var oldMass = this.mass;
        this.mass = RangeCheck.check("mass", mass, 0.1, 100.0);
        if (oldMass != this.mass) {
            emitter.fireChangeEvent(MASS_PROPERTY, new ChangeSource(MASS_PROPERTY, this.mass));
            Options.getInstance().setDirty();
        }
    }

    /**
     * Sets the position, but only if not {@code locked}
     * 
     * @param position new position
     */
    public void setPosition(Point2D.Double position) {
        var oldPosition = new Point2D.Double(this.position.x, this.position.y);
        this.position.setLocation(position);
        if (!oldPosition.equals(this.position)) {
            // value of position is irrelevant because the MusicianComponent
            // will simply call updatePosition
            emitter.fireChangeEvent(POSITION_PROPERTY, new ChangeSource(POSITION_PROPERTY, null));
        }
    }

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
    public void setRadius(int radius) {
        var oldRadius = this.radius;
        this.radius = radius;
        if (oldRadius != radius) {
            emitter.fireChangeEvent(RADIUS_PROPERTY, new ChangeSource(RADIUS_PROPERTY, this.radius));
            Options.getInstance().setDirty();
        }
    }

    /**
     * @param property name of property
     * @param listener listener of property
     */
    public void addChangeListener(String property, ChangeListener listener) {
        this.emitter.addChangeListener(property, listener);
    }

    MusicianComponentOptions copy() {
        var copy = new MusicianComponentOptions();
        copy.locked = locked;
        copy.mass = mass;
        copy.radius = radius;

        return copy;
    }

    /**
     * @param options copies the settings of this {@link MusicianComponentOptions}
     *                into the given one
     */
    public void copyInto(MusicianComponentOptions options) {
        options.setLocked(this.locked);
        options.setRadius(this.radius);
        options.setMass(this.mass);
    }
}
