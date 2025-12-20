package org.roach.margia.ui;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * GUI element that displays an agent as a colored circle
 */
public class AgentComponent extends JComponent implements PropertyChangeListener {
	private final Musician agent;
	private Color color;
	private final int tickLengthMillis;
	private Timer timer;
	private volatile float brightness = 1f;
	private static final Stroke LINE_3PX = new BasicStroke(3);
	private static final Stroke LINE_1PX = new BasicStroke(1);

	/**
	 * @param agent            the {@link Musician} being displayed
	 * @param tickLengthMillis length of a tick in milliseconds
	 */
	public AgentComponent(Musician agent, int tickLengthMillis) {
		this.agent = agent;
		this.tickLengthMillis = tickLengthMillis;
		agent.addPropertyChangeListener(this);
		this.color = Color.black;
		this.setSize(40, 40);
		this.setPreferredSize(new Dimension(40, 40));
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
				var normalizedColor = (float) noteInfo.noteNum() / 127f;
				var normalizedSaturation = (float) noteInfo.velocity() / 127f;
				brightness = 1f;
				timer = new Timer(delay, _ -> {
					this.color = Color.getHSBColor(normalizedColor, normalizedSaturation, brightness);
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
		g2d.fillOval(0, 0, this.getWidth(), this.getHeight());
		if (agent.isMuted()) {
			g2d.setColor(Color.black);
			g2d.setStroke(LINE_3PX);
			g2d.drawLine(0, 0, getWidth(), getHeight());
			g2d.drawLine(0, getHeight(), getWidth(), 0);
		}
	}
}
