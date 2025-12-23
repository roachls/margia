package org.roach.margia.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * GUI element that displays an agent as a colored circle
 */
public class MusicianComponent extends JComponent implements PropertyChangeListener, ChangeListener {
	private static final float[] FRACTIONS = new float[] { 0.0f, 1.0f };
	private final Musician agent;
	private Color color;
	private int tickLengthMillis;
	private Timer timer;
	private volatile float brightness = 1f;
	/**
	 * size of circle to draw
	 */
	private int radius = MusicianComponent.DEFAULT_RADIUS;
	final double mass;
	double px, py;
	double vx, vy; // velocity
	double fx, fy; // total force
	/**
	 * default radius of musician components
	 */
	public static final int DEFAULT_RADIUS = 20;
	/**
	 * property name of radius spinner
	 */
	public static final String RADIUS_PROPERTY = "Radius";

	/**
	 * @param musician         the {@link Musician} being displayed
	 * @param tickLengthMillis length of a tick in milliseconds
	 * @param mass             mass to use in position calculations
	 */
	public MusicianComponent(Musician musician, int tickLengthMillis, double mass) {
		this.agent = musician;
		this.tickLengthMillis = tickLengthMillis;
		this.mass = mass;
		musician.addPropertyChangeListener(this);
		this.color = Color.black;
		setCircleRadius(20);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
		case Musician.LAST_NOTE_PROPERTY:
			NoteInfo noteInfo = (NoteInfo) evt.getNewValue();
			if (timer != null)
				timer.stop();
			if (noteInfo.noteNum() == -1) {
				this.color = Color.black;
				repaint();
			} else {
				// Hue depends on MIDI note played, relative to the full range of the musician
				var normalizedHue = agent.noteToRange(noteInfo.noteNum());
				// Saturation depends on velocity of MIDI note
				var normalizedSaturation = (float) noteInfo.velocity() / 127f;
				// Start the brightness at full (1.0) and decrease it to 0 over the life of the
				// note
				var delay = 1000 / (tickLengthMillis * noteInfo.length());
				brightness = 1f;
				timer = new Timer(delay, _ -> {
					color = Color.getHSBColor(normalizedHue, normalizedSaturation, brightness);
					brightness = Math.max(0f, brightness - 0.004f);
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
		var g2d = (Graphics2D) g;
		// Enable anti-aliasing for shapes/lines
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Enable anti-aliasing for text (use VALUE_TEXT_ANTIALIAS_GASP for potentially
		// better results)
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

		g2d.setColor(color);
		var paint = new RadialGradientPaint(new Point2D.Float(radius + 3, radius + 3), radius, FRACTIONS,
				new Color[] { Color.white, color });
		g2d.setPaint(paint);
		g2d.fillOval(0, 0, getWidth(), getHeight());
		g2d.setColor(Color.LIGHT_GRAY);
		g2d.drawString(Integer.toString(agent.getId()), (int) radius - 6, (int) radius - 3);
		if (agent.isMuted()) {
			g2d.setColor(new Color(0.5f, 0.5f, 0.5f, 0.8f));
			g2d.fillOval(0, 0, getWidth(), getHeight());
		}
	}

	void updateLocation() {
		setLocation((int) px, (int) py);
	}

	/**
	 * @return radius of displayed circle
	 */
	public int getRadius() {
		return radius;
	}

	/**
	 * @return diameter of displayed circle
	 */
	public int getDiameter() {
		return radius * 2;
	}

	/**
	 * @param circleRadius radius of displayed circle
	 */
	public void setCircleRadius(int circleRadius) {
		this.radius = Math.max(0, circleRadius);
		var dim = new Dimension(circleRadius * 2, circleRadius * 2);
		setPreferredSize(dim);
		setMinimumSize(dim);
		setSize(dim);
		setMaximumSize(dim);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JSpinner spinner && spinner.getName().equals(MusicianComponent.RADIUS_PROPERTY)) {
			this.setCircleRadius((int) spinner.getValue());
		}
	}

	/**
	 * @param tickLengthMillis tick length in milliseconds, used for animations
	 */
	public void setTickLengthMillis(int tickLengthMillis) {
		this.tickLengthMillis = tickLengthMillis;
	}
}
