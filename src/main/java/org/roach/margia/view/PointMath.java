package org.roach.margia.view;

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
     * Move the point in the direction of the given vector
     * 
     * @param point original point
     * @param vec   vector
     * @return a new {@link Point2D} moved by the given vector
     */
    public static Point2D.Double movePoint(Point2D point, Vector2D vec) {
        return new Point2D.Double(point.getX() + vec.x(), point.getY() + vec.y());
    }

}
