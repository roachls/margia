package org.roach.margia.model;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.event.ChangeListener;

import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * UI-related options
 */
public class UiOptions {
    final ChangeEmitter emitter = new ChangeEmitter();
    private boolean showNumbers = true;
    private double gravity = UiOptions.DEFAULT_GRAVITATIONAL_CONSTANT;
    private int edgeLength = UiOptions.DEFAULT_EDGE_LENGTH;
    private final Map<Integer, MusicianComponentOptions> musicianComponents = new LinkedHashMap<>();
    /**
     * property name of whether to show numbers
     */
    public static final String SHOW_NUMBERS_PROPERTY = "ui.show_numbers";
    /**
     * property name of edge length spinner
     */
    public static final String EDGE_LENGTH_PROPERTY = "ui.edge_length";
    /**
     * default edge length
     */
    public static final int DEFAULT_EDGE_LENGTH = 60;
    /**
     * default gravitational constant
     */
    public static final double DEFAULT_GRAVITATIONAL_CONSTANT = 5.0;

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
            emitter.fireChangeEvent(SHOW_NUMBERS_PROPERTY, new ChangeSource(SHOW_NUMBERS_PROPERTY, this.showNumbers));
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
            emitter.fireChangeEvent(EDGE_LENGTH_PROPERTY, new ChangeSource(EDGE_LENGTH_PROPERTY, this.edgeLength));
            Options.getInstance().setDirty();
        }
    }

    @Override
    public String toString() {
        return "UiOptions [showNumbers=" + showNumbers + ", gravity=" + gravity + ", edgeLength=" + edgeLength
                + ", musicianComponents=" + musicianComponents + "]";
    }

    /**
     * Creates a copy of the {@link MusicianComponentOptions} with id
     * {@code musicianIdToCopy} and stores it with the id {@code id}
     * 
     * @param musicianIdToCopy id of the component to copy
     * @param id               new ID
     */
    public void copyMusicianComponentOptions(int musicianIdToCopy, int id) {
        var orig = musicianComponents.get(musicianIdToCopy);
        if (orig == null)
            throw new IllegalArgumentException(
                    "Somehow tried to make a copy of musician component " + musicianIdToCopy + " which doesn't exist");
        var copy = orig.copy();
        musicianComponents.put(id, copy);
    }

    /**
     * @param property property being listened for
     * @param listener listener
     */
    public void addChangeListener(String property, ChangeListener listener) {
        emitter.addChangeListener(property, listener);
    }

}