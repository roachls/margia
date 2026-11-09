package org.roach.margia.view;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.roach.margia.command.Command;
import org.roach.margia.command.CommandCaretaker;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.Transport;
import org.roach.margia.controller.rules.StateBasedRule;
import org.roach.margia.model.*;
import org.roach.margia.storage.Global;
import org.roach.margia.storage.Options;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;

/**
 * GUI element that displays multiple {@link Musician Musicians} in a
 * {@link Pane}
 */
public class AgentPanel extends Pane implements PropertyChangeListener {
    int numMusicians;
    private final Map<Integer, MusicianComponent> musicianComponents = new TreeMap<>();
    private List<Edge> edges = new ArrayList<>();
    private static final double K_REPULSION = 10000; // Repulsion constant
    private static final double K_SPRING = 0.13; // Spring constant
    private int tickLengthMillis;
    private Point2D startSelection;
    private Point2D endSelection;
    private boolean isConnecting;
    private EditMode mode = EditMode.SELECT;
    static final String SELECTED_AGENT_PROPERTY = "selected_agent";
    static final String AGENT_ADDED_PROPERTY = "agent_added";
    static final String AGENT_REMOVED_PROPERTY = "agent_removed";
    private Point2D dragStart;
    private Point2D dragEnd;
    private List<MusicianComponent> copiedComponents;
    private MusicianComponentOptions copiedComponentOptions;
    private MusicianOptions copiedMusicianOptions;
    private static final Random RANDOM = new SecureRandom();

    // Dashed stroke settings for JavaFX GraphicsContext / Shape
    private static final double SELECTION_LINE_WIDTH = 2.0;
    private static final double[] SELECTION_LINE_DASHES = new double[] { 10.0, 10.0 };

    private static final Object ANIMATE_LOCK = new Object();
    private static final double ANIMATE_SPEED = 0.015;
    private static final double[] GRAD_FRACTIONS = new double[] { 0.0, ANIMATE_SPEED, ANIMATE_SPEED * 4, 1.0 };
    private static final Color[] GRAD_COLORS = new Color[] { Color.WHITE, Color.GRAY, Color.BLUE, Color.WHITE };

    // Background layer for custom 2D drawing (replaces paintComponent)
    private final Canvas backgroundCanvas = new Canvas();

    // Replaces JPanel's built-in Swing PropertyChangeSupport
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Constructor
     */
    public AgentPanel() {
        setStyle("-fx-background-color: lightgray;");

        backgroundCanvas.widthProperty().bind(widthProperty());
        backgroundCanvas.heightProperty().bind(heightProperty());
        getChildren().add(backgroundCanvas);

        var clipRect = new Rectangle();
        clipRect.widthProperty().bind(widthProperty());
        clipRect.heightProperty().bind(heightProperty());
        setClip(clipRect);

        widthProperty().addListener((_, _, newWidth) -> Global.setScreenWidth(newWidth.intValue()));
        heightProperty().addListener((_, _, newHeight) -> Global.setScreenHeight(newHeight.intValue()));

        // Wire mouse handlers (replacing MouseAdapter)
        setupMouseHandlers();

        Options.getInstance().getUiOptions().edgeLengthProperty().addListener((_, _, newVal) -> {
            int len = newVal.intValue();
            edges.forEach(edge -> edge.idealLength = len);
        });
        Options.getInstance().getMusicOptions().tempoProperty().addListener((_, _, newVal) -> {
            var tempo = newVal.intValue();
            if (tempo != 0) {
                this.tickLengthMillis = 60000 / (tempo * 24);
                MusicianComponent.setTickLengthMillis(tickLengthMillis);
            }
        });
    }

    private Timeline animationTimeline;

