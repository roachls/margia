package org.roach.margia.model;

import org.roach.margia.storage.Options;
import org.roach.margia.view.MusicianComponent;

import javafx.beans.property.*;
import javafx.geometry.Point2D;

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
    private final ObjectProperty<Point2D> position = new SimpleObjectProperty<Point2D>(Point2D.ZERO);
    private IntegerProperty radius = new SimpleIntegerProperty(MusicianComponentOptions.DEFAULT_RADIUS);
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
    public Point2D getPosition() { return position.get(); }

    /**
     * @return the locked
     */
    public boolean isLocked() { return locked; }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked) {
        var oldLocked = this.locked;
        this.locked = locked;
        if (oldLocked != this.locked)
            Options.getInstance().setDirty();
    }

    /**
     * Sets the position, but only if not {@code locked}
     * 
     * @param position new position
     */
    public void setPosition(Point2D position) {
        this.position.set(position);
    }

    /**
     * @return the position property
     */
    public ObjectProperty<Point2D> positionProperty() {
        return this.position;
    }

    @Override
    public String toString() {
        return "MusicianComponentOptions [locked=" + locked + ", position=" + position + ", radius=" + radius + "]";
    }

    /**
     * @return the radius
     */
    public int getRadius() { return radius.get(); }

    /**
     * @param radius the radius to set
     */
    public void setRadius(int radius) {
        var oldRadius = this.radius.get();
        this.radius.set(radius);
        if (oldRadius != radius) {
            Options.getInstance().setDirty();
        }
    }

    /**
     * @return the radius property
     */
    public IntegerProperty radiusProperty() {
        return this.radius;
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
        options.setRadius(this.radius.get());
    }
}
