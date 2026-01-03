package org.roach.margia.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class PointMathTest {

    @ParameterizedTest
    @MethodSource("unitVectorArgs")
    void testUnitVector(double x1, double y1, double x2, double y2, double ex, double ey) {
        var p1 = new Point2D.Double(x1, y1);
        var p2 = new Point2D.Double(x2, y2);
        var u = PointMath.unitVector(p1, p2);
        assertEquals(1.0, u.magnitude(), 1e-6);
        assertEquals(ex, u.x(), 1e-6);
        assertEquals(ey, u.y(), 1e-6);
    }

    static Stream<Arguments> unitVectorArgs() {
        var sin45 = Math.sin(Math.PI / 4.0);
        var cos45 = Math.cos(Math.PI / 4.0);
        return Stream.of(
        // @formatter:off
                Arguments.of(0.0, 0.0, 10.0, 10.0, cos45, sin45),
                Arguments.of(4.0, 5.5, 6.0, 7.5, cos45, sin45),
                Arguments.of(0.0, 0.0, -10.0, -10.0, -cos45, -sin45),
                Arguments.of(6.0, 7.5, 4.0, 5.5, -cos45, -sin45),
                Arguments.of(0.0, 0.0, 10.0, -10.0, cos45, -sin45),
                Arguments.of(0.0, 0.0, 10.0, 0.0, 1.0, 0.0),
                Arguments.of(0.0, 0.0, 0.0, 10.0, 0.0, 1.0)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,1,1", "4,5,6,7,10,12", "-1.2,4.6,10,3.2,8.8,7.8" })
    void testAdd(double x1, double y1, double x2, double y2, double ex, double ey) {
        var p1 = new Point2D.Double(x1, y1);
        var p2 = new Point2D.Double(x2, y2);
        var expected = new Vector2D(ex, ey);
        var sum = PointMath.add(p1, p2);
        assertEquals(expected, sum);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,1,1", "4,5,6,7,10,12", "-1.2,4.6,10,3.2,8.8,7.8" })
    void testMovePoint(double x1, double y1, double x2, double y2, double ex, double ey) {
        var p1 = new Point2D.Double(x1, y1);
        var v = new Vector2D(x2, y2);
        var expected = new Point2D.Double(ex, ey);
        var newPos = PointMath.movePoint(p1, v);
        assertEquals(expected, newPos);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,-1,-1", "4,5,6,7,-2,-2", "-1.2,4.6,10,3.2,-11.2,1.4" })
    void testSubtract(double x1, double y1, double x2, double y2, double ex, double ey) {
        var p1 = new Point2D.Double(x1, y1);
        var p2 = new Point2D.Double(x2, y2);
        var diff = PointMath.subtract(p1, p2);
        assertEquals(ex, diff.x(), 1e-6);
        assertEquals(ey, diff.y(), 1e-6);
    }

}
