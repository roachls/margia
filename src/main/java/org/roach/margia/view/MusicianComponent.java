package org.roach.margia.view;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Global;
import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * GUI element that displays an agent as a colored circle
 */
@SuppressWarnings({ "java:S1948", "java:S115" })
public class MusicianComponent extends JComponent implements PropertyChangeListener, ChangeListener {
    private static final float[] FRACTIONS = new float[] { 0.0f, 1.0f };
    private final Musician musician;
    private Color color;
    private static int tickLengthMillis;
    private Timer timer;
    private boolean selected;

    private Vector2D velocity = new Vector2D(0, 0);
    private Vector2D force = new Vector2D(0, 0);
    private static final double DAMPING = 0.6; // Damping factor
    private static final double TIMESTEP = 0.8; // Simulation speed/stability
    private final MusicianComponentOptions options;

    private static boolean showNumbers = Options.getInstance().getUiOptions().isShowNumbers();
    /**
     * listener for the show numbers property
     */
    static final ChangeListener SHOW_NUMBERS_LISTENER = e -> showNumbers = (boolean) ((ChangeSource) e.getSource())
            .newValue();
    static final ChangeListener TEMPO_LISTENER = _ -> tickLengthMillis = Length
            .getMillisForTempo(1, Options.getInstance().getMusicOptions().getTempo()).getValue().intValue();

    static {
        var tempo = Options.getInstance().getMusicOptions().getTempo();
        tickLengthMillis = Length.getMillisForTempo(1, tempo).getValue().intValue();
    }

    static final Stroke SELECTED_STROKE = new BasicStroke(2.0f);

    private static final Font LOCK_FONT = new Font("SansSerif", Font.PLAIN, 15);

    private static final double ρ = 0.00009;

    /**
     * @param musician         the {@link Musician} being displayed
     * @param tickLengthMillis length of a tick in milliseconds
     */
    MusicianComponent(Musician musician) {
        this.musician = musician;
        this.options = Options.getInstance().getUiOptions().getMusicianComponents().computeIfAbsent(musician.getId(),
                _ -> new MusicianComponentOptions());
        options.addChangeListener(MusicianComponentOptions.RADIUS_PROPERTY, this);
        options.addChangeListener(MusicianComponentOptions.POSITION_PROPERTY, new PositionChangeListener());
        updatePosition();
        this.setName("Musician_" + musician.getId());
        musician.addPropertyChangeListener(this);
        this.color = Color.black;
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
            MidiController.getInstance().sendControlChange(musician.getOptions().getBusName(), musician.getChannel(),
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
            MidiController.getInstance().sendControlChange(musician.getOptions().getBusName(), musician.getChannel(),
                    Options.getInstance().getMidiOptions().getPanController(), panValue);
        }

    }

    @Override
    @SuppressWarnings("java:S1301")
    public void propertyChange(PropertyChangeEvent evt) {
        var brightness = new AtomicReference<Float>(1.0f);
        switch (evt.getPropertyName()) {
        case Musician.LAST_CHORD_PROPERTY:
            Chord chord = (Chord) evt.getNewValue();
            if (timer != null)
                timer.stop();
            if (Musician.REST.equals(chord)) {
                this.color = Color.black;
                repaint();
            } else {
                // Hue depends on average MIDI note played (over the who chord), relative to the
                // full range of the musician
                float normalizedHue = chord.averageNote() / 127.0f;
                // Saturation depends on velocity of MIDI note
                float normalizedSaturation = chord.getVelocity() / 127f;
                // Start the brightness at full (1.0) and decrease it to 0 over the life of the
                // note
                var noteInfoMillis = tickLengthMillis * chord.getLength();
                var delayMillis = 1000 / noteInfoMillis;
                var brightnessOffset = 1.0f / noteInfoMillis;
                brightness.set(1.0f);
                timer = new Timer(delayMillis, _ -> {
                    color = Color.getHSBColor(normalizedHue, normalizedSaturation, brightness.get());
                    brightness.getAndAccumulate(brightnessOffset, (b, f) -> Math.max(0f, b - f));
                    repaint();
                });
                timer.start();
            }
            break;
        default:
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2d = (Graphics2D) g.create();
        // Enable anti-aliasing for shapes/lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Enable anti-aliasing for text (use VALUE_TEXT_ANTIALIAS_GASP for potentially
        // better results)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        g2d.setColor(color);
        var radius = options.getRadius();
        var paint = new RadialGradientPaint(new Point2D.Float(radius + 3f, radius + 3f), radius, FRACTIONS,
                new Color[] { Color.white, color });
        g2d.setPaint(paint);
        g2d.fillOval(0, 0, getWidth(), getHeight());

        if (showNumbers) {
            // Create a TextLayout to get the text's shape
            var font = new Font("Serif", Font.BOLD, 12 * radius / 20);
            var frc = g2d.getFontRenderContext();
            var tl = new TextLayout(Integer.toString(musician.getId()), font, frc);

            // Get the outline shape
            // AffineTransform is used to position the text at (x, y)
            var transform = AffineTransform.getTranslateInstance(radius - 6d, radius + 3d);
            Shape shape = tl.getOutline(transform);

            // draw an oval around the text
            g2d.setColor(Color.BLACK); // Outline color
            g2d.fillOval(shape.getBounds().x - 1, shape.getBounds().y - 1, shape.getBounds().width + 2,
                    shape.getBounds().height + 2);
            g2d.setColor(Color.white); // Fill color
            // draw the text
            g2d.fill(shape);

            // draw icons for locked/listening
            String status = "";
            if (options.isLocked())
                status += "🔒";
            if (!musician.isListening())
                status += "🎤\u033D";
            g2d.setFont(LOCK_FONT);
            g2d.setColor(Color.yellow);
            g2d.drawString(status, 0, getHeight());
            g2d.setFont(font);
        }
        if (musician.isMuted()) {
            // draw a semi-transparent gray oval over the whole thing
            g2d.setColor(new Color(0.5f, 0.5f, 0.5f, 0.8f));
            g2d.fillOval(0, 0, getWidth(), getHeight());
        }
        if (selected) {
            g2d.setColor(Color.red);
            g2d.setStroke(SELECTED_STROKE);
            g2d.drawRect(0, 0, getWidth() - 2, getHeight() - 2);
        }
    }

    private void updateLocation() {
        var position = options.getPosition();
        setLocation((int) position.getX(), (int) position.getY());
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
        var diameter = getDiameter();
        var dim = new Dimension(diameter, diameter);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setSize(dim);
        setMaximumSize(dim);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof

        ChangeSource(String property, Object newVal) && MusicianComponentOptions.RADIUS_PROPERTY.equals(property)) {
            this.updateSize((int) newVal);
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
                + ", timer=" + timer + ", options=" + options + ", velocity=" + velocity + ", force=" + force + "]";
    }

    /**
     * @return the wrapped Musician
     */
    public Musician getMusician() { return this.musician; }

    boolean isSelected() { return selected; }

    void setSelected(boolean selected) { this.selected = selected; }

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
    void clampPosition(int width, int height) {
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
