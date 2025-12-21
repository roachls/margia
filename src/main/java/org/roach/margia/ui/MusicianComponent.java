package org.roach.margia.ui;

import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * GUI element that displays an agent as a colored circle
 */
public class MusicianComponent extends JComponent implements PropertyChangeListener {
	private static final float[] FRACTIONS = new float[] { 0.0f, 1.0f };
	private final Musician agent;
	private Color color;
	private final int tickLengthMillis;
	private Timer timer;
	private volatile float brightness = 1f;
	private static final Stroke LINE_1PX = new BasicStroke(1);
	private static int circleRadius = 20;
	private static int circleDiameter = circleRadius * 2;

	/**
	 * @param musician            the {@link Musician} being displayed
	 * @param tickLengthMillis length of a tick in milliseconds
	 */
	public MusicianComponent(Musician musician, int tickLengthMillis) {
		this.agent = musician;
		this.tickLengthMillis = tickLengthMillis;
		musician.addPropertyChangeListener(this);
		this.color = Color.black;
		var dim = new Dimension(circleRadius * 2, circleRadius * 2);
		setPreferredSize(dim);
		setMinimumSize(dim);
		setSize(dim);
		setMaximumSize(dim);
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
				var delay = 1000 / (tickLengthMillis * noteInfo.length());
				var normalizedColor = agent.noteToRange(noteInfo.noteNum());
				var normalizedSaturation = (float) noteInfo.velocity() / 127f;
				brightness = 1f;
				timer = new Timer(delay, _ -> {
					color = Color.getHSBColor(normalizedColor, normalizedSaturation, brightness);
					brightness -= 0.004f;
					if (brightness < 0.0f)
						brightness = 0.0f;
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
		g2d.setColor(color);
		g2d.setStroke(LINE_1PX);
		var centerX = getWidth() / 2;
		var centerY = getHeight() / 2;
		var topLeftX = centerX - circleRadius;
		var topLeftY = centerY - circleRadius;
		var paint = new RadialGradientPaint(new Point2D.Float(centerX + 3, centerY + 3), circleRadius, FRACTIONS,
				new Color[] { Color.white, color });
		g2d.setPaint(paint);
		g2d.fillOval(topLeftX, topLeftY, circleDiameter, circleDiameter);
		if (agent.isMuted()) {
			g2d.setColor(Color.black);
			g2d.drawOval(topLeftX, topLeftY, circleDiameter, circleDiameter);
		}
	}
}
