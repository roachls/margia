package org.roach.margia.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

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
    @CsvSource({ "0,0,1,0,0", "1,1,10,10,10", "10,10,0.1,1,1", "-1,-2,0.5,-0.5,-1", "40,-50,100,4000,-5000" })
    void testMultiply(double x, double y, double scalar, double ex, double ey) {
        var vec = new Vector2D(x, y);
        var newVec = vec.multiply(scalar);
        assertEquals(ex, newVec.x(), 1e-9);
        assertEquals(ey, newVec.y(), 1e-9);
    }

}
