package org.roach.margia.model;

import java.awt.geom.Point2D;

import javax.swing.event.ChangeListener;

import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter;
import org.roach.margia.view.ChangeEmitter.ChangeSource;
import org.roach.margia.view.MusicianComponent;

/**
 * UI Options for {@link MusicianComponent}s
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class MusicianComponentOptions {
    private boolean locked;
    private final Point2D.Double position = new Point2D.Double();
    private int radius = MusicianComponentOptions.DEFAULT_RADIUS;
    private final ChangeEmitter emitter = new ChangeEmitter();
    /**
     * property to update position
     */
    public static final String POSITION_PROPERTY = "position";
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
    public void setLocked(boolean locked) { 
        var oldLocked = locked;
        this.locked = locked; 
        if (oldLocked != this.locked)
            Options.getInstance().setDirty();
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
        return "MusicianComponentOptions [locked=" + locked + ", position=" + position + ", radius=" + radius + "]";
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
    }
}
