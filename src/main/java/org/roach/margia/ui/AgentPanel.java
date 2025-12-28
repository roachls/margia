package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.Options;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link JPanel}
 */
@SuppressWarnings({ "java:S1948" })
public class AgentPanel extends JPanel implements ActionListener, ChangeListener {
    int numMusicians;
    private List<MusicianComponent> musicianComponents;
    private List<Edge> edges = new ArrayList<>();
    private static final double K_REPULSION = 10000; // Repulsion constant
    private static final double K_SPRING = 0.13; // Spring constant
    private static final double DAMPING = 0.6; // Damping factor
    private static final double TIMESTEP = 0.8; // Simulation speed/stability
    private double gravity;
    private final List<Musician> musicians;
    private int tickLengthMillis;
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
    public static final String EDGE_LENGTH_PROPERTY = "Edge_length";
    /**
     * property name of gravity spinner
     */
    public static final String GRAVITY_PROPERTY = "Gravitational_Constant";

    /**
     * @param musicians {@link Musician musicians} to display
     */
    public AgentPanel(List<Musician> musicians) {
        this.musicians = musicians;
        var defTempo = Options.getInstance().getOrDefaultAsInt(TimingSource.TEMPO_PROPERTY, TimingSource.DEFAULT_TEMPO);
        this.tickLengthMillis = 60000 / (defTempo * 24);
        this.gravity = Options.getInstance().getOrDefaultAsDouble(GRAVITY_PROPERTY, DEFAULT_GRAVITATIONAL_CONSTANT);
        setLayout(null);
        this.numMusicians = musicians.size();
        this.musicianComponents = new ArrayList<>();
        Options.getInstance().addChangeListener(GRAVITY_PROPERTY, this);
        Options.getInstance().addChangeListener(EDGE_LENGTH_PROPERTY, this);
        setDoubleBuffered(true);
        setBackground(Color.LIGHT_GRAY);
    }

    void initMusicians() {
        var gridSize = Math.ceil(Math.sqrt(musicians.size()));
        var cellSizeX = getWidth() / gridSize;
        var cellSizeY = getHeight() / gridSize;
        var musicianIter = musicians.iterator();
        for (int x = 1; x <= gridSize; x++) {
            for (int y = 1; y <= gridSize && musicianIter.hasNext(); y++) {
                var musician = musicianIter.next();
                var n = new MusicianComponent(musician, tickLengthMillis, 1.0);
                Options.getInstance().addChangeListener(MusicianComponent.RADIUS_PROPERTY, n);
                n.px = x * cellSizeX - cellSizeX / 2;
                n.py = y * cellSizeY - cellSizeY / 2;
                n.updateLocation();
                musicianComponents.add(n);
                add(n);
            }
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

        var gradient = new GradientPaint(new Point2D.Float(getWidth() / 2f, 0), getBackground(),
                new Point2D.Float(getWidth() / 2f, getHeight()), Color.black);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        var transform = new AffineTransform();
        for (Edge edge : edges) {
            transform.setToIdentity();
            var g2d2 = (Graphics2D) g2d.create();
            g2d.setColor(edge.source.getColor());
            // center of source component
            var scx = edge.source.px + edge.source.getWidth() / 2d;
            var scy = edge.source.py + edge.source.getHeight() / 2d;
            // center of target component
            var tcx = edge.target.px + edge.target.getWidth() / 2d;
            var tcy = edge.target.py + edge.target.getHeight() / 2d;
            // calculate distance from source to target
            var dx = tcx - scx;
            var dy = tcy - scy;
            var angle = Math.atan2(dy, dx);
            var dist = Math.hypot(dx, dy) - edge.target.getRadius();
            var line = new Line2D.Double(0, 0, 0, dist);
            var path = new Path2D.Double();
            path.moveTo(0, dist - 5);
            path.lineTo(3, dist - 5);
            path.lineTo(0, dist);
            path.lineTo(-3, dist - 5);
            path.closePath();
            transform.translate(scx, scy);
            transform.rotate(angle - Math.PI / 2d);
            g2d2.transform(transform);
            g2d2.setColor(edge.source.getColor());
            g2d2.draw(line);
            g2d2.fill(path);
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
            node.px = Math.clamp(node.px, node.getDiameter(), (double) getWidth() - node.getDiameter());
            node.py = Math.clamp(node.py, node.getDiameter(), (double) getHeight() - node.getDiameter());

            node.updateLocation();
        }
    }

    static class Edge {
        private final MusicianComponent source;
        private MusicianComponent target;
        double idealLength; // the desired distance between nodes

        Edge(MusicianComponent source, MusicianComponent target, double idealLength) {
            this.source = source;
            this.target = target;
            this.idealLength = idealLength;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource cs) {
            switch (cs.key()) {
            case AgentPanel.GRAVITY_PROPERTY:
                this.gravity = Double.parseDouble(cs.newValue());
                break;
            case AgentPanel.EDGE_LENGTH_PROPERTY:
                var len = Integer.parseInt(cs.newValue());
                edges.forEach(edge -> edge.idealLength = len);
                break;
            case TimingSource.TEMPO_PROPERTY:
                this.tickLengthMillis = 60000 / (Integer.parseInt(cs.newValue()) * 24);
                musicianComponents.forEach(m -> m.setTickLengthMillis(tickLengthMillis));
                break;
            default:
                break;
            }
        }
    }

}
