package org.roach.margia.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Global;
import org.roach.margia.storage.Options;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.util.Duration;

/**
 * 3D GUI element that displays an agent as a shaded sphere in 3D space.
 */
@SuppressWarnings({ "java:S1948", "java:S115" })
public class MusicianComponent extends Group implements PropertyChangeListener {
    private final Musician musician;
    private Color color = Color.BLACK;
    private static int tickLengthMillis;

    private boolean selected;

    private Point3D velocity = Point3D.ZERO;
    private Point3D force = Point3D.ZERO;
    private static final double DAMPING = 0.6; // Damping factor
    private static final double TIMESTEP = 0.8; // Simulation speed/stability
    private final MusicianComponentOptions options;

    private static boolean showNumbers = Options.getInstance().getUiOptions().isShowNumbers();

    /**
     * Listener for the show numbers property
     */
    static final ChangeListener<Boolean> SHOW_NUMBERS_LISTENER = (_, _, newVal) -> showNumbers = newVal;
    static final ChangeListener<? super Number> TEMPO_LISTENER = (_, _, _) -> tickLengthMillis = Length
            .getMillisForTempo(1, Options.getInstance().getMusicOptions().getTempo()).getValue().intValue();

    static {
        var tempo = Options.getInstance().getMusicOptions().getTempo();
        tickLengthMillis = Length.getMillisForTempo(1, tempo).getValue().intValue();
    }

    private static final double ρ = 0.00009;

    // 3D Scene Graph Nodes
    private final Sphere sphere = new Sphere();
    private final PhongMaterial sphereMaterial = new PhongMaterial();
    private final Box selectionBox = new Box();
    private final PhongMaterial selectionMaterial = new PhongMaterial(Color.RED);

    private final ChangeListener<Boolean> mutedListener = (_, _, _) -> Platform.runLater(this::updateAppearance);
    private final ChangeListener<Number> radiusListener = (_, _, newVal) -> Platform
            .runLater(() -> updateSize(newVal.intValue()));
    private final PositionChangeListener positionListener = new PositionChangeListener();

    private BiConsumer<MusicianComponent, Color> onNotePlayedCallback;
    private Timeline fadeTimeline;

    /**
     * @param musician the {@link Musician} being displayed
     */
    MusicianComponent(Musician musician) {
        this.musician = musician;
        this.options = Options.getInstance().getUiOptions().getMusicianComponents().computeIfAbsent(musician.getId(),
                _ -> {
                    var defaultCompOpts = Options.getInstance().getDefaultMusicianComponentOptions();
                    var newOpts = new MusicianComponentOptions();
                    newOpts.setRadius(defaultCompOpts.getRadius());
                    newOpts.setLocked(defaultCompOpts.isLocked());
                    return newOpts;
                });

        // Configure primary 3D sphere
        sphereMaterial.setSpecularColor(Color.WHITE);
        sphereMaterial.setSpecularPower(32.0);
        sphere.setMaterial(sphereMaterial);
        sphere.radiusProperty().bind(options.radiusProperty());

        // Configure 3D wireframe selection indicator
        selectionBox.setMaterial(selectionMaterial);
        selectionBox.setDrawMode(DrawMode.LINE);
        selectionBox.setCullFace(CullFace.NONE);
        selectionBox.setMouseTransparent(true);
        selectionBox.setVisible(false);

        getChildren().addAll(sphere, selectionBox);

        options.radiusProperty().addListener(radiusListener);
        options.positionProperty().addListener(positionListener);
        musician.getOptions().mutedProperty().addListener(mutedListener);

        setId("Musician_" + musician.getId());
        musician.addPropertyChangeListener(this);

        updateSize(options.getRadius());
        updateLocation();
        updateAppearance();
    }

