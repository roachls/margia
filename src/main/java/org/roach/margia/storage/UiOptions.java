package org.roach.margia.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import org.roach.margia.ui.*;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * UI-related options
 */
public class UiOptions {
    final ChangeEmitter emitter = new ChangeEmitter();
    private boolean showNumbers = true;
    private int radius = MusicianComponent.DEFAULT_RADIUS;
    private double gravity = AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT;
    private int edgeLength = AgentPanel.DEFAULT_EDGE_LENGTH;
    private final Map<Integer, MusicianComponentOptions> musicianComponents = new LinkedHashMap<>();

    /**
     * @return the musicianComponents
     */
    public Map<Integer, MusicianComponentOptions> getMusicianComponents() { return musicianComponents; }

    /**
     * @return true if numbers of musicians should be displayed
     */
    public boolean isShowNumbers() { return showNumbers; }

    /**
     * @param showNumbers the showNumbers to set
     */
    public void setShowNumbers(boolean showNumbers) {
        var oldShowNumbers = this.showNumbers;
        this.showNumbers = showNumbers;
        if (oldShowNumbers != showNumbers) {
            emitter.fireChangeEvent(MusicianComponent.SHOW_NUMBERS_PROPERTY,
                    new ChangeSource(MusicianComponent.SHOW_NUMBERS_PROPERTY, this.showNumbers));
            Options.getInstance().setDirty();
        }
    }

    /**
     * @return the radius
     */
    public int getRadius() { return radius; }

    /**
     * @param radius the radius to set
     */
    public void setRadius(int radius) {
        var oldRadius = this.radius;
        this.radius = radius;
        if (oldRadius != radius) {
            emitter.fireChangeEvent(MusicianComponent.RADIUS_PROPERTY,
                    new ChangeSource(MusicianComponent.RADIUS_PROPERTY, this.radius));
            Options.getInstance().setDirty();
        }
    }

    /**
     * @return the gravity
     */
    public double getGravity() { return gravity; }

    /**
     * @param gravity the gravity to set
     */
    public void setGravity(double gravity) {
        var oldGravity = this.gravity;
        this.gravity = gravity;
        if (oldGravity != this.gravity)
            Options.getInstance().setDirty();
    }

    /**
     * @return the edgeLength
     */
    public int getEdgeLength() { return edgeLength; }

    /**
     * @param edgeLength the edgeLength to set
     */
    public void setEdgeLength(int edgeLength) {
        var oldEdgeLength = this.edgeLength;
        this.edgeLength = edgeLength;
        if (oldEdgeLength != edgeLength) {
            emitter.fireChangeEvent(AgentPanel.EDGE_LENGTH_PROPERTY,
                    new ChangeSource(AgentPanel.EDGE_LENGTH_PROPERTY, this.edgeLength));
            Options.getInstance().setDirty();
        }
    }

    @Override
    public String toString() {
        return "UiOptions [showNumbers=" + showNumbers + ", radius=" + radius + ", gravity=" + gravity + ", edgeLength="
                + edgeLength + ", musicianComponents=" + musicianComponents + "]";
    }

}