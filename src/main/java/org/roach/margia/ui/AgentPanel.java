package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.roach.margia.Musician;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link JPanel}
 */
public class AgentPanel extends JPanel implements ActionListener, ComponentListener {
	private final Random rand = new SecureRandom();
	int numMusicians;
	private List<MusicianComponent> musicianComponents;
	private List<Edge> edges = new ArrayList<>();
	private final double K_REPULSION = 10000; // Repulsion constant
	private final double K_SPRING = 0.13; // Spring constant
	private final double DAMPING = 0.6; // Damping factor
	private final double TIMESTEP = 0.3; // Simulation speed/stability
	private Dimension panelSize;

	/**
	 * @param musicians        {@link Musician musicians} to display
	 * @param tickLengthMillis length of a tick in milliseconds
	 */
	public AgentPanel(List<Musician> musicians, int tickLengthMillis) {
		setLayout(null);
		addComponentListener(this);
		this.numMusicians = musicians.size();
		this.musicianComponents = new ArrayList<>();
		var size = 500;
		var dim = new Dimension(size, size);
		setSize(dim);
		setPreferredSize(dim);
		setDoubleBuffered(true);
		setBackground(Color.LIGHT_GRAY);
		for (var musician : musicians) {
			var n = new MusicianComponent(musician, tickLengthMillis, 1.0);
			n.px = rand.nextInt(size - MusicianComponent.circleRadius * 2);
			n.py = rand.nextInt(size - MusicianComponent.circleRadius * 2);
			n.updateLocation();
			musicianComponents.add(n);
			add(n);
		}

		calcEdges(musicians);
		// Use a Timer for animation
		new Timer(20, this).start();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updatePhysics();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		var g2d = (Graphics2D) g;
		var gradient = new GradientPaint(new Point2D.Float(getWidth() / 2, 0), getBackground(),
				new Point2D.Float(getWidth() / 2, getHeight()), Color.black);
		g2d.setPaint(gradient);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.setColor(Color.BLACK);
		for (Edge edge : edges) {
			g2d.drawLine((int) edge.source.px + edge.source.getWidth() / 2,
					(int) edge.source.py + edge.source.getHeight() / 2,
					(int) edge.target.px + edge.target.getWidth() / 2,
					(int) edge.target.py + edge.target.getHeight() / 2);
			drawArrow(g, (int) edge.source.px + edge.source.getWidth() / 2,
					(int) edge.source.py + edge.source.getHeight() / 2,
					(int) edge.target.px + edge.target.getWidth() / 2,
					(int) edge.target.py + edge.target.getHeight() / 2);
		}
	}

	private void calcEdges(List<Musician> musicians) {
		for (int i = 0; i < musicians.size(); i++) {
			var mus = musicians.get(i);
			var peerIds = mus.peerIds();
			for (var peerId : peerIds) {
				edges.add(new Edge(musicianComponents.get(i), musicianComponents.get(peerId), 60));
			}
		}
	}

	private void updatePhysics() {
		// 1. Reset forces
		for (var node : musicianComponents) {
			node.fx = 0;
			node.fy = 0;
		}

		// 2. Calculate repulsive forces between ALL pairs of nodes (O(N^2))
		for (int i = 0; i < numMusicians; i++) {
			for (int j = i + 1; j < numMusicians; j++) {
				var n1 = musicianComponents.get(i);
				var n2 = musicianComponents.get(j);
				double dx = n1.px - n2.px;
				double dy = n1.py - n2.py;
				double distance = Math.hypot(dx, dy);
				if (distance == 0)
					continue;

				// Repulsion force (inverse square law)
				double force = K_REPULSION / (distance * distance);
				n1.fx += (dx / distance) * force;
				n1.fy += (dy / distance) * force;
				n2.fx -= (dx / distance) * force;
				n2.fy -= (dy / distance) * force;
			}
		}

		// 3. Calculate attractive forces along edges (O(E))
		for (Edge edge : edges) {
			MusicianComponent n1 = edge.source;
			MusicianComponent n2 = edge.target;
			double dx = n1.px - n2.px;
			double dy = n1.py - n2.py;
			double distance = Math.hypot(dx, dy);
			if (distance == 0)
				continue;

			// Spring force (Hooke's law analog)
			double displacement = distance - edge.idealLength;
			double force = K_SPRING * displacement;

			n1.fx -= (dx / distance) * force;
			n1.fy -= (dy / distance) * force;
			n2.fx += (dx / distance) * force;
			n2.fy += (dy / distance) * force;
		}

		// 4. Update velocities and positions
		for (MusicianComponent node : musicianComponents) {
			node.vx = (node.vx + node.fx / node.mass * TIMESTEP) * DAMPING;
			node.vy = (node.vy + node.fy / node.mass * TIMESTEP) * DAMPING;
			node.px += node.vx * TIMESTEP;
			node.py += node.vy * TIMESTEP;

			// Simple boundary constraints (optional)
			node.px = Math.max(MusicianComponent.circleRadius,
					Math.min(getWidth() - MusicianComponent.circleRadius, node.px));
			node.py = Math.max(MusicianComponent.circleRadius,
					Math.min(getHeight() - MusicianComponent.circleRadius, node.py));

			node.updateLocation();
		}
	}

	private static final int ARR_SIZE = 4;

	void drawArrow(Graphics g1, int x1, int y1, int x2, int y2) {
		Graphics2D g = (Graphics2D) g1.create();

		double dx = x2 - x1, dy = y2 - y1;
		double angle = Math.atan2(dy, dx);
		int len = (int) Math.sqrt(dx * dx + dy * dy);
		AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
		at.concatenate(AffineTransform.getRotateInstance(angle));
		g.transform(at);

		// Draw horizontal arrow starting in (0, 0)
		g.drawLine(0, 0, len, 0);
		g.fillPolygon(new int[] { len, len - ARR_SIZE, len - ARR_SIZE, len }, new int[] { 0, -ARR_SIZE, ARR_SIZE, 0 },
				4);
	}

	static class Edge {
		MusicianComponent source, target;
		double idealLength; // the desired distance between nodes

		Edge(MusicianComponent source, MusicianComponent target, double idealLength) {
			this.source = source;
			this.target = target;
			this.idealLength = idealLength;
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		for (var musician : musicianComponents) {
			musician.px = rand.nextInt(getWidth());
			musician.py = rand.nextInt(getHeight());
			musician.updateLocation();
		}
		invalidate();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

}
