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

/**
 * GUI element that displays an agent as a colored circle
 */
@SuppressWarnings({ "java:S1948" })
public class MusicianComponent extends JComponent implements PropertyChangeListener, ChangeListener {
    private static final float[] FRACTIONS = new float[] { 0.0f, 1.0f };
    private final Musician musician;
    private Color color;
    private int tickLengthMillis;
    private Timer timer;
    private boolean selected;

    /**
     * size of circle to draw
     */
    private int radius = MusicianComponent.DEFAULT_RADIUS;
    private final double mass;
    private final Point2D.Double position = new Point2D.Double();
    double vx;
    double vy; // velocity
    double fx;
    double fy; // total force
    /**
     * default radius of musician components
     */
    static final int DEFAULT_RADIUS = 20;
    /**
     * property name of radius spinner
     */
    static final String RADIUS_PROPERTY = "Radius";
    /**
     * property name of whether to show numbers
     */
    static final String SHOW_NUMBERS_PROPERTY = "Show_numbers";
    private static boolean showNumbers = Options.getInstance().getOrDefaultAsBoolean(SHOW_NUMBERS_PROPERTY, true);

    /**
     * listener for the show numbers property
     */
    static final ChangeListener SHOW_NUMBERS_LISTENER = e -> showNumbers = Boolean
            .parseBoolean(((ChangeSource) e.getSource()).newValue());

    static final Stroke SELECTED_STROKE = new BasicStroke(2.0f);

    /**
     * @param musician         the {@link Musician} being displayed
     * @param tickLengthMillis length of a tick in milliseconds
     * @param mass             mass to use in position calculations
     */
    MusicianComponent(Musician musician, int tickLengthMillis, double mass) {
        this.musician = musician;
        this.setName("Musician_" + musician.getId());
        this.tickLengthMillis = tickLengthMillis;
        this.mass = mass;
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
            this.setCircleRadius(Integer.parseInt(cs.newValue()));
        }
    }

    /**
     * @param tickLengthMillis tick length in milliseconds, used for animations
     */
    public void setTickLengthMillis(int tickLengthMillis) { this.tickLengthMillis = tickLengthMillis; }

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
                + ", timer=" + timer + ", radius=" + radius + ", mass=" + mass + ", position=" + position + ", vx=" + vx
                + ", vy=" + vy + ", fx=" + fx + ", fy=" + fy + "]";
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
        position.setLocation(x, y);
        updateLocation();
    }

    Point2D.Double getCenter() {
        return new Point2D.Double(position.x + getWidth() / 2.0, position.y + getHeight() / 2.0);
    }
}
