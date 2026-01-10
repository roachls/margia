package org.roach.margia.ui;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.*;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;
import org.roach.margia.util.Range;

/**
 * GUI element that displays an agent as a colored circle
 */
@SuppressWarnings({ "java:S1948" })
public class MusicianComponent extends JComponent implements PropertyChangeListener, ChangeListener {
    private static final float[] FRACTIONS = new float[] { 0.0f, 1.0f };
    private final Musician musician;
    private Color color;
    private static int tickLengthMillis;
    private Timer timer;
    private boolean selected;
    private boolean locked;

    /**
     * size of circle to draw
     */
    private int radius = MusicianComponent.DEFAULT_RADIUS;
    private double mass = 1.0;
    private final Point2D.Double position = new Point2D.Double();
    private Vector2D velocity = new Vector2D(0, 0);
    private Vector2D force = new Vector2D(0, 0);
    private static final double DAMPING = 0.6; // Damping factor
    private static final double TIMESTEP = 0.8; // Simulation speed/stability

    /**
     * default radius of musician components
     */
    static final int DEFAULT_RADIUS = 20;
    /**
     * property name of radius spinner
     */
    static final String RADIUS_PROPERTY = "ui.radius";
    static final String MASS_PROPERTY = "ui.mass";
    /**
     * property name of whether to show numbers
     */
    static final String SHOW_NUMBERS_PROPERTY = "ui.show_numbers";
    private static boolean showNumbers = Options.getInstance().getOrDefaultAsBoolean(SHOW_NUMBERS_PROPERTY, true);

    /**
     * listener for the show numbers property
     */
    static final ChangeListener SHOW_NUMBERS_LISTENER = e -> showNumbers = (boolean) ((ChangeSource) e.getSource())
            .newValue();

    static final Stroke SELECTED_STROKE = new BasicStroke(2.0f);

    private static final Font LOCK_FONT = new Font("SansSerif", Font.PLAIN, 12);

    /**
     * @param musician         the {@link Musician} being displayed
     * @param tickLengthMillis length of a tick in milliseconds
     */
    MusicianComponent(Musician musician) {
        this.musician = musician;
        this.setName("Musician_" + musician.getId());
        musician.addPropertyChangeListener(this);
        this.color = Color.black;
        setCircleRadius(Options.getInstance().getOrDefaultAsInt(RADIUS_PROPERTY, DEFAULT_RADIUS));
    }

    @Override
    @SuppressWarnings("java:S1301")
    public void propertyChange(PropertyChangeEvent evt) {
        var brightness = new AtomicReference<Float>(1.0f);
        switch (evt.getPropertyName()) {
        case Musician.LAST_NOTE_PROPERTY:
            NoteInfo noteInfo = (NoteInfo) evt.getNewValue();
            if (timer != null)
                timer.stop();
            if (noteInfo.noteNum() == Note.REST) {
                this.color = Color.black;
                repaint();
            } else {
                // Hue depends on MIDI note played, relative to the full range of the musician
                float normalizedHue = musician.noteToRange(noteInfo.noteNum());
                // Saturation depends on velocity of MIDI note
                float normalizedSaturation = noteInfo.velocity() / 127f;
                // Start the brightness at full (1.0) and decrease it to 0 over the life of the
                // note
                var noteInfoMillis = tickLengthMillis * noteInfo.length();
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
        if (locked) {
            // draw yellow circle border
            var font = g2d.getFont();
            g2d.setFont(LOCK_FONT);
            g2d.setColor(Color.yellow);
            g2d.drawString("🔒", 0, getHeight());
            g2d.setFont(font);
        }
    }

    private void updateLocation() {
        setLocation((int) position.getX(), (int) position.getY());
    }

    /**
     * @return radius of displayed circle
     */
    int getRadius() { return radius; }

    /**
     * @return diameter of displayed circle
     */
    int getDiameter() { return radius * 2; }

    /**
     * @param circleRadius radius of displayed circle
     */
    void setCircleRadius(int circleRadius) {
        this.radius = Math.max(0, circleRadius);
        var dim = new Dimension(circleRadius * 2, circleRadius * 2);
        setPreferredSize(dim);
        setMinimumSize(dim);
        setSize(dim);
        setMaximumSize(dim);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource cs && MusicianComponent.RADIUS_PROPERTY.equals(cs.key())) {
            this.setCircleRadius((int) cs.newValue());
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
                + ", timer=" + timer + ", radius=" + radius + ", mass=" + mass + ", position=" + position
                + ", velocity=" + velocity + ", force=" + force + "]";
    }

    /**
     * @return the wrapped Musician
     */
    public Musician getMusician() { return this.musician; }

    boolean isSelected() { return selected; }

    void setSelected(boolean selected) { this.selected = selected; }

    double getMass() { return mass; }

    Point2D.Double getPosition() { return position; }

    void setPosition(double x, double y) {
        if (locked)
            return;
        position.setLocation(x, y);
        updateLocation();
    }

    Vector2D getVelocity() { return velocity; }

    void setVelocity(double x, double y) {
        if (locked)
            return;
        velocity = new Vector2D(x, y);
    }

    Vector2D getForce() { return force; }

    void setForce(double x, double y) {
        if (locked)
            return;
        force = new Vector2D(x, y);
    }

    void setForce(Vector2D force) {
        if (locked)
            return;
        this.force = force;
    }

    void resetForce() {
        this.force = new Vector2D(0, 0);
    }

    Point2D.Double getCenter() {
        return new Point2D.Double(position.x + getWidth() / 2.0, position.y + getHeight() / 2.0);
    }

    void applyForces() {
        var scaledForceVec = force.divide(mass).multiply(TIMESTEP);
        velocity = velocity.add(scaledForceVec).multiply(DAMPING);
        var scaledVelocity = velocity.multiply(TIMESTEP);
        position.setLocation(PointMath.movePoint(position, scaledVelocity));
    }

    /**
     * Clamp this component's position to within 1 radius of the boundaries given.
     * 
     * @param width
     * @param height
     */
    void clampPosition(int width, int height) {
        /*
         * Note that the position is the upper-left corner of the bounding rectangle,
         * not the center, which is why we multiply radius by 3 for the bounds.
         */
        setPosition(Math.clamp(position.getX(), radius, width - radius * 3.0),
                Math.clamp(position.getY(), radius, height - radius * 3.0));

    }

    void toggleLocked() {
        locked = !locked;
    }

    boolean isLocked() { return locked; }

    void setLocked(boolean locked) { this.locked = locked; }

    static int getTickLengthMillis() { return tickLengthMillis; }

    void setMass(double mass) { this.mass = Range.check("mass", mass, 0.1, 100.0); }
}
