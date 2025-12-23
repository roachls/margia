package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.timing.TimingSource;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link JPanel}
 */
public class AgentPanel extends JPanel implements ActionListener, ChangeListener {
	private final Random rand = new SecureRandom();
	int numMusicians;
	private List<MusicianComponent> musicianComponents;
	private List<Edge> edges = new ArrayList<>();
	private final double K_REPULSION = 10000; // Repulsion constant
	private final double K_SPRING = 0.13; // Spring constant
	private final double DAMPING = 0.6; // Damping factor
	private final double TIMESTEP = 0.8; // Simulation speed/stability
	private double gravity = AgentPanel.DEFAULT_GRAVITATIONAL_CONSTANT;
	private final List<Musician> musicians;
	private int tickLengthMillis;
	private final OptionPanel optionPanel;
	/**
	 * default edge length
	 */
	public static final int DEFAULT_EDGE_LENGTH = 60;
	/**
	 * default gravitational constant
	 */
	public static final double DEFAULT_GRAVITATIONAL_CONSTANT = 5.0;
	/**
	 * property name of edge length spinner
	 */
	public static final String EDGE_LENGTH_PROPERTY = "Edge length";
	/**
	 * property name of gravity spinner
	 */
	public static final String GRAVITY_PROPERTY = "Gravitational Constant";

	/**
	 * @param musicians   {@link Musician musicians} to display
	 * @param optionPanel the option panel
	 */
	public AgentPanel(List<Musician> musicians, OptionPanel optionPanel) {
		this.musicians = musicians;
		this.tickLengthMillis = 60000 / (TimingSource.DEFAULT_TEMPO * 24);
		this.optionPanel = optionPanel;
		setLayout(null);
		this.numMusicians = musicians.size();
		this.musicianComponents = new ArrayList<>();
		optionPanel.addGravityListener(this);
		optionPanel.addEdgeLengthListener(this);
		setDoubleBuffered(true);
		setBackground(Color.LIGHT_GRAY);
	}

	void initMusicians() {
		for (var musician : musicians) {
			var n = new MusicianComponent(musician, tickLengthMillis, 1.0);
			optionPanel.addRadiusListener(n);
			n.px = rand.nextInt(getWidth() - n.getDiameter());
			n.py = rand.nextInt(getHeight() - n.getDiameter());
			n.updateLocation();
			musicianComponents.add(n);
			add(n);
		}

		calcEdges();

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
		// Enable anti-aliasing for shapes/lines
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
//			drawArrow(g, (int) edge.source.px + edge.source.getWidth() / 2,
//					(int) edge.source.py + edge.source.getHeight() / 2,
//					(int) edge.target.px + edge.target.getWidth() / 2,
//					(int) edge.target.py + edge.target.getHeight() / 2);
		}
	}

	private void calcEdges() {
		for (int i = 0; i < musicians.size(); i++) {
			var mus = musicians.get(i);
			var peerIds = mus.peerIds();
			for (var peerId : peerIds) {
				edges.add(new Edge(musicianComponents.get(i), musicianComponents.get(peerId),
						AgentPanel.DEFAULT_EDGE_LENGTH));
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

		// 4. pull all items towards center in inverse square relationship
		var centerX = getWidth() / 2;
		var centerY = getHeight() / 2;
		for (int i = 0; i < numMusicians; i++) {
			var n1 = musicianComponents.get(i);
			var distX = n1.px - centerX;
			var distY = n1.py - centerY;
			var dist = Math.hypot(distX, distY) + 50;

			var force = gravity / dist;
			n1.fx -= distX * force;
			n1.fy -= distY * force;
		}
		// 5. Update velocities and positions
		for (MusicianComponent node : musicianComponents) {
			node.vx = (node.vx + node.fx / node.mass * TIMESTEP) * DAMPING;
			node.vy = (node.vy + node.fy / node.mass * TIMESTEP) * DAMPING;
			node.px += node.vx * TIMESTEP;
			node.py += node.vy * TIMESTEP;

			// Simple boundary constraints (optional)
			node.px = Math.max(node.getDiameter(), Math.min(getWidth() - node.getDiameter(), node.px));
			node.py = Math.max(node.getDiameter(), Math.min(getHeight() - node.getDiameter(), node.py));

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
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JSpinner spinner) {
			if (spinner.getName().equals(AgentPanel.GRAVITY_PROPERTY)) {
				this.gravity = (double) spinner.getValue();
			} else if (spinner.getName().equals(AgentPanel.EDGE_LENGTH_PROPERTY)) {
				for (var edge : edges) {
					edge.idealLength = (int) spinner.getValue();
				}
			} else if (spinner.getName().equals("tempo")) {
				this.tickLengthMillis = 60000 / ((int) spinner.getValue() * 24);
				for (var musician : musicianComponents) {
					musician.setTickLengthMillis(tickLengthMillis);
				}
			}
		}
	}

}