    /**
     * @param propertyName property to listen to
     * @param listener     listener of property
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @param propertyName property to listen to
     * @param listener     listener to remove
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    void init() {
        var defTempo = Options.getInstance().getMusicOptions().getTempo();
        this.tickLengthMillis = 60000 / (defTempo * 24);
        this.numMusicians = MusicianList.getInstance().numMusicians();
        this.musicianComponents.clear();

        // Clear previous musician nodes while keeping backgroundCanvas at index 0
        getChildren().retainAll(backgroundCanvas);
    }

    void initMusicians() {
        for (var musician : MusicianList.getInstance().getMusicians().values()) {
            addMusicianComponent(musician);
        }

        calcEdges();

        // Replaces: new Timer(20, this).start();
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        animationTimeline = new Timeline(new KeyFrame(javafx.util.Duration.millis(20), _ -> onAnimationTick()));
        animationTimeline.setCycleCount(Animation.INDEFINITE);
        animationTimeline.play();
    }

    private MusicianComponent addMusicianComponent(Musician musician) {
        var n = new MusicianComponent(musician);
        var compOptions = n.getOptions();
        if (compOptions.getPosition().getX() == 0.0 && compOptions.getPosition().getY() == 0.0) {
            // Fallback bounds if AgentPanel hasn't completed its first layout pass yet
            int panelWidth = getWidth() > n.getDiameter() * 2 ? (int) getWidth() : 800;
            int panelHeight = getHeight() > n.getDiameter() * 2 ? (int) getHeight() : 600;

            compOptions.setPosition(new Point2D(RANDOM.nextInt(n.getDiameter(), panelWidth - n.getDiameter()),
                    RANDOM.nextInt(n.getDiameter(), panelHeight - n.getDiameter())));
            n.updateLocation();
        }
        musicianComponents.put(musician.getId(), n);
        Platform.runLater(() -> getChildren().add(n));
        numMusicians = musicianComponents.size();
        pcs.firePropertyChange(AGENT_ADDED_PROPERTY, null, n);
        return n;
    }

    // Called every 20ms by animationTimeline (replaces actionPerformed)
    private void onAnimationTick() {
        updatePhysics();
        drawBackground();
    }

    // Replaces paintComponent(Graphics g)
    private void drawBackground() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        var gc = backgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // 1. Animated radial background gradient
        synchronized (ANIMATE_LOCK) {
            var stops = new Stop[GRAD_FRACTIONS.length];
            for (int i = 0; i < GRAD_FRACTIONS.length; i++) {
                stops[i] = new Stop(GRAD_FRACTIONS[i], GRAD_COLORS[i]);
            }
            var gradient = new RadialGradient(0, 0, w / 2.0, h / 2.0, w, false, // proportional =
                                                                                // false (pixel
                                                                                // coordinates)
                    CycleMethod.NO_CYCLE, stops);
            gc.setFill(gradient);
            gc.fillRect(0, 0, w, h);
        }

        // 2. Curved edges & arrowheads between musicians
        for (Edge edge : edges) {
            gc.save();
            var edgeColor = edge.source.getColor();
            gc.setStroke(edgeColor);
            gc.setFill(edgeColor);
            gc.setLineWidth(1.0);
            gc.setLineDashes(null);

            var sCenter = edge.source.getCenter();
            var tCenter = edge.target.getCenter();
            var vector = new Vector2D(sCenter, tCenter);
            var angle = vector.angle();
            var dist = sCenter.distance(tCenter) - edge.target.getRadius();

            // JavaFX GraphicsContext.rotate() expects degrees, whereas Swing uses radians
            gc.translate(sCenter.getX(), sCenter.getY());
            gc.rotate(Math.toDegrees(angle - Math.PI / 2.0));

            // Draw quadratic curve (replaces QuadCurve2D.Double(0, 0, 5, dist / 2d, 0,
            // dist))
            gc.beginPath();
            gc.moveTo(0, 0);
            gc.quadraticCurveTo(5.0, dist / 2.0, 0, dist);
            gc.stroke();

            // Draw filled arrowhead triangle at target radius
            gc.beginPath();
            gc.moveTo(0, dist - 5.0);
            gc.lineTo(3.0, dist - 5.0);
            gc.lineTo(0, dist);
            gc.lineTo(-3.0, dist - 5.0);
            gc.closePath();
            gc.fill();

            gc.restore();
        }

        // 3. Connection line while dragging an edge between nodes
        if (startSelection != null && endSelection != null) {
            gc.save();
            gc.setStroke(Color.RED);
            gc.setLineWidth(SELECTION_LINE_WIDTH);
            gc.setLineDashes(SELECTION_LINE_DASHES);
            gc.strokeLine(startSelection.getX(), startSelection.getY(), endSelection.getX(), endSelection.getY());
            gc.restore();
        }

        // 4. Multi-select bounding rectangle
        if (dragStart != null && dragEnd != null) {
            gc.save();
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(SELECTION_LINE_WIDTH);
            gc.setLineDashes(SELECTION_LINE_DASHES);
            var rect = makeSelectionRectangle(dragStart, dragEnd);
            gc.strokeRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
            gc.restore();
        }
    }

    private static void animateBackground() {
        boolean wrap = false;
        synchronized (ANIMATE_LOCK) {
            for (var i = 1; i < GRAD_FRACTIONS.length - 1; i++) {
                if (GRAD_FRACTIONS[i] + ANIMATE_SPEED < 1.0) {
                    GRAD_FRACTIONS[i] += ANIMATE_SPEED;
                } else {
                    wrap = true;
                }
            }
        }
        if (wrap) {
            synchronized (ANIMATE_LOCK) {
                for (var i = GRAD_COLORS.length - 1; i > 0; i--) {
                    GRAD_COLORS[i] = GRAD_COLORS[i - 1];
                }
                // JavaFX Color.color() takes double values in [0.0, 1.0]
                GRAD_COLORS[0] = Color.color(Math.random(), Math.random(), Math.random());
                GRAD_FRACTIONS[2] = GRAD_FRACTIONS[1];
                GRAD_FRACTIONS[1] = ANIMATE_SPEED;
            }
        }
    }

    private void calcEdges() {
        edges.clear();
        for (var musEntry : MusicianList.getInstance().getMusicians().entrySet()) {
            var id = musEntry.getKey();
            var mus = musEntry.getValue();
            var peerIds = mus.peerIds();
            for (var peerId : peerIds) {
                edges.add(new Edge(musicianComponents.get(id), musicianComponents.get(peerId),
                        Options.getInstance().getUiOptions().getEdgeLength()));
            }
        }
    }

    private void updatePhysics() {
        // Reset forces
        musicianComponents.values().forEach(MusicianComponent::resetForce);

        // Calculate repulsive forces between ALL pairs of nodes (O(N^2))
        for (var n1 : musicianComponents.values()) {
            for (var n2 : musicianComponents.values().stream().filter(n -> !n.equals(n1)).toList()) {
                var unitVec = PointMath.unitVector(n2.getCenter(), n1.getCenter());
                double distance = n1.getCenter().distance(n2.getCenter());
                if (distance == 0) { // prevent division by 0
                    continue;
                }

                // Repulsion force (inverse square law)
                double force = K_REPULSION / (distance * distance);
                var forceVec = unitVec.multiply(force);
                n1.setForce(n1.getForce().add(forceVec));
                n2.setForce(n2.getForce().subtract(forceVec));
            }
        }

        // Calculate attractive forces along edges (O(E))
        synchronized (edges) {
            for (var edge : edges) {
                MusicianComponent n1 = edge.source;
                MusicianComponent n2 = edge.target;
                var unitVec = PointMath.unitVector(n2.getCenter(), n1.getCenter());
                double distance = n1.getCenter().distance(n2.getCenter());

                // Spring force (Hooke's law analog)
                double displacement = distance - edge.idealLength;
                double force = K_SPRING * displacement;
                var forceVec = unitVec.multiply(force);

                n1.setForce(n1.getForce().subtract(forceVec));
                n2.setForce(n2.getForce().add(forceVec));
            }
        }

        // Apply gravity and wind
        var center = new Point2D(getWidth() / 2.0, getHeight() / 2.0);
        for (var n1 : musicianComponents.values()) {
            var vec = new Vector2D(center, n1.getCenter());
            var dist = n1.getCenter().distance(center);
            if (dist == 0.0) { // prevent division by zero
                continue;
            }

            var gravity = Options.getInstance().getUiOptions().getGravity();
            var force = n1.getMass() * gravity / dist;
            var forceVec = vec.multiply(force);
            n1.setForce(n1.getForce().subtract(forceVec));

            /*
             * 5. wind - blow objects clockwise. We take the gravity, negate it so it points
             * away from the center, and rotate it clockwise 90 degrees.
             */
            var windVec = forceVec.flip().rotateClockwise90().normalize()
                    .multiply(Options.getInstance().getUiOptions().getWindSpeed());
            n1.setForce(n1.getForce().add(windVec));
        }

        // Update velocities and positions
        for (MusicianComponent node : musicianComponents.values()) {
            node.applyForces();
            node.clampPosition(getWidth(), getHeight());
        }

        if (Options.getInstance().getMidiOptions().isSendPanMessage()) {
            Options.getInstance().getUiOptions().getMusicianComponents().values().stream()
                    .map(mc -> mc.getPosition().getX()).min(Double::compare).ifPresent(Global::setMinComponentX);
            Options.getInstance().getUiOptions().getMusicianComponents().values().stream()
                    .map(mc -> mc.getPosition().getX()).max(Double::compare).ifPresent(Global::setMaxComponentX);
        }
        if (Options.getInstance().getMidiOptions().isSendVerticalPanMessage()
                && Options.getInstance().getMidiOptions().isVerticalPanWithRelativeLocations()) {
            Options.getInstance().getUiOptions().getMusicianComponents().values().stream()
                    .map(mc -> mc.getPosition().getY()).min(Double::compare).ifPresent(Global::setMinComponentY);
            Options.getInstance().getUiOptions().getMusicianComponents().values().stream()
                    .map(mc -> mc.getPosition().getY()).max(Double::compare).ifPresent(Global::setMaxComponentY);
        }
    }

    private final MouseHandler mouseHandler = new MouseHandler();

    private void setupMouseHandlers() {
        setOnMousePressed(mouseHandler::mousePressed);
        setOnMouseDragged(mouseHandler::mouseDragged);
        setOnMouseReleased(mouseHandler::mouseReleased);
        setOnMouseMoved(mouseHandler::mouseMoved);
    }

    private class MouseHandler {
        private MusicianComponent source;
        private MusicianComponent movingComponent;
        // Position of cursor in movingComponent local space
        private Point2D movingComponentXY;

        void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseButton.PRIMARY) {
                leftMouseButtonPressed(e);
            }
        }

        private void leftMouseButtonPressed(MouseEvent e) {
            // Replaces e.getModifiersEx() == BUTTON1 (no Shift, Ctrl, Alt, or Meta
            // modifiers held)
            if (!e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                leftMousePressedSingleSelectionMode(e);
            }
        }

        private void leftMousePressedSingleSelectionMode(MouseEvent e) {
            var clickPoint = new Point2D(e.getX(), e.getY());
            var mc = findMusicianAt(e.getX(), e.getY());
            if (mc != null) {
                mc.setSelected(false);
                switch (mode) {
                case CONNECT -> {
                    if (source != null) {
                        source.setSelected(false);
                    }
                    source = mc;
                    mc.setSelected(true);
                    isConnecting = true;
                    startSelection = clickPoint;
                    setCursor(Cursor.CROSSHAIR);
                }
                case MOVE -> {
                    mc.setSelected(true);
                    movingComponent = mc;
                    // Replaces SwingUtilities.convertPoint(AgentPanel.this, e.getPoint(), mc)
                    movingComponentXY = mc.parentToLocal(e.getX(), e.getY());
                    setCursor(Cursor.MOVE);
                }
                default -> {
                    // do nothing
                }
                }
                notifyListenersOfSelection();
            } else {
                deselectAllMusicians();
                dragStart = clickPoint;
            }
        }

        // Helper replacing Swing's Container.getComponentAt(Point)
        private MusicianComponent findMusicianAt(double x, double y) {
            var children = getChildren();
            // Iterate in reverse so top-most overlapping musicians are hit first
            for (int i = children.size() - 1; i >= 0; i--) {
                if (children.get(i) instanceof MusicianComponent mc && mc.getBoundsInParent().contains(x, y)) {
                    return mc;
                }
            }
            return null;
        }

        void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseButton.PRIMARY) {
                leftMouseButtonReleased(e);
            }
        }

        private void leftMouseButtonReleased(MouseEvent e) {
            if (dragStart != null && dragEnd != null) {
                // Multi-select drag rectangle completed
                var rectangle = makeSelectionRectangle(dragStart, dragEnd);
                var selectedComponents = getComponentsInRectangle(rectangle);
                dragStart = null;
                dragEnd = null;
                handleMultipleSelection(selectedComponents);
            } else {
                leftMouseButtonReleasedSingleSelection(e);
            }
        }

        private void handleMultipleSelection(List<MusicianComponent> selectedMusicians) {
            selectedMusicians.forEach(mc -> mc.setSelected(true));
            notifyListenersOfSelection();
        }

        private void leftMouseButtonReleasedSingleSelection(MouseEvent e) {
            var mc = findMusicianAt(e.getX(), e.getY());
            switch (mode) {
            case ADD -> {
                // In Swing, getComponentAt returned AgentPanel when clicking empty space;
                // in JavaFX, findMusicianAt returns null when no MusicianComponent is under the
                // cursor.
                if (mc == null) {
                    handleAdd(e);
                }
            }
            case CONNECT -> handleMusicianConnection(mc);
            case MOVE -> handleMusicianMove(e);
            case SELECT -> {
                if (!e.isControlDown()) {
                    deselectAll();
                }
                if (mc != null) {
                    mc.setSelected(true);
                    notifyListenersOfSelection();
                }
            }
            default -> {
                // do nothing
            }
            }
        }

        private void handleMusicianMove(MouseEvent e) {
            if (movingComponent != null) {
                var mcOpts = movingComponent.getOptions();
                mcOpts.setLocked(false);
                mcOpts.setPosition(
                        new Point2D(e.getX() - movingComponentXY.getX(), e.getY() - movingComponentXY.getY()));
                mcOpts.setLocked(true);
                movingComponent.setSelected(false);
                setCursor(Cursor.DEFAULT);
            }
            movingComponent = null;
        }

        private void handleMusicianConnection(MusicianComponent target) {
            if (isConnecting && target != null && !target.equals(source)) {
                isConnecting = false;
                // Check if already connected; if so, disconnect; otherwise, connect
                edges.stream().filter(edge -> source.equals(edge.source)).filter(edge -> target.equals(edge.target))
                        .findAny().ifPresentOrElse(connection -> {
                            connection.source.getMusician().removePeer(connection.target.getMusician());
                            edges.remove(connection);
                        }, () -> {
                            source.getMusician().addPeer(target.getMusician());
                            edges.add(new Edge(source, target, UiOptions.DEFAULT_EDGE_LENGTH));
                        });
                Options.getInstance().setDirty();
                source.setSelected(false);
                this.source = null;
                startSelection = null;
                endSelection = null;
                AgentPanel.this.setCursor(Cursor.DEFAULT);
            } else if (target == null) {
                // Replaces `else if (comp == AgentPanel.this)` when released on empty space
                isConnecting = false;
                if (source != null) {
                    source.setSelected(false);
                }
                this.source = null;
                startSelection = null;
                endSelection = null;
                AgentPanel.this.setCursor(Cursor.DEFAULT);
            }
        }

        private void handleAdd(MouseEvent e) {
            var musician = Musician.newInstance();
            var musOpts = musician.getOptions();
            musOpts.setChannel(15);
            musOpts.setMuted(true);
            MusicianList.getInstance().addMusician(musician);
            numMusicians = MusicianList.getInstance().numMusicians();
            var musicianComponent = new MusicianComponent(musician);
            musicianComponents.put(musician.getId(), musicianComponent);
            musicianComponent.getOptions().setPosition(new Point2D(e.getX(), e.getY()));
            musicianComponent.updateLocation();
            getChildren().add(musicianComponent);
            pcs.firePropertyChange(AGENT_ADDED_PROPERTY, null, musicianComponent);
            Options.getInstance().setDirty();
        }

        // Add this line inside setupMouseHandlers() alongside mousePressed,
        // mouseDragged, and mouseReleased:
        // setOnMouseMoved(mouseHandler::mouseMoved);

        void mouseDragged(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = new Point2D(e.getX(), e.getY());
            } else if (mode == EditMode.MOVE && e.isPrimaryButtonDown() && movingComponent != null) {
                var mcOpts = movingComponent.getOptions();
                mcOpts.setLocked(false);
                mcOpts.setPosition(
                        new Point2D(e.getX() - movingComponentXY.getX(), e.getY() - movingComponentXY.getY()));
                mcOpts.setLocked(true);
            } else if (mode == EditMode.SELECT) {
                dragEnd = new Point2D(e.getX(), e.getY());
            }
        }

        void mouseMoved(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                endSelection = new Point2D(e.getX(), e.getY());
            }
        }

        private List<MusicianComponent> getComponentsInRectangle(Bounds targetRect) {
            List<MusicianComponent> componentsInArea = new ArrayList<>();

            for (Node node : getChildren()) {
                if (node instanceof MusicianComponent mc && mc.getBoundsInParent().intersects(targetRect)) {
                    componentsInArea.add(mc);
                }
            }

            return componentsInArea;
        }

        private void deselectAllMusicians() {
            musicianComponents.values().forEach(mc -> mc.setSelected(false));
            notifyListenersOfSelection();
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

    private static Bounds makeSelectionRectangle(Point2D p1, Point2D p2) {
        var x = Math.min(p1.getX(), p2.getX());
        var y = Math.min(p1.getY(), p2.getY());
        var width = Math.abs(p1.getX() - p2.getX());
        var height = Math.abs(p1.getY() - p2.getY());
        return new BoundingBox(x, y, width, height);
    }

    void setEditMode(EditMode mode) { this.mode = mode; }

    class LockAllCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().forEach(mc -> mc.setLocked(true));
            if (!undo)
                CommandCaretaker.pushStack(new Memento(this, List.of(new UnlockAllCommand())));
        }
    }

    class UnlockAllCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().forEach(mc -> mc.setLocked(false));
            if (!undo)
                CommandCaretaker.pushStack(new Memento(this, List.of(new LockAllCommand())));
        }
    }

    void delete(List<MusicianComponent> selectedMusicianComponents) {
        for (var mc : selectedMusicianComponents) {
            var id = mc.getMusician().getId();
            MusicianList.getInstance().removeMusician(mc.getMusician());
            numMusicians = MusicianList.getInstance().numMusicians();
            musicianComponents.remove(id);

            // Remove any edges connected to this musician
            edges.removeIf(edge -> edge.source.equals(mc) || edge.target.equals(mc));

            // Remove the node from the JavaFX Pane's children
            getChildren().remove(mc);

            Options.getInstance().getMusicians().remove(id);
            Options.getInstance().getUiOptions().getMusicianComponents().remove(id);
            Options.getInstance().getMusicians().values().forEach(m -> m.getPeerIds().remove(Integer.valueOf(id)));

            pcs.firePropertyChange(AGENT_REMOVED_PROPERTY, null, mc);
        }

        // Redraw canvas immediately so removed edges disappear right away
        drawBackground();
    }

    enum EditMode {
        ADD, CONNECT, MOVE, SELECT;
    }

    void selectAll() {
        musicianComponents.values().forEach(mc -> mc.setSelected(true));
        notifyListenersOfSelection();
    }

    private void notifyListenersOfSelection() {
        var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                .map(MusicianComponent::getMusician).toList();
        pcs.firePropertyChange(SELECTED_AGENT_PROPERTY, Collections.emptyList(), selectedMusicians);
    }

    void deselectAll() {
        musicianComponents.values().forEach(mc -> mc.setSelected(false));
        notifyListenersOfSelection();
    }

    class MuteSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.getMusician().getOptions().setMuted(true));
            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new UnmuteSelectedCommand())));
        }

    }

    class UnmuteSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.getMusician().getOptions().setMuted(false));
            if (!undo)
                CommandCaretaker
                        .pushStack(new Memento(this, List.of(new RestoreSelectedCommand(), new MuteSelectedCommand())));
        }

    }

    private class RestoreSelectedCommand implements Command {
        private final Set<Integer> selectedAgents;

        public RestoreSelectedCommand() {
            selectedAgents = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .map(mc -> mc.getMusician().getId()).collect(Collectors.toSet());
        }

        @Override
        public void execute(boolean undo) {
            for (var mc : musicianComponents.values()) {
                mc.setSelected(selectedAgents.contains(mc.getMusician().getId()));
            }
        }
    }

    class ToggleMuteSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected).forEach(mc -> {
                boolean isMuted = mc.getMusician().getOptions().isMuted();
                mc.getMusician().getOptions().setMuted(!isMuted);
            });
            if (!undo) {
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new ToggleMuteSelectedCommand())));
            }
        }
    }

    class LockSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.setLocked(true));
            if (!undo) {
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new UnlockSelectedCommand())));
            }
        }

    }

    class UnlockSelectedCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.setLocked(false));
            if (!undo) {
                CommandCaretaker
                        .pushStack(new Memento(this, List.of(new RestoreSelectedCommand(), new LockSelectedCommand())));
            }
        }
    }

    class ToggleSelectedListeningCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.getMusician().setListening(!mc.getMusician().isListening()));
            if (!undo) {
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new ToggleSelectedListeningCommand())));
            }
        }
    }

    class SetSelectedNotListeningCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.getMusician().setListening(false));
            if (!undo) {
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new SetSelectedListeningCommand())));
            }
        }
    }

    class SetSelectedListeningCommand implements Command {
        @Override
        public void execute(boolean undo) {
            musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .forEach(mc -> mc.getMusician().setListening(true));
            if (!undo) {
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new SetSelectedNotListeningCommand())));
            }
        }
    }

    class DeleteSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
            var deletedMusicianOptions = selectedMusicians.stream().map(m -> m.getMusician().getId())
                    .collect(Collectors.toMap(id -> id, id -> Options.getInstance().getMusicians().get(id)));
            var deletedMusicianComponentOptions = selectedMusicians.stream().map(m -> m.getMusician().getId())
                    .collect(Collectors.toMap(id -> id,
                            id -> Options.getInstance().getUiOptions().getMusicianComponents().get(id)));
            var mapOfOthersToThese = new HashMap<Integer, List<Integer>>();
            for (var selectedMusician : selectedMusicians) {
                var id = selectedMusician.getMusician().getId();
                for (var otherMusicianEntry : musicianComponents.entrySet()) {
                    if (otherMusicianEntry.getValue().getMusician().peerIds().contains(id)) {
                        var list = mapOfOthersToThese.computeIfAbsent(id, _ -> new ArrayList<>());
                        list.add(otherMusicianEntry.getKey());
                    }
                }
            }
            delete(selectedMusicians);
            if (!undo) {
                CommandCaretaker
                        .pushStack(new Memento(this, List.of(new RestoreDeletedMusiciansCommand(selectedMusicians,
                                deletedMusicianOptions, deletedMusicianComponentOptions, mapOfOthersToThese))));
            }
        }
    }

    private class RestoreDeletedMusiciansCommand implements Command {
        private final List<MusicianComponent> deletedMusicians;
        private final Map<Integer, MusicianOptions> deletedMusicianOptions;
        private final Map<Integer, MusicianComponentOptions> deletedMusicianComponentOptions;
        // Key = id of deleted musician, value = list of musicians with the deleted
        // musician as a peer
        private final Map<Integer, List<Integer>> mapOfOthersToThese;

        RestoreDeletedMusiciansCommand(List<MusicianComponent> deletedMusicians,
                Map<Integer, MusicianOptions> deletedMusicianOptions,
                Map<Integer, MusicianComponentOptions> deletedMusicianComponentOptions,
                Map<Integer, List<Integer>> mapOfOthersToThese) {
            this.deletedMusicians = deletedMusicians;
            this.deletedMusicianOptions = deletedMusicianOptions;
            this.deletedMusicianComponentOptions = deletedMusicianComponentOptions;
            this.mapOfOthersToThese = mapOfOthersToThese;
        }

        @Override
        public void execute(boolean undo) {
            for (var mc : deletedMusicians) {
                var id = mc.getMusician().getId();
                MusicianList.getInstance().addMusician(mc.getMusician());
                numMusicians = MusicianList.getInstance().numMusicians();
                musicianComponents.put(id, mc);

                // Replaces Swing's add(mc)
                getChildren().add(mc);
                mc.updateLocation();
                mc.draw();

                Options.getInstance().getMusicians().put(id, deletedMusicianOptions.get(id));
                Options.getInstance().getUiOptions().getMusicianComponents().put(id,
                        deletedMusicianComponentOptions.get(id));
                pcs.firePropertyChange(AGENT_ADDED_PROPERTY, null, mc);
            }
            // Restore peers
            for (var otherEntry : mapOfOthersToThese.entrySet()) {
                var listOfOthers = otherEntry.getValue();
                for (var otherId : listOfOthers) {
                    musicianComponents.get(otherId).getMusician()
                            .addPeer(musicianComponents.get(otherEntry.getKey()).getMusician());
                }
            }
            // Re-calc edges
            synchronized (edges) {
                edges.clear();
                calcEdges();
            }
            drawBackground();
        }
    }

    class ConnectSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
            var selectedMusicians = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
            for (int i = 0; i < selectedMusicians.size() - 1; i++) {
                for (int j = i + 1; j < selectedMusicians.size(); j++) {
                    var mc1 = selectedMusicians.get(i);
                    var mus1 = mc1.getMusician();
                    var mc2 = selectedMusicians.get(j);
                    var mus2 = mc2.getMusician();
                    mus1.addPeer(mus2);
                    mus2.addPeer(mus1);
                    edges.add(new Edge(mc1, mc2, UiOptions.DEFAULT_EDGE_LENGTH));
                    edges.add(new Edge(mc2, mc1, UiOptions.DEFAULT_EDGE_LENGTH));
                }
            }
            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new DisconnectSelectedCommand())));
        }
    }

    class DisconnectSelectedCommand implements Command {

        @Override
        public void execute(boolean undo) {
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
            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new RestoreSelectedCommand(), new ConnectSelectedCommand())));
        }
    }

    void copySelectedComponents() {
        this.copiedComponents = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
    }

    void copySelectedComponentOptions() {
        var firstSelected = musicianComponents.values().stream().filter(MusicianComponent::isSelected).findAny();
        firstSelected.ifPresent(c -> {
            this.copiedComponentOptions = c.getOptions();
            this.copiedMusicianOptions = c.getMusician().getOptions();
        });
    }

    void selectConnected() {
        var selected = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
        var seen = new HashSet<Integer>();
        selected.forEach(s -> seen.add(s.getMusician().getId()));
        for (var comp : selected) {
            _selectConnected(seen, comp);
        }
    }

    @SuppressWarnings("java:S100")
    private void _selectConnected(HashSet<Integer> seen, MusicianComponent comp) {
        var peerIds = comp.getMusician().peerIds().stream().filter(pi -> !seen.contains(pi)).toList();
        for (var peerId : peerIds) {
            seen.add(peerId);
            var peer = musicianComponents.get(peerId);
            peer.setSelected(true);
            _selectConnected(seen, peer);
        }
        notifyListenersOfSelection();
    }

    void paste() {
        if (this.copiedComponents == null) {
            return;
        }
        for (var componentToCopy : copiedComponents) {
            var musicianIdToCopy = componentToCopy.getMusician().getId();
            var musicianToCopyOpts = componentToCopy.getMusician().getOptions()
                    .copy(MusicianOptions.ID_GENERATOR.getAndIncrement());
            var newMusician = Musician.restoreFromStorage(musicianToCopyOpts);

            addMusicianComponent(newMusician);
            Options.getInstance().getUiOptions().copyMusicianComponentOptions(musicianIdToCopy, newMusician.getId());
        }
        requestLayout();
        drawBackground();
        this.copiedComponents = null;
        deselectAll();
    }

    void pasteSettings() {
        if (this.copiedComponentOptions == null || this.copiedMusicianOptions == null)
            return;
        var selectedComponents = musicianComponents.values().stream().filter(MusicianComponent::isSelected).toList();
        for (var comp : selectedComponents) {
            copiedComponentOptions.copyInto(comp.getOptions());
            copiedMusicianOptions.copyInto(comp.getMusician().getOptions());
        }
    }

    void reset() {
        this.musicianComponents.values().forEach(mc -> getChildren().remove(mc));
        this.musicianComponents.clear();
        this.edges.clear();
        this.numMusicians = 0;
        drawBackground();
    }

    void addCircle(int numToAdd) {
        if (numToAdd < 3)
            return;
        var list = IntStream.range(0, numToAdd).mapToObj(_ -> Musician.newInstance()).toList();
        list.forEach(m -> MusicianList.getInstance().addMusician(m));
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

    private class SelectAddedCommand implements Command {
        private final List<MusicianComponent> addedMcs;

        SelectAddedCommand(List<MusicianComponent> addedMcs) {
            this.addedMcs = addedMcs;
        }

        @Override
        public void execute(boolean undo) {
            addedMcs.forEach(mc -> mc.setSelected(true));
        }

    }

    class AddGridCommand implements Command {
        final int numRows;
        final int numCols;

        public AddGridCommand(int numRows, int numCols) {
            super();
            this.numRows = numRows;
            this.numCols = numCols;
            if (numRows <= 0)
                throw new IllegalArgumentException("Must have at least 1 row");
            if (numCols <= 0)
                throw new IllegalArgumentException("Must have at least 1 column");
        }

        @Override
        @SuppressWarnings("java:S3776")
        public void execute(boolean undo) {

            var list = new ArrayList<Musician>();
            var arr = new Musician[numRows][numCols];
            for (var y = 0; y < numRows; y++) {
                for (var x = 0; x < numCols; x++) {
                    arr[y][x] = Musician.newInstance();
                    MusicianList.getInstance().addMusician(arr[y][x]);
                    arr[y][x].getOptions().getRuleOptions().setName(new StateBasedRule().getName());
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
            var components = list.stream().map(AgentPanel.this::addMusicianComponent).toList();
            createEdges(components);

            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new SelectAddedCommand(components), new DeleteSelectedCommand())));
        }

    }

    /**
     * Direction in which fan peer relationships should be constructed
     */
    public enum FanDirection {
        /**
         * node to children
         */
        DOWN,
        /**
         * node to parent
         */
        UP,
        /**
         * node to children and parent
         */
        BIDIRECTIONAL;
    }

    class AddFanCommand implements Command {
        final int levels;
        final FanDirection direction;

        AddFanCommand(int levels, FanDirection direction) {
            super();
            if (levels <= 2 || levels > 10)
                throw new IllegalArgumentException("Levels must be Between 2 and 10 inclusive");
            this.levels = levels;
            this.direction = direction;
        }

        @Override
        @SuppressWarnings("java:S3776")
        public void execute(boolean undo) {
            var num = Math.powExact(2, levels) - 1;
            var numWithConnections = Math.powExact(2, levels - 1) - 1;
            var arr = new Musician[num];
            for (var y = 0; y < num; y++) {
                arr[y] = Musician.newInstance();
                MusicianList.getInstance().addMusician(arr[y]);
                arr[y].getOptions().getRuleOptions().setName(new StateBasedRule().getName());
            }
            // make connections
            for (var y = 0; y < num; y++) {
                if ((direction == FanDirection.DOWN || direction == FanDirection.BIDIRECTIONAL)
                        && y < numWithConnections) {
                    arr[y].addPeer(arr[y * 2 + 1]);
                    arr[y].addPeer(arr[y * 2 + 2]);
                }
                if ((direction == FanDirection.UP || direction == FanDirection.BIDIRECTIONAL) && y > 0) {
                    arr[y].addPeer(arr[(y - 1) / 2]);
                }
            }
            var components = Arrays.asList(arr).stream().map(AgentPanel.this::addMusicianComponent).toList();

            createEdges(components);

            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new SelectAddedCommand(components), new DeleteSelectedCommand())));
        }
    }

    class AddNMusiciancCommand implements Command {
        final int n;

        AddNMusiciancCommand(int n) {
            super();
            if (n < 1)
                throw new IllegalArgumentException("Number of musicians to add must be at least 1");
            this.n = n;
        }

        @Override
        public void execute(boolean undo) {
            var list = new ArrayList<Musician>();
            for (var i = 0; i < n; i++) {
                var m = Musician.newInstance();
                list.add(m);
                MusicianList.getInstance().addMusician(m);
            }
            var components = list.stream().map(AgentPanel.this::addMusicianComponent).toList();
            createEdges(components);

            if (!undo)
                CommandCaretaker.pushStack(
                        new Memento(this, List.of(new SelectAddedCommand(components), new DeleteSelectedCommand())));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if (Transport.TICK_PROPERTY.equals(evt.getPropertyName())
                && Options.getInstance().getUiOptions().isAnimateBackground()) {
            // animateBackground() only mutates synchronized gradient arrays (read by
            // drawBackground on FX thread)
            animateBackground();
        } else if (OptionsWindow.OPTIONS_AGENTS_SELECTED_PROPERTY.equals(evt.getPropertyName())) {
            var selectedMusicianIds = (List<Integer>) evt.getNewValue();
            Platform.runLater(() -> {
                deselectAll();
                for (var id : selectedMusicianIds) {
                    var mc = musicianComponents.get(id);
                    if (mc != null) {
                        mc.setSelected(true);
                    }
                }
            });
        }
    }

}
