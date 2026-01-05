package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.beans.PropertyVetoException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.Options;
import org.roach.margia.rules.RandomRule;
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
    private double gravity;
    private final List<Musician> musicians;
    private int tickLengthMillis;
    private Point startSelection;
    private Point endSelection;
    private boolean isConnecting;
    private EditMode mode = EditMode.SELECT;
    static final String SELECTED_AGENT_PROPERTY = "selected_agent";
    private MusicianComponent selectedAgent;
    private int width;
    private int height;

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
    public static final String EDGE_LENGTH_PROPERTY = "ui.edge_length";
    /**
     * property name of gravity spinner
     */
    public static final String GRAVITY_PROPERTY = "ui.gravitational_Constant";

    private static final Stroke SELECTION_LINE_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, new float[] { 10.0f, 10.0f }, 0.0f);

    private static final float[] GRAD_FRACTIONS = new float[] { 0f, 0.75f, 1f };
    private static final Color[] GRAD_COLORS = new Color[] { Color.white, Color.black, Color.white };

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
        this.addMouseListener(mouseAdapter);
        this.addMouseMotionListener(mouseAdapter);
        addComponentListener(new Resizer());
    }

    private class Resizer extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            // width and height are from before the resize
            var newWidth = getWidth();
            var newHeight = getHeight();
            var xRatio = (double) newWidth / (double) width;
            var yRatio = (double) newHeight / (double) height;
            musicianComponents.forEach(mc -> {
                var locked = mc.isLocked();
                mc.setLocked(false);
                mc.setPosition(mc.getPosition().getX() * xRatio, mc.getPosition().getY() * yRatio);
                mc.setVelocity(mc.getVelocity().x() * xRatio, mc.getVelocity().y() * yRatio);
                // restore to previous locked status
                mc.setLocked(locked);
            });
            width = newWidth;
            height = newHeight;
        }
    }

    void initMusicians() {
        // store width and height in case we resize later
        width = getWidth();
        height = getHeight();
        var rand = new SecureRandom();
        for (var musician : musicians) {
            var n = new MusicianComponent(musician, tickLengthMillis, 1.0);
            Options.getInstance().addChangeListener(MusicianComponent.RADIUS_PROPERTY, n);
            n.setPosition(rand.nextInt(n.getDiameter(), getWidth() - n.getDiameter()),
                    rand.nextInt(n.getDiameter(), getHeight() - n.getDiameter()));
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

        var gradient = new RadialGradientPaint(getWidth() / 2f, getHeight() / 2f, getWidth(), GRAD_FRACTIONS,
                GRAD_COLORS);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        var transform = new AffineTransform();
        for (Edge edge : edges) {
            transform.setToIdentity();
            var g2d2 = (Graphics2D) g2d.create();
            g2d.setColor(edge.source.getColor());
            // center of source component
            var sCenter = edge.source.getCenter();
            // center of target component
            var tCenter = edge.target.getCenter();
            // calculate distance from source to target
            var vector = new Vector2D(sCenter, tCenter);
            var angle = vector.angle();
            var dist = sCenter.distance(tCenter) - edge.target.getRadius();
            var line = new Line2D.Double(0, 0, 0, dist);
            var path = new Path2D.Double();
            path.moveTo(0, dist - 5);
            path.lineTo(3, dist - 5);
            path.lineTo(0, dist);
            path.lineTo(-3, dist - 5);
            path.closePath();
            transform.translate(sCenter.x, sCenter.y);
            transform.rotate(angle - Math.PI / 2d);
            g2d2.transform(transform);
            g2d2.setColor(edge.source.getColor());
            g2d2.draw(line);
            g2d2.fill(path);
        }

        if (startSelection != null && endSelection != null) {
            g2d.setColor(Color.red);
            g2d.setStroke(SELECTION_LINE_STROKE);
            g2d.drawLine((int) startSelection.getX(), (int) startSelection.getY(), (int) endSelection.getX(),
                    (int) endSelection.getY());
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
        musicianComponents.forEach(MusicianComponent::resetForce);

        // 2. Calculate repulsive forces between ALL pairs of nodes (O(N^2))
        for (int i = 0; i < numMusicians; i++) {
            for (int j = i + 1; j < numMusicians; j++) {
                var n1 = musicianComponents.get(i);
                var n2 = musicianComponents.get(j);
                var unitVec = PointMath.unitVector(n2.getPosition(), n1.getPosition());
                double distance = n1.getPosition().distance(n2.getPosition());
                if (distance == 0) // prevent division by 0
                    continue;

                // Repulsion force (inverse square law)
                double force = K_REPULSION / (distance * distance);
                var forceVec = unitVec.multiply(force);
                n1.setForce(n1.getForce().add(forceVec));
                n2.setForce(n2.getForce().subtract(forceVec));
            }
        }

        // 3. Calculate attractive forces along edges (O(E))
        for (Edge edge : edges) {
            MusicianComponent n1 = edge.source;
            MusicianComponent n2 = edge.target;
            var unitVec = PointMath.unitVector(n2.getPosition(), n1.getPosition());
            double distance = n1.getPosition().distance(n2.getPosition());

            // Spring force (Hooke's law analog)
            double displacement = distance - edge.idealLength;
            double force = K_SPRING * displacement;
            var forceVec = unitVec.multiply(force);

            n1.setForce(n1.getForce().subtract(forceVec));
            n2.setForce(n2.getForce().add(forceVec));
        }

        // 4. pull all items towards center in inverse square relationship
        var center = new Point2D.Double(getWidth() / 2d, getHeight() / 2d);
        for (int i = 0; i < numMusicians; i++) {
            var n1 = musicianComponents.get(i);
            var vec = new Vector2D(center, n1.getPosition());
            var dist = n1.getPosition().distance(center);
            if (dist == 0.0) // prevent division by zero
                continue;

            var force = gravity / dist;
            var forceVec = vec.multiply(force);
            n1.setForce(n1.getForce().subtract(forceVec));
        }
        // 5. Update velocities and positions
        for (MusicianComponent node : musicianComponents) {
            node.applyForces();
            node.clampPosition(getWidth(), getHeight());
        }
    }

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        private MusicianComponent source;
        private MusicianComponent movingComponent;

        @Override
        @SuppressWarnings("java:S1301")
        public void mousePressed(MouseEvent e) {
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                leftMouseButtonPressed(e);
                break;
            default:
                break;
            }
        }

        private void leftMouseButtonPressed(MouseEvent e) {
            var comp = getComponentAt(e.getPoint());
            if (comp instanceof MusicianComponent mc) {
                switch (mode) {
                case CONNECT:
                    if (source != null) {
                        source.setSelected(false);
                    }
                    source = mc;
                    mc.setSelected(true);
                    isConnecting = true;
                    startSelection = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    break;
                case MOVE:
                    mc.setSelected(true);
                    movingComponent = mc;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    break;
                default:
                    break;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                leftMouseButtonReleased(e);
            }
        }

        private void leftMouseButtonReleased(MouseEvent e) {
            var comp = getComponentAt(e.getPoint());
            switch (mode) {
            case ADD:
                if (comp instanceof AgentPanel)
                    handleAdd(e);
                break;
            case DELETE:
                if (comp instanceof MusicianComponent mc)
                    handleRemove(mc);
                break;
            case LOCK:
                if (comp instanceof MusicianComponent mc) {
                    mc.toggleLocked();
                }
                break;
            case MUTE:
                if (comp instanceof MusicianComponent mc) {
                    mc.toggleMuted();
                }
                break;
            case SELECT:
                handleMusicianSelection(comp);
                break;
            case CONNECT:
                handleMusicianConnection(comp);
                break;
            case MOVE:
                handleMusicianMove(e);
                break;
            default:
                break;

            }
        }

        private void handleMusicianMove(MouseEvent e) {
            if (movingComponent != null) {
                movingComponent.setLocked(false);
                movingComponent.setPosition(e.getX(), e.getY());
                movingComponent.setLocked(true);
                movingComponent.setSelected(false);
                setCursor(Cursor.getDefaultCursor());
            }
            movingComponent = null;
        }

        private void handleMusicianConnection(Component comp) {
            if (isConnecting && comp instanceof MusicianComponent target && !target.equals(source)) {
                isConnecting = false;
                // check if already connected, and if so, disconnect; otherwise, connect
                edges.stream().filter(edge -> source.equals(edge.source)).filter(edge -> target.equals(edge.target))
                        .findAny().ifPresentOrElse(connection -> {
                            connection.source.getMusician().removePeer(connection.target.getMusician());
                            edges.remove(connection);
                        }, () -> {
                            source.getMusician().addPeer(target.getMusician());
                            edges.add(new Edge(source, target, DEFAULT_EDGE_LENGTH));
                        });
                source.setSelected(false);
                this.source = null;
                startSelection = null;
                endSelection = null;
                AgentPanel.this.setCursor(Cursor.getDefaultCursor());
            } else if (comp == AgentPanel.this) {
                if (source != null)
                    source.setSelected(false);
                this.source = null;
                startSelection = null;
                endSelection = null;
                AgentPanel.this.setCursor(Cursor.getDefaultCursor());
            }
        }

        private void handleMusicianSelection(Component comp) {
            try {
                if (comp instanceof MusicianComponent mc) {
                    fireVetoableChange(SELECTED_AGENT_PROPERTY, selectedAgent, mc);
                    selectedAgent = mc;
                } else {
                    fireVetoableChange(SELECTED_AGENT_PROPERTY, selectedAgent, null);
                    selectedAgent = null;
                }
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        }

        private void handleRemove(MusicianComponent mc) {
            musicians.remove(mc.getMusician());
            numMusicians = musicians.size();
            musicianComponents.remove(mc);
            var edgeIter = edges.iterator();
            while (edgeIter.hasNext()) {
                var edge = edgeIter.next();
                if (edge.source.equals(mc) || edge.target.equals(mc))
                    edgeIter.remove();
            }
            remove(mc);
        }

        private void handleAdd(MouseEvent e) {
            var newId = musicians.stream().map(Musician::getId).max(Integer::compare).orElse(-1) + 1;
            var musician = new Musician(newId, 0, new RandomRule());
            musicians.add(musician);
            numMusicians = musicians.size();
            var musicianComponent = new MusicianComponent(musician, tickLengthMillis, 1.0);
            musicianComponents.add(musicianComponent);
            Options.getInstance().addChangeListener(MusicianComponent.RADIUS_PROPERTY, musicianComponent);
            musicianComponent.setPosition(e.getX(), e.getY());
            add(musicianComponent);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = e.getPoint();
            } else if (mode == EditMode.MOVE && movingComponent != null) {
                movingComponent.setLocked(false);
                movingComponent.setPosition(e.getX(), e.getY());
                movingComponent.setLocked(true);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = e.getPoint();
            }
        }

    };

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
                this.gravity = (double) cs.newValue();
                break;
            case AgentPanel.EDGE_LENGTH_PROPERTY:
                var len = (int) cs.newValue();
                edges.forEach(edge -> edge.idealLength = len);
                break;
            case TimingSource.TEMPO_PROPERTY:
                this.tickLengthMillis = 60000 / ((int) cs.newValue() * 24);
                musicianComponents.forEach(m -> m.setTickLengthMillis(tickLengthMillis));
                break;
            default:
                break;
            }
        }
    }

    void setEditMode(EditMode mode) { this.mode = mode; }

    enum EditMode {
        ADD, DELETE, MUTE, SELECT, LOCK, CONNECT, MOVE;
    }

    void unlockAll() {
        musicianComponents.forEach(mc -> mc.setLocked(false));
    }

    void lockAll() {
        musicianComponents.forEach(mc -> mc.setLocked(true));
    }

    void muteAll() {
        musicianComponents.forEach(mc -> mc.getMusician().setMuted(true));
    }

    void unmuteAll() {
        musicianComponents.forEach(mc -> mc.getMusician().setMuted(false));
    }
}
