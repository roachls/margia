package org.roach.margia.storage;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.roach.margia.view.AgentPanel;

/**
 * Parking spot for global variables where necessary
 */
public class Global {
    private Global() {
        // no instantiation
    }

    private static final AtomicInteger screenWidth = new AtomicInteger();
    private static final AtomicReference<Double> minComponentX = new AtomicReference<>(0d);
    private static final AtomicReference<Double> maxComponentX = new AtomicReference<>(0d);

    /**
     * @return the minimum component x
     */
    public static double getMincomponentY() { return minComponentX.get(); }

    /**
     * @return the min component x-position
     */
    public static double getMinComponentX() { return minComponentX.get(); }

    /**
     * @param min the min component x-position
     */
    public static void setMinComponentX(double min) {
        minComponentX.set(min);
    }

    /**
     * @return the max component x-position
     */
    public static double getMaxComponentX() { return maxComponentX.get(); }

    /**
     * @param max max component x-position
     */
    public static void setMaxComponentX(double max) {
        maxComponentX.set(max);
    }

    /**
     * @return width of screen (actually {@link AgentPanel})
     */
    public static int getScreenWidth() { return screenWidth.get(); }

    /**
     * @param screenWidth width of screen
     */
    public static void setScreenWidth(int screenWidth) {
        Global.screenWidth.set(screenWidth);
    }
}
