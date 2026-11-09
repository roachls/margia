package org.roach.margia.view;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Global;
import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

import javafx.animation.PauseTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * GUI element that displays an agent as a colored circle
 */
@SuppressWarnings({ "java:S1948", "java:S115" })
public class MusicianComponent extends Pane implements PropertyChangeListener, ChangeListener {
    private final Musician musician;
    private Color color;
    private static int tickLengthMillis;

    // Replaces javax.swing.Timer for one-shot note flash/highlight decay
    private PauseTransition flashTimer;
    private boolean selected;

    private Vector2D velocity = new Vector2D(0, 0);
    private Vector2D force = new Vector2D(0, 0);
    private static final double DAMPING = 0.6; // Damping factor
    private static final double TIMESTEP = 0.8; // Simulation speed/stability
    private final MusicianComponentOptions options;

    private static boolean showNumbers = Options.getInstance().getUiOptions().isShowNumbers();

    /**
     * Listener for the show numbers property
     */
    static final ChangeListener SHOW_NUMBERS_LISTENER = e -> showNumbers = (boolean) ((ChangeSource) e.getSource())
            .newValue();
    static final ChangeListener TEMPO_LISTENER = _ -> tickLengthMillis = Length
            .getMillisForTempo(1, Options.getInstance().getMusicOptions().getTempo()).getValue().intValue();

    static {
        var tempo = Options.getInstance().getMusicOptions().getTempo();
        tickLengthMillis = Length.getMillisForTempo(1, tempo).getValue().intValue();
    }

    static final double SELECTED_LINE_WIDTH = 2.0;

    private static final Font LOCK_FONT = Font.font("SansSerif", 15);

    private static final double ρ = 0.00009;

    // Drawing surface sized to match this component's diameter
    private final Canvas canvas = new Canvas();

    /**
     * @param musician the {@link Musician} being displayed
     */
    MusicianComponent(Musician musician) {
        this.musician = musician;
        this.options = Options.getInstance().getUiOptions().getMusicianComponents().computeIfAbsent(musician.getId(),
                _ -> new MusicianComponentOptions());

        // Bind canvas dimensions to the Pane's width/height
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        options.addChangeListener(MusicianComponentOptions.RADIUS_PROPERTY, this);
        options.addChangeListener(MusicianComponentOptions.POSITION_PROPERTY, new PositionChangeListener());

        updatePosition();
        setId("Musician_" + musician.getId());
        musician.addPropertyChangeListener(this);
        this.color = Color.BLACK;
        updateSize(options.getRadius());
    }

