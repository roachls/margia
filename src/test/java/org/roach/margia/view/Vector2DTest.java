package org.roach.margia.view;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class Vector2DTest {

    @ParameterizedTest
    @MethodSource("magArgs")
    void testMagnitude(Vector2D vec, double expMag) {
        assertEquals(expMag, vec.magnitude());
    }

    static Stream<Arguments> magArgs() {
        return Stream.of(
        // @formatter:off
                Arguments.of(new Vector2D(0, 0), 0),
                Arguments.of(new Vector2D(0, 2), 2),
                Arguments.of(new Vector2D(0, -2), 2),
                Arguments.of(new Vector2D(2, 0), 2),
                Arguments.of(new Vector2D(-2, 0), 2),
                Arguments.of(new Vector2D(10, 10), Math.sqrt(200))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("angleArgs")
    void testAngle(Vector2D vec, double expAngle) {
        assertEquals(expAngle, vec.angle());
    }

    static Stream<Arguments> angleArgs() {
        return Stream.of(
        // @formatter:off
                Arguments.of(new Vector2D(0, 0), 0),
                Arguments.of(new Vector2D(0, 2), Math.PI / 2d),
                Arguments.of(new Vector2D(0, -2), -Math.PI / 2d),
                Arguments.of(new Vector2D(2, 0), 0),
                Arguments.of(new Vector2D(-2, 0), Math.PI),
                Arguments.of(new Vector2D(10, 10), Math.PI / 4d)
                // @formatter:on
        );
    }

    @ParameterizedTest
    @CsvSource({ "0,0,1,0,0", "1,1,10,10,10", "10,10,0.1,1,1", "-1,-2,0.5,-0.5,-1", "40,-50,100,4000,-5000",
            "10,10,0,0,0" })
    void testMultiply(double x, double y, double scalar, double ex, double ey) {
        var vec = new Vector2D(x, y);
        var newVec = vec.multiply(scalar);
        assertEquals(ex, newVec.x(), 1e-9);
        assertEquals(ey, newVec.y(), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,1,0,0", "1,1,10,0.1,0.1", "10,10,0.1,100,100", "-1,-2,0.5,-2,-4", "40,-50,100,0.4,-0.5" })
    void testDivide(double x, double y, double scalar, double ex, double ey) {
        var vec = new Vector2D(x, y);
        var newVec = vec.divide(scalar);
        assertEquals(ex, newVec.x(), 1e-9);
        assertEquals(ey, newVec.y(), 1e-9);
    }

    @Test
    void testDivideByZero() {
        var vec = new Vector2D(1, 2);
        assertThrows(IllegalArgumentException.class, () -> vec.divide(0));
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,1,1", "4,5,6,7,2,2", "-1.2,4.6,10,3.2,11.2,-1.4" })
    void testNewVectorFromPoints(double originx, double originy, double targetx, double targety, double ex, double ey) {
        var origin = new Point2D.Double(originx, originy);
        var target = new Point2D.Double(targetx, targety);
        var diff = new Vector2D(origin, target);
        assertEquals(ex, diff.x(), 1e-6);
        assertEquals(ey, diff.y(), 1e-6);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,1,1", "4,5,6,7,10,12", "-1.2,4.6,10,3.2,8.8,7.8" })
    void testAdd(double x1, double y1, double x2, double y2, double ex, double ey) {
        var v1 = new Vector2D(x1, y1);
        var v2 = new Vector2D(x2, y2);
        var expected = new Vector2D(ex, ey);
        var sum = v1.add(v2);
        assertEquals(expected, sum);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,0,0,0,0", "0,0,1,1,-1,-1", "4,5,6,7,-2,-2", "-1.2,4.6,10,3.2,-11.2,1.4" })
    void testSubtract(double x1, double y1, double x2, double y2, double ex, double ey) {
        var v1 = new Vector2D(x1, y1);
        var v2 = new Vector2D(x2, y2);
        var sum = v1.subtract(v2);
        assertEquals(ex, sum.x(), 1e-6);
        assertEquals(ey, sum.y(), 1e-6);
    }

    @ParameterizedTest
    @CsvSource({ "1,0,1,0,1", "0,1,0,1,1", "4,5,0.6246950475544243,0.7808688094430304,1",
            "-1.2,4.6,-0.25242189714700275,0.9676172723968439,1", "0,0,0,0,0" })
    void testNormalize(double x, double y, double ex, double ey, double expMag) {
        var v = new Vector2D(x, y);
        var n = assertDoesNotThrow(v::normalize);
        assertEquals(expMag, n.magnitude(), 1e-8);
        assertEquals(ex, n.x());
        assertEquals(ey, n.y());
    }

    @ParameterizedTest
    @CsvSource({ "1,0,0,-1", "0,1,1,-0", "4,5,5,-4", "-1.2,4.6,4.6,1.2", "0,0,0,-0" })
    void testRotate(double x, double y, double ex, double ey) {
        var v = new Vector2D(x, y);
        var n = assertDoesNotThrow(v::rotateClockwise90);
        assertEquals(ex, n.x());
        assertEquals(ey, n.y());
    }

    @ParameterizedTest
    @CsvSource({ "1,0,-1,-0", "0,1,-0,-1", "4,5,-4,-5", "-1.2,4.6,1.2,-4.6", "0,0,-0,-0" })
    void testflip(double x, double y, double ex, double ey) {
        var v = new Vector2D(x, y);
        var n = assertDoesNotThrow(v::flip);
        assertEquals(ex, n.x());
        assertEquals(ey, n.y());
    }

}
