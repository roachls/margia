package org.roach.margia.ui;

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
}