    private class PositionChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (musician.isMuted())
                return;
            if (e.getSource() instanceof ChangeSource(_, Point2D.Double pos)) {
                if (Options.getInstance().getMidiOptions().isSendPanMessage()) {
                    sendStereoPanning(pos);
                }
                if (Options.getInstance().getMidiOptions().isSendVerticalPanMessage()) {
                    sendVerticalPanning(pos);
                }
            }
        }

        private void sendVerticalPanning(Point2D.Double pos) {
            double height;
            int verticalPanValue;
            if (Options.getInstance().getMidiOptions().isVerticalPanWithRelativeLocations()) {
                height = Global.getMaxComponentY() - Global.getMinComponentY();
                // get distance from max because we want the "top" (i.e., smaller y's) to send
                // higher numbers
                verticalPanValue = height == 0 ? 63 : (int) (127d * (Global.getMaxComponentY() - pos.y) / height);
            } else {
                height = Global.getScreenHeight();
                // use "height - pos.y" instead of just pos.y because we want the "top" (i.e.,
                // smaller y's) to send higher numbers
                verticalPanValue = height == 0 ? 63 : (int) (127d * (height - pos.y) / height);
            }
            MidiController.getInstance().sendControlChange(musician.getOptions().getDeviceName(), musician.getChannel(),
                    Options.getInstance().getMidiOptions().getVerticalPanController(), verticalPanValue);
        }

        private void sendStereoPanning(Point2D.Double pos) {
            double width;
            int panValue;
            if (Options.getInstance().getMidiOptions().isPanWithRelativeLocations()) {
                width = Global.getMaxComponentX() - Global.getMinComponentX();
                panValue = width == 0 ? 63 : (int) (127d * (pos.x - Global.getMinComponentX()) / width);
            } else {
                width = Global.getScreenWidth();
                panValue = width == 0 ? 63 : (int) (127d * pos.x / width);
            }
            MidiController.getInstance().sendControlChange(musician.getOptions().getDeviceName(), musician.getChannel(),
                    Options.getInstance().getMidiOptions().getPanController(), panValue);
        }

    }

    // Replace the PauseTransition field from earlier with Timeline since this timer
    // repeats to fade brightness
    private javafx.animation.Timeline fadeTimeline;

    @Override
    @SuppressWarnings("java:S1301")
    public void propertyChange(PropertyChangeEvent evt) {
        javafx.application.Platform.runLater(() -> {
            var brightness = new java.util.concurrent.atomic.AtomicReference<>(1.0);
            switch (evt.getPropertyName()) {
            case Musician.LAST_CHORD_PROPERTY -> {
                Chord chord = (Chord) evt.getNewValue();
                if (fadeTimeline != null) {
                    fadeTimeline.stop();
                }
                if (Musician.REST.equals(chord)) {
                    this.color = javafx.scene.paint.Color.BLACK;
                    draw();
                } else {
                    // JavaFX Color.hsb expects hue in, saturation in, brightness in [0, 1]
                    double hueDegrees = (chord.averageNote() / 127.0) * 360.0;
                    double normalizedSaturation = Math.clamp(chord.getVelocity() / 127.0, 0.0, 1.0);

                    var noteInfoMillis = tickLengthMillis * chord.getLength();
                    var delayMillis = Math.max(1.0, 1000.0 / noteInfoMillis);
                    var brightnessOffset = 1.0 / noteInfoMillis;
                    brightness.set(1.0);

                    fadeTimeline = new javafx.animation.Timeline(
                            new javafx.animation.KeyFrame(javafx.util.Duration.millis(delayMillis), _ -> {
                                color = javafx.scene.paint.Color.hsb(hueDegrees, normalizedSaturation,
                                        brightness.get());
                                brightness.getAndAccumulate(brightnessOffset, (b, f) -> Math.max(0.0, b - f));
                                draw();
                            }));
                    fadeTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
                    fadeTimeline.play();
                }
            }
            default -> {
                // TODO
            }
            }
        });
    }

    void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double radius = options.getRadius();

        // Radial gradient from offset highlight (radius + 3, radius + 3) to outer color
        var gradient = new javafx.scene.paint.RadialGradient(0, // focusAngle
                0, // focusDistance
                radius + 3.0, // centerX
                radius + 3.0, // centerY
                radius, // radius
                false, // proportional (false = pixel coordinates, matching Swing's
                       // RadialGradientPaint)
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, javafx.scene.paint.Color.WHITE),
                new javafx.scene.paint.Stop(1.0, color));

        gc.setFill(gradient);
        gc.fillOval(0, 0, w, h);

        if (showNumbers) {
            var font = javafx.scene.text.Font.font("Serif", javafx.scene.text.FontWeight.BOLD, 12.0 * radius / 20.0);
            String idText = Integer.toString(musician.getId());

            // Measure text bounds using a temporary Text node (replaces
            // TextLayout.getOutline)
            var textHelper = new javafx.scene.text.Text(idText);
            textHelper.setFont(font);
            var bounds = textHelper.getLayoutBounds();

            double textX = radius - 6.0;
            double textY = radius + 3.0; // Baseline Y coordinate

            // Draw black background oval around the ID number
            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.fillOval(textX - 1.0, textY - bounds.getHeight() + 2.0, bounds.getWidth() + 2.0,
                    bounds.getHeight() + 2.0);

            // Draw white ID number
            gc.setFont(font);
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillText(idText, textX, textY);

            // Draw status icons for locked / not listening
            String status = "";
            if (options.isLocked()) {
                status += "🔒";
            }
            if (!musician.isListening()) {
                status += "🎤\u033D";
            }
            if (!status.isEmpty()) {
                gc.setFont(LOCK_FONT);
                gc.setFill(javafx.scene.paint.Color.YELLOW);
                gc.fillText(status, 0, h - 2.0);
            }
        }

        if (musician.isMuted()) {
            // Semi-transparent gray oval over the whole circle
            gc.setFill(javafx.scene.paint.Color.color(0.5, 0.5, 0.5, 0.8));
            gc.fillOval(0, 0, w, h);
        }

        if (selected) {
            gc.setStroke(javafx.scene.paint.Color.RED);
            gc.setLineWidth(SELECTED_LINE_WIDTH);
            gc.setLineDashes(null); // Ensure solid line
            gc.strokeRect(1.0, 1.0, w - 2.0, h - 2.0);
        }
    }

    void updateLocation() {
        var position = options.getPosition();
        setLayoutX(position.getX());
        setLayoutY(position.getY());
    }

    /**
     * @return radius of displayed circle
     */
    int getRadius() { return options.getRadius(); }

    /**
     * @return diameter of displayed circle
     */
    int getDiameter() { return options.getRadius() * 2; }

    /**
     * @param circleRadius radius of displayed circle
     */
    void updateSize(int circleRadius) {
        double diameter = getDiameter();
        setPrefSize(diameter, diameter);
        setMinSize(diameter, diameter);
        setMaxSize(diameter, diameter);
        resize(diameter, diameter);
        draw();
    }

    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String property, Object newVal)
                && MusicianComponentOptions.RADIUS_PROPERTY.equals(property)) {
            javafx.application.Platform.runLater(() -> this.updateSize((int) newVal));
        }
    }

    /**
     * @param tickLengthMillis tick length in milliseconds, used for animations
     */
    public static void setTickLengthMillis(int tickLengthMillis) {
        MusicianComponent.tickLengthMillis = tickLengthMillis;
    }

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
        draw();
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

    Point2D.Double getPosition() { return options.getPosition(); }

    void updatePosition() {
        updateLocation();
    }

    Vector2D getVelocity() { return velocity; }

    void setVelocity(double x, double y) {
        if (options.isLocked())
            return;
        velocity = new Vector2D(x, y);
    }

    Vector2D getForce() { return force; }

    void setForce(double x, double y) {
        if (options.isLocked())
            return;
        force = new Vector2D(x, y);
    }

    void setForce(Vector2D force) {
        if (options.isLocked())
            return;
        this.force = force;
    }

    void resetForce() {
        this.force = new Vector2D(0, 0);
    }

    Point2D.Double getCenter() {
        var position = options.getPosition();
        return new Point2D.Double(position.x + getWidth() / 2.0, position.y + getHeight() / 2.0);
    }

    void applyForces() {
        var scaledForceVec = force.divide(getMass()).multiply(TIMESTEP);
        velocity = velocity.add(scaledForceVec).multiply(DAMPING);
        var scaledVelocity = velocity.multiply(TIMESTEP);
        options.setPosition(PointMath.movePoint(options.getPosition(), scaledVelocity));
    }

    /**
     * Clamp this component's position to within 1 radius of the boundaries given.
     * 
     * @param width
     * @param height
     */
    void clampPosition(double width, double height) {
        var radius = options.getRadius();
        if (width <= radius * 4d || height <= radius * 4d)
            return;
        /*
         * Note that the position is the upper-left corner of the bounding rectangle,
         * not the center, which is why we multiply radius by 3 for the bounds.
         */
        var position = options.getPosition();
        options.setPosition(new Point2D.Double(Math.clamp(position.getX(), radius, width - radius * 3.0),
                Math.clamp(position.getY(), radius, height - radius * 3.0)));
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

}
