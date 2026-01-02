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
    public static Point2D.Double unitVector(Point2D origin, Point2D target) {
        var dx = target.getX() - origin.getX();
        var dy = target.getY() - origin.getY();
        var dist = origin.distance(target);
        return new Point2D.Double(dx / dist, dy / dist);
    }

    /**
     * Add two points
     * 
     * @param p1 first point
     * @param p2 second point
     * @return sum of two points
     */
    public static Point2D.Double add(Point2D p1, Point2D p2) {
        return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    /**
     * @param p1 first point
     * @param p2 second point
     * @return difference of p1 - p2
     */
    public static Point2D subtract(Point2D p1, Point2D p2) {
        return new Point2D.Double(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    /**
     * @param point  a point
     * @param scalar a scalar value
     * @return the point multiplied by the scalar
     */
    public static Point2D.Double multiply(Point2D point, double scalar) {
        return new Point2D.Double(point.getX() * scalar, point.getY() * scalar);
    }
    
    public static double distance(Point2D.Double p1, Point2D.Double p2) {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }
}