    private class PositionChangeListener implements ChangeListener<Point3D> {
        private int lastSentDepthCC = -1; // Field on PositionChangeListener to avoid duplicate MIDI messages
        private int lastSentStereoCC = -1; // Field on PositionChangeListener to avoid duplicate MIDI messages
        private int lastSentVerticalCC = -1; // Field on PositionChangeListener to avoid duplicate MIDI messages

        @Override
        public void changed(ObservableValue<? extends Point3D> observable, Point3D oldValue, Point3D newValue) {
            if (newValue == null || musician.isMuted()) {
                return;
            }
            var midiOpts = Options.getInstance().getMidiOptions();
            if (midiOpts.isSendPanMessage()) {
                sendStereoPanning(newValue);
            }
            if (midiOpts.isSendVerticalPanMessage()) {
                sendVerticalPanning(newValue);
            }
            if (Options.getInstance().getUiOptions().isThreeDMode() && midiOpts.isSendDepthMessage()) {
                sendDepthPanning(newValue);
            }
        }

        private void sendDepthPanning(Point3D pos) {
            var midiOpts = Options.getInstance().getMidiOptions();
            int depthValue;
            if (midiOpts.isDepthWithRelativeLocations()) {
                double depthSpan = Global.getMaxComponentZ() - Global.getMinComponentZ();
                // Further away (+Z) -> higher CC (127); closer (-Z) -> lower CC (0)
                depthValue = depthSpan == 0 ? 63
                        : (int) Math.clamp(127d * (pos.getZ() - Global.getMinComponentZ()) / depthSpan, 0, 127);
            } else {
                // Map full 3D world cube [-WORLD_HALF_EXTENT, +WORLD_HALF_EXTENT] to [0, 127]
                double worldDepth = AgentPanel.WORLD_HALF_EXTENT * 2.0;
                double normalizedZ = pos.getZ() + AgentPanel.WORLD_HALF_EXTENT;
                depthValue = worldDepth == 0 ? 63 : (int) Math.clamp(127d * normalizedZ / worldDepth, 0, 127);
            }

            // Only transmit when the integer 0-127 MIDI value actually changes
            if (depthValue != lastSentDepthCC) {
                lastSentDepthCC = depthValue;
                MidiController.getInstance().sendControlChange(musician.getOptions().getDeviceName(),
                        musician.getChannel(), midiOpts.getDepthController(), depthValue);
            }
        }

        private void sendVerticalPanning(Point3D pos) {
            double height;
            int verticalPanValue;
            if (Options.getInstance().getMidiOptions().isVerticalPanWithRelativeLocations()) {
                height = Global.getMaxComponentY() - Global.getMinComponentY();
                // Higher Y in 3D space is downward on screen, so invert for MIDI CC
                verticalPanValue = height == 0 ? 63 : (int) (127d * (Global.getMaxComponentY() - pos.getY()) / height);
            } else {
                height = Global.getScreenHeight();
                // Map centered 3D Y coordinates [-height/2, +height/2] to [0, 127]
                double normalizedY = (height / 2.0) - pos.getY();
                verticalPanValue = height == 0 ? 63 : (int) Math.clamp(127d * normalizedY / height, 0, 127);
            }
            if (verticalPanValue != lastSentVerticalCC) {
                lastSentVerticalCC = verticalPanValue;
                MidiController.getInstance().sendControlChange(musician.getOptions().getDeviceName(),
                        musician.getChannel(), Options.getInstance().getMidiOptions().getVerticalPanController(),
                        verticalPanValue);
            }
        }

        private void sendStereoPanning(Point3D pos) {
            double width;
            int panValue;
            if (Options.getInstance().getMidiOptions().isPanWithRelativeLocations()) {
                width = Global.getMaxComponentX() - Global.getMinComponentX();
                panValue = width == 0 ? 63 : (int) (127d * (pos.getX() - Global.getMinComponentX()) / width);
            } else {
                width = Global.getScreenWidth();
                // Map centered 3D X coordinates [-width/2, +width/2] to [0, 127]
                double normalizedX = pos.getX() + (width / 2.0);
                panValue = width == 0 ? 63 : (int) Math.clamp(127d * normalizedX / width, 0, 127);
            }
            if (lastSentStereoCC != panValue) {
                panValue = lastSentStereoCC;
                MidiController.getInstance().sendControlChange(musician.getOptions().getDeviceName(),
                        musician.getChannel(), Options.getInstance().getMidiOptions().getPanController(), panValue);
            }
        }
    }

