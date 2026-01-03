package org.roach.margia.ui;

import java.awt.geom.Point2D;

/**
 * Utilities for dealing with {@link Point2D points}
 */
public class PointMath {
    private PointMath() {
        // no instantiation
    }

    /**
     * @param origin origin point
     * @param target target point
     * @return the unit vector from the origin pointing towards the target
     */
    public static Vector2D unitVector(Point2D origin, Point2D target) {
        var dx = target.getX() - origin.getX();
        var dy = target.getY() - origin.getY();
        var dist = origin.distance(target);
        return new Vector2D(dx / dist, dy / dist);
    }

    /**
     * Add two points
     * 
     * @param p1 first point
     * @param p2 second point
     * @return sum of two points
     */
    public static Vector2D add(Point2D p1, Point2D p2) {
        return new Vector2D(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    /**
     * Move the point in the direction of the given vector
     * 
     * @param point original point
     * @param vec   vector
     * @return a new {@link Point2D} moved by the given vector
     */
    public static Point2D movePoint(Point2D point, Vector2D vec) {
        return new Point2D.Double(point.getX() + vec.x(), point.getY() + vec.y());
    }

    /**
     * @param p1 first point
     * @param p2 second point
     * @return difference of p1 - p2
     */
    public static Vector2D subtract(Point2D p1, Point2D p2) {
        return new Vector2D(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

}
