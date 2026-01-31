package org.roach.margia.view;

import java.awt.geom.Point2D;

/**
 * A 2-dimensional vector
 * 
 * @param x x-vector
 * @param y y-vector
 */
public record Vector2D(double x, double y) {

    /**
     * @return magnitude of the vector
     */
    public double magnitude() {
        return Math.hypot(x, y);
    }

    /**
     * @return angle from origin to endpoint
     */
    public double angle() {
        return Math.atan2(y, x);
    }

    /**
     * @param scalar scalar to multiply by
     * @return a new vector that is this vector multiplied component-wise with the
     *         scalar
     */
    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    /**
     * @param scalar scalar to multiply by
     * @return a new vector that is this vector multiplied component-wise with the
     *         scalar
     */
    public Vector2D divide(double scalar) {
        if (scalar == 0.0)
            throw new IllegalArgumentException("Can't divide by zero");
        return new Vector2D(x / scalar, y / scalar);
    }

    /**
     * Perform vector addition
     * 
     * @param other the other vector
     * @return a new vector with each component being the sum of this component and
     *         the other component
     */
    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    /**
     * Vector subtraction. I.e., returns the equivalent of adding this vector to
     * another vector but with the direction "flipped" the other way
     * 
     * @param other other vector
     * @return a new vector with each component being the difference between this
     *         component and the other component
     */
    public Vector2D subtract(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }

    /**
     * Construct a vector that points in the direction from the origin to the
     * target, and whose magnitude is the distance from origin to target
     * 
     * @param origin origin point
     * @param target target point
     */
    public Vector2D(Point2D origin, Point2D target) {
        this(target.getX() - origin.getX(), target.getY() - origin.getY());
    }
}