    @Override
    @SuppressWarnings("java:S1301")
    public void propertyChange(PropertyChangeEvent evt) {
        Platform.runLater(() -> {
            var brightness = new AtomicReference<>(1.0);
            switch (evt.getPropertyName()) {
            case Musician.LAST_CHORD_PROPERTY -> {
                Chord chord = (Chord) evt.getNewValue();
                if (fadeTimeline != null) {
                    fadeTimeline.stop();
                }
                if (Musician.REST.equals(chord)) {
                    this.color = Color.BLACK;
                    updateAppearance();
                } else {
                    double hueDegrees = (chord.averageNote() / 127.0) * 360.0;
                    double normalizedSaturation = Math.clamp(chord.getVelocity() / 127.0, 0.0, 1.0);

                    var noteInfoMillis = tickLengthMillis * chord.getLength();
                    var delayMillis = Math.max(1.0, 1000.0 / noteInfoMillis);
                    var brightnessOffset = 1.0 / noteInfoMillis;
                    brightness.set(1.0);

                    fadeTimeline = new Timeline(new KeyFrame(Duration.millis(delayMillis), _ -> {
                        double currentBrightness = brightness.get();
                        color = Color.hsb(hueDegrees, normalizedSaturation, currentBrightness);
                        updateAppearance();
                        if (currentBrightness <= 0.0) {
                            fadeTimeline.stop();
                        } else {
                            brightness.set(Math.max(0.0, currentBrightness - brightnessOffset));
                        }
                    }));
                    fadeTimeline.setCycleCount(Animation.INDEFINITE);
                    fadeTimeline.play();

                    if (onNotePlayedCallback != null && !this.getMusician().isMuted()) {
                        var rippleColor = Color.hsb(hueDegrees, Math.max(0.65, normalizedSaturation), 1.0);
                        onNotePlayedCallback.accept(this, rippleColor);
                    }
                }
            }
            default -> {
                // do nothing
            }
            }
        });
    }

    void setOnNotePlayedCallback(BiConsumer<MusicianComponent, Color> callback) {
        this.onNotePlayedCallback = callback;
    }

    /**
     * Updates the 3D sphere material and wireframe selection indicator.
     */
    void updateAppearance() {
        Color displayColor = musician.isMuted() ? color.interpolate(Color.GRAY, 0.75) : color;
        sphereMaterial.setDiffuseColor(displayColor);
        sphereMaterial.setSpecularColor(musician.isMuted() ? Color.DARKGRAY : Color.WHITE);
        selectionBox.setVisible(selected);
    }

    void updateLocation() {
        var position = options.getPosition();
        if (position != null) {
            setTranslateX(position.getX());
            setTranslateY(position.getY());
            setTranslateZ(position.getZ());
        }
    }

    /**
     * @return radius of displayed sphere
     */
    int getRadius() { return options.getRadius(); }

    /**
     * @return diameter of displayed sphere
     */
    int getDiameter() { return options.getRadius() * 2; }

    /**
     * @param sphereRadius radius of displayed sphere
     */
    void updateSize(int sphereRadius) {
        double boxExtent = sphereRadius * 2.3;
        selectionBox.setWidth(boxExtent);
        selectionBox.setHeight(boxExtent);
        selectionBox.setDepth(boxExtent);
    }

    /**
     * @param tickLengthMillis tick length in milliseconds, used for animations
     */
    public static void setTickLengthMillis(int tickLengthMillis) {
        MusicianComponent.tickLengthMillis = tickLengthMillis;
    }

