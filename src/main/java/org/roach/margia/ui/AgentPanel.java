package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class AgentPanel extends JPanel implements ActionListener {
	private final Random rand = new SecureRandom();
	int numMusicians;
	private List<MusicianComponent> musicianComponents;
	private List<Edge> edges = new ArrayList<>();
	private final double K_REPULSION = 10000; // Repulsion constant
	private final double K_SPRING = 0.13; // Spring constant
	private final double DAMPING = 0.9; // Damping factor
	private final double TIMESTEP = 0.3; // Simulation speed/stability

	/**
	 * @param musicians        {@link Musician musicians} to display
	 * @param tickLengthMillis length of a tick in milliseconds
	 */
	public AgentPanel(List<Musician> musicians, int tickLengthMillis) {
		this.numMusicians = musicians.size();
		this.musicianComponents = new ArrayList<>();
		var size = 500;
		var dim = new Dimension(size, size);
		setSize(dim);
		setPreferredSize(dim);
		setDoubleBuffered(true);
		for (var musician : musicians) {
			var n = new MusicianComponent(musician, tickLengthMillis, 1.0);
			n.px = rand.nextInt(size);
			n.py = rand.nextInt(size);
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
		g.setColor(Color.BLACK);
		for (Edge edge : edges) {
			g2d.drawLine((int) edge.source.px + edge.source.getWidth() / 2,
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
				edges.add(new Edge(musicianComponents.get(i), musicianComponents.get(peerId), 100));
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
			node.px = Math.max(10, Math.min(getWidth() - 10, node.px));
			node.py = Math.max(10, Math.min(getHeight() - 10, node.py));

			node.updateLocation();
		}
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

}
