package org.roach.margia.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.beans.PropertyVetoException;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.Musician;
import org.roach.margia.MusicianList;
import org.roach.margia.rules.*;
import org.roach.margia.storage.Options;
import org.roach.margia.timing.TimingSource;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link JPanel}
 */
@SuppressWarnings({ "java:S1948" })
public class AgentPanel extends JPanel implements ActionListener, ChangeListener {
    int numMusicians;
    private final Map<Integer, MusicianComponent> musicianComponents = new TreeMap<>();
    private List<Edge> edges = new ArrayList<>();
    private static final double K_REPULSION = 10000; // Repulsion constant
    private static final double K_SPRING = 0.13; // Spring constant
    private int tickLengthMillis;
    private Point startSelection;
    private Point endSelection;
    private boolean isConnecting;
    private EditMode mode = EditMode.SELECT;
    static final String SELECTED_AGENT_PROPERTY = "selected_agent";
    private MusicianComponent selectedAgent;
    private int oldWidth;
    private int oldHeight;
    private Point dragStart;
    private Point dragEnd;
    private List<MusicianComponent> copiedComponents;
    private static final Random RANDOM = new SecureRandom();

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

    private static final Stroke SELECTION_LINE_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, new float[] { 10.0f, 10.0f }, 0.0f);

    private static final float[] GRAD_FRACTIONS = new float[] { 0f, 0.75f, 1f };
    private static final Color[] GRAD_COLORS = new Color[] { Color.white, Color.black, Color.white };

    /**
     * constructor
     */
    public AgentPanel() {
        setLayout(null);
        Options.getInstance().addChangeListener(EDGE_LENGTH_PROPERTY, this);
        setDoubleBuffered(true);
        setBackground(Color.LIGHT_GRAY);
        this.addMouseListener(mouseAdapter);
        this.addMouseMotionListener(mouseAdapter);
        addComponentListener(new Resizer());
    }

    void init() {
        var defTempo = Options.getInstance().getMusicOptions().getTempo();
        this.tickLengthMillis = 60000 / (defTempo * 24);
        this.numMusicians = MusicianList.getInstance().numMusicians();
        this.musicianComponents.clear();
    }

    private class Resizer extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            // width and height are from before the resize
            var newWidth = getWidth();
            var newHeight = getHeight();
            var xRatio = (double) newWidth / (double) oldWidth;
            var yRatio = (double) newHeight / (double) oldHeight;
            musicianComponents.values().forEach(mc -> {
                var locked = mc.isLocked();
                mc.setLocked(false);
                mc.setPosition(mc.getPosition().getX() * xRatio, mc.getPosition().getY() * yRatio);
                mc.setVelocity(mc.getVelocity().x() * xRatio, mc.getVelocity().y() * yRatio);
                // restore to previous locked status
                mc.setLocked(locked);
            });
            oldWidth = newWidth;
            oldHeight = newHeight;
        }
    }

    void initMusicians() {
        // store width and height in case we resize later
        oldWidth = getWidth();
        oldHeight = getHeight();
        for (var musician : MusicianList.getInstance().getMusicians().values()) {
            addMusicianComponent(musician);
        }

        calcEdges();

        // Use a Timer for animation
        new Timer(20, this).start();
    }

    private MusicianComponent addMusicianComponent(Musician musician) {
        var n = new MusicianComponent(musician);
        Options.getInstance().addChangeListener(MusicianComponent.RADIUS_PROPERTY, n);
        n.setPosition(RANDOM.nextInt(n.getDiameter(), getWidth() - n.getDiameter()),
                RANDOM.nextInt(n.getDiameter(), getHeight() - n.getDiameter()));
        musicianComponents.put(musician.getId(), n);
        add(n);
        numMusicians = musicianComponents.size();
        return n;
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

        if (dragStart != null && dragEnd != null) {
            g2d.setColor(Color.yellow);
            g2d.setStroke(SELECTION_LINE_STROKE);
            var rect = makeSelectionRectangle(dragStart, dragEnd);
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    private void calcEdges() {
        for (var musEntry : MusicianList.getInstance().getMusicians().entrySet()) {
            var id = musEntry.getKey();
            var mus = musEntry.getValue();
            var peerIds = mus.peerIds();
            for (var peerId : peerIds) {
                edges.add(new Edge(musicianComponents.get(id), musicianComponents.get(peerId),
                        AgentPanel.DEFAULT_EDGE_LENGTH));
            }
        }
    }

    private void updatePhysics() {
        // 1. Reset forces
        musicianComponents.values().forEach(MusicianComponent::resetForce);

        // 2. Calculate repulsive forces between ALL pairs of nodes (O(N^2))
        for (var n1 : musicianComponents.values()) {
            for (var n2 : musicianComponents.values()) {
                if (n1.equals(n2))
                    continue;
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
        for (var n1 : musicianComponents.values()) {
            var vec = new Vector2D(center, n1.getPosition());
            var dist = n1.getPosition().distance(center);
            if (dist == 0.0) // prevent division by zero
                continue;

            var gravity = Options.getInstance().getUiOptions().getGravity();
            var force = n1.getMass() * gravity / dist;
            var forceVec = vec.multiply(force);
            n1.setForce(n1.getForce().subtract(forceVec));
        }
        // 5. Update velocities and positions
        for (MusicianComponent node : musicianComponents.values()) {
            node.applyForces();
            node.clampPosition(getWidth(), getHeight());
        }
    }

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        private MusicianComponent source;
        private MusicianComponent movingComponent;
        // position of cursor in movingComponent space
        private Point movingComponentXY;
        private static final int BUTTON1 = InputEvent.BUTTON1_DOWN_MASK;
        private static final int SHIFT_BUTTON1 = InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                leftMouseButtonPressed(e);
            }
        }

        private void leftMouseButtonPressed(MouseEvent e) {
            if (e.getModifiersEx() == BUTTON1) {
                leftMousePressedSingleSelectionMode(e);
            } else if (e.getModifiersEx() == SHIFT_BUTTON1) {
                deselectAllMusicians();
                dragStart = e.getPoint();
            }
        }

        private void leftMousePressedSingleSelectionMode(MouseEvent e) {
            var comp = getComponentAt(e.getPoint());
            if (comp instanceof MusicianComponent mc) {
                mc.setSelected(false);
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
                    movingComponentXY = SwingUtilities.convertPoint(AgentPanel.this, e.getPoint(), mc);
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    break;
                default:
                    break;
                }
            } else {
                deselectAllMusicians();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                leftMouseButtonReleased(e);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                var editingComponent = getComponentAt(e.getPoint());
                handleMusicianSelectedForEditing(editingComponent);
            }
        }

        private void leftMouseButtonReleased(MouseEvent e) {
            if (!e.isShiftDown()) {
                // no modifier keys
                leftMouseButtonReleasedSingleSelection(e);
            } else if (e.isShiftDown() && dragStart != null && dragEnd != null) {
                // shift held down
                var rectangle = makeSelectionRectangle(dragStart, dragEnd);
                var selectedComponents = getComponentsInRectangle(rectangle);
                dragStart = null;
                dragEnd = null;
                handleMultipleSelection(selectedComponents);
            }

        }

        private void handleMultipleSelection(List<MusicianComponent> selectedMusicians) {
            selectedMusicians.forEach(mc -> mc.setSelected(true));
        }

        private void leftMouseButtonReleasedSingleSelection(MouseEvent e) {
            var comp = getComponentAt(e.getPoint());
            switch (mode) {
            case ADD:
                if (comp instanceof AgentPanel)
                    handleAdd(e);
                break;
            case CONNECT:
                handleMusicianConnection(comp);
                break;
            case MOVE:
                handleMusicianMove(e);
                break;
            case SELECT:
                if (!e.isControlDown())
                    deselectAll();
                if (comp instanceof MusicianComponent mc)
                    mc.setSelected(true);
                break;
            default:
                break;

            }
        }

        private void handleMusicianMove(MouseEvent e) {
            if (movingComponent != null) {
                movingComponent.setLocked(false);
                movingComponent.setPosition(e.getX() - movingComponentXY.getX(), e.getY() - movingComponentXY.getY());
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

        private void handleMusicianSelectedForEditing(Component comp) {
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

        private void handleAdd(MouseEvent e) {
            var musician = Musician.newInstance();
            musician.setRule(new RandomRule());
            musician.setMuted(true);
            MusicianList.getInstance().addMusician(musician);
            numMusicians = MusicianList.getInstance().numMusicians();
            var musicianComponent = new MusicianComponent(musician);
            musicianComponents.put(musician.getId(), musicianComponent);
            Options.getInstance().addChangeListener(MusicianComponent.RADIUS_PROPERTY, musicianComponent);
            musicianComponent.setLocked(false);
            musicianComponent.setPosition(e.getPoint().getX(), e.getPoint().getY());
            musicianComponent.setLocked(false);
            add(musicianComponent);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = e.getPoint();
            } else if (mode == EditMode.MOVE && e.getModifiersEx() == BUTTON1 && movingComponent != null) {
                movingComponent.setLocked(false);
                movingComponent.setPosition(e.getX() - movingComponentXY.getX(), e.getY() - movingComponentXY.getY());
                movingComponent.setLocked(true);
            } else if (e.getModifiersEx() == SHIFT_BUTTON1) {
                dragEnd = e.getPoint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = e.getPoint();
            }
        }

        private List<MusicianComponent> getComponentsInRectangle(Rectangle targetRect) {
            List<MusicianComponent> componentsInArea = new ArrayList<>();
            Component[] components = getComponents(); // Get all components in the panel.

            for (Component component : components) {
                // Get the bounds of the current component.
                var componentBounds = component.getBounds();

                // Check if the component's bounds intersect with the target rectangle.
                if (componentBounds.intersects(targetRect) && component instanceof MusicianComponent mc) {
                    componentsInArea.add(mc);
                }
            }

            return componentsInArea;
        }

        private void deselectAllMusicians() {
            musicianComponents.values().forEach(mc -> mc.setSelected(false));
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
        if (e.getSource() instanceof ChangeSource(String key, Object newValue)) {
            switch (key) {
            case AgentPanel.EDGE_LENGTH_PROPERTY:
                var len = (int) newValue;
                edges.forEach(edge -> edge.idealLength = len);
                break;
            case TimingSource.TEMPO_PROPERTY:
                this.tickLengthMillis = 60000 / ((int) newValue * 24);
                MusicianComponent.setTickLengthMillis(tickLengthMillis);
                break;
            default:
                break;
            }
        }
    }

    private static Rectangle makeSelectionRectangle(Point p1, Point p2) {
        var x = Math.min(p1.x, p2.x);
        var y = Math.min(p1.y, p2.y);
        var width = Math.abs(p1.x - p2.x);
        var height = Math.abs(p1.y - p2.y);
        return new Rectangle(x, y, width, height);
    }

    void setEditMode(EditMode mode) { this.mode = mode; }

    void lockAll() {
        musicianComponents.values().forEach(mc -> mc.setLocked(true));
    }

    void unlockAll() {
        musicianComponents.values().forEach(mc -> mc.setLocked(false));
    }

    void unlock(List<MusicianComponent> selectedMusicianComponents) {
        selectedMusicianComponents.forEach(mc -> mc.setLocked(false));
    }

    void lock(List<MusicianComponent> selectedMusicianComponents) {
        selectedMusicianComponents.forEach(mc -> mc.setLocked(true));
    }

    void muteAll() {
        musicianComponents.values().forEach(mc -> mc.getMusician().setMuted(true));
    }

    void unmuteAll() {
        musicianComponents.values().forEach(mc -> mc.getMusician().setMuted(false));
    }

    void mute(List<MusicianComponent> selectedMusicianComponents) {
        selectedMusicianComponents.forEach(mc -> mc.getMusician().setMuted(true));
    }

    void unmute(List<MusicianComponent> selectedMusicianComponents) {
        selectedMusicianComponents.forEach(mc -> mc.getMusician().setMuted(false));
    }

    void delete(List<MusicianComponent> selectedMusicianComponents) {
        for (var mc : selectedMusicianComponents) {
            var id = mc.getMusician().getId();
            MusicianList.getInstance().removeMusician(mc.getMusician());
            numMusicians = MusicianList.getInstance().numMusicians();
            musicianComponents.remove(mc.getMusician().getId());
            var edgeIter = edges.iterator();
            while (edgeIter.hasNext()) {
                var edge = edgeIter.next();
                if (edge.source.equals(mc) || edge.target.equals(mc))
                    edgeIter.remove();
            }
            remove(mc);
            Options.getInstance().getMusicians().remove(id);
            Options.getInstance().getUiOptions().getMusicianComponents().remove(id);
            Options.getInstance().getMusicians().values().forEach(m -> m.getPeerIds().remove(Integer.valueOf(id)));
        }
    }

    enum EditMode {
        ADD, CONNECT, MOVE, SELECT;
    }

    void selectAll() {
        musicianComponents.values().forEach(mc -> mc.setSelected(true));
    }

    void deselectAll() {
        musicianComponents.values().forEach(mc -> mc.setSelected(false));
    }

    void muteSelected() {
        musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                .forEach(mc -> mc.getMusician().setMuted(true));
    }

    void unmuteSelected() {
        musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                .forEach(mc -> mc.getMusician().setMuted(false));
    }

    void lockSelected() {
        musicianComponents.values().stream().filter(MusicianComponent::isSelected).forEach(mc -> mc.setLocked(true));
    }

    void unlockSelected() {
        musicianComponents.values().stream().filter(MusicianComponent::isSelected).forEach(mc -> mc.setLocked(false));
    }

    void deleteSelected() {
        var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
        delete(selectedMusicians);
    }

    void connectSelected() {
        var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
        for (int i = 0; i < selectedMusicians.size() - 1; i++) {
            for (int j = i + 1; j < selectedMusicians.size(); j++) {
                var mc1 = selectedMusicians.get(i);
                var mus1 = mc1.getMusician();
                var mc2 = selectedMusicians.get(j);
                var mus2 = mc2.getMusician();
                mus1.addPeer(mus2);
                mus2.addPeer(mus1);
                edges.add(new Edge(mc1, mc2, DEFAULT_EDGE_LENGTH));
                edges.add(new Edge(mc2, mc1, DEFAULT_EDGE_LENGTH));
            }
        }
    }

    void disconnectSelected() {
        var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
        for (int i = 0; i < selectedMusicians.size() - 1; i++) {
            for (int j = i + 1; j < selectedMusicians.size(); j++) {
                var mc1 = selectedMusicians.get(i);
                var mc2 = selectedMusicians.get(j);
                var edgeIter = edges.iterator();
                while (edgeIter.hasNext()) {
                    var edge = edgeIter.next();
                    if ((mc1.equals(edge.source) && mc2.equals(edge.target))
                            || (mc2.equals(edge.source) && mc1.equals(edge.target))) {
                        edge.source.getMusician().removePeer(edge.target.getMusician());
                        edgeIter.remove();
                    }
                }
            }
        }
    }

    void copySelectedComponents() {
        this.copiedComponents = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
    }

    void paste() {
        if (this.copiedComponents == null)
            return;
        for (var componentToCopy : copiedComponents) {
            var musicianToCopy = componentToCopy.getMusician();
            var newMusician = Musician.newInstance();
            newMusician.setRule((AbstractMusicianRule) musicianToCopy.getRule());
            newMusician.setChannel(musicianToCopy.getChannel());
            newMusician.setKey(musicianToCopy.getKey());
            newMusician.setMuted(musicianToCopy.isMuted());
            var newComponent = addMusicianComponent(newMusician);
            newComponent.setMass(componentToCopy.getMass());
            newComponent.setCircleRadius(componentToCopy.getRadius());
        }
        invalidate();
        this.copiedComponents = null;
        deselectAll();
    }

    void reset() {
        this.musicianComponents.values().forEach(this::remove);
        this.musicianComponents.clear();
        this.edges.clear();
        this.numMusicians = 0;
    }

    void addCircle(int numToAdd) {
        if (numToAdd < 3)
            return;
        var list = IntStream.range(0, numToAdd).mapToObj(_ -> Musician.newInstance()).toList();
        list.forEach(m -> {
            m.setRule(new StateBasedRule());
            MusicianList.getInstance().addMusician(m);
        });
        for (int i = 0; i < list.size() - 1; i++) {
            list.get(i).addPeer(list.get(i + 1));
        }
        list.get(list.size() - 1).addPeer(list.get(0));
        var components = list.stream().map(this::addMusicianComponent).toList();
        createEdges(components);
    }

    private void createEdges(List<MusicianComponent> components) {
        for (var component : components) {
            for (var peerId : component.getMusician().peerIds()) {
                var edge = new Edge(component, musicianComponents.get(peerId),
                        Options.getInstance().getUiOptions().getEdgeLength());
                edges.add(edge);
            }
        }
    }

    void addGrid(int numRows, int numCols) {
        if (numRows <= 0 || numCols <= 0)
            return;
        var list = new ArrayList<Musician>();
        var arr = new Musician[numRows][numCols];
        for (var y = 0; y < numRows; y++) {
            for (var x = 0; x < numCols; x++) {
                arr[y][x] = Musician.newInstance();
                MusicianList.getInstance().addMusician(arr[y][x]);
                arr[y][x].setRule(new StateBasedRule());
                list.add(arr[y][x]);
            }
        }
        // make connections
        for (var y = 0; y < numRows; y++) {
            for (var x = 0; x < numCols; x++) {
                if (y < numRows - 1) {
                    arr[y][x].addPeer(arr[y + 1][x]);
                    arr[y + 1][x].addPeer(arr[y][x]);
                }
                if (y > 0) {
                    arr[y][x].addPeer(arr[y - 1][x]);
                    arr[y - 1][x].addPeer(arr[y][x]);
                }
                if (x < numCols - 1) {
                    arr[y][x].addPeer(arr[y][x + 1]);
                    arr[y][x + 1].addPeer(arr[y][x]);
                }
                if (x > 0) {
                    arr[y][x].addPeer(arr[y][x - 1]);
                    arr[y][x - 1].addPeer(arr[y][x]);
                }
            }
        }
        var components = list.stream().map(this::addMusicianComponent).toList();
        createEdges(components);
    }
}