    static boolean isShowNumbers() { return showNumbers; }

    Color getColor() { return this.color; }

    @Override
    public int hashCode() {
        return Objects.hash(musician.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MusicianComponent other = (MusicianComponent) obj;
        return Objects.equals(musician.getId(), other.musician.getId());
    }

    @Override
    public String toString() {
        return "MusicianComponent [musician=" + musician + ", color=" + color + ", tickLengthMillis=" + tickLengthMillis
                + ", options=" + options + ", velocity=" + velocity + ", force=" + force + "]";
    }

    /**
     * @return the wrapped Musician
     */
    public Musician getMusician() { return this.musician; }

    boolean isSelected() { return selected; }

    void setSelected(boolean selected) {
        this.selected = selected;
        updateAppearance();
    }

    /**
     * Mass is ρ * 4/3 * πr^3, where ρ = density
     *
     * @return the mass of the component
     */
    double getMass() {
        var r = options.getRadius();
        return (4d / 3d) * Math.PI * Math.pow(r, 3) * ρ;
    }

    Point3D getPosition() { return options.getPosition(); }

    void updatePosition() {
        updateLocation();
    }

    Point3D getVelocity() { return velocity; }

    void setVelocity(double x, double y, double z) {
        if (options.isLocked())
            return;
        velocity = new Point3D(x, y, z);
    }

    Point3D getForce() { return force; }

    void setForce(double x, double y, double z) {
        if (options.isLocked())
            return;
        force = new Point3D(x, y, z);
    }

    void setForce(Point3D force) {
        if (options.isLocked())
            return;
        this.force = force;
    }

    void resetForce() {
        this.force = Point3D.ZERO;
    }

    /**
     * Since the Group is centered at (0,0,0), the center is identical to the
     * position.
     *
     * @return 3D center coordinates
     */
    Point3D getCenter() { return options.getPosition(); }

    void applyForces() {
        if (options.isLocked()) {
            velocity = Point3D.ZERO;
            return;
        }
        var scaledForceVec = force.multiply(TIMESTEP / getMass());
        velocity = velocity.add(scaledForceVec).multiply(DAMPING);
        var scaledVelocity = velocity.multiply(TIMESTEP);
        options.setPosition(options.getPosition().add(scaledVelocity));
    }

    /**
     * Clamps this component's 3D position symmetrically around the origin (0, 0, 0)
     * within [-halfExtent + radius, +halfExtent - radius].
     *
     * @param halfExtent half-width of the 3D bounding cube
     */
    void clampPosition(double halfExtent) {
        double radius = options.getRadius();
        double limit = halfExtent - radius;
        if (limit <= 0) {
            return;
        }
        var pos = options.getPosition();
        options.setPosition(new Point3D(Math.clamp(pos.getX(), -limit, limit), Math.clamp(pos.getY(), -limit, limit),
                Math.clamp(pos.getZ(), -limit, limit)));
        updatePosition();
    }

    MusicianComponentOptions getOptions() { return options; }

    void toggleLocked() {
        options.setLocked(!options.isLocked());
    }

    boolean isLocked() { return options.isLocked(); }

    void setLocked(boolean locked) {
        options.setLocked(locked);
    }

    static int getTickLengthMillis() { return tickLengthMillis; }

    void dispose() {
        musician.removePropertyChangeListener(this);
        options.radiusProperty().removeListener(radiusListener);
        options.positionProperty().removeListener(positionListener);
        musician.getOptions().mutedProperty().removeListener(mutedListener);
        if (fadeTimeline != null) {
            fadeTimeline.stop();
        }
    }

    void reattach() {
        musician.addPropertyChangeListener(this);
        options.radiusProperty().addListener(radiusListener);
        options.positionProperty().addListener(positionListener);
        musician.getOptions().mutedProperty().addListener(mutedListener);
    }
}