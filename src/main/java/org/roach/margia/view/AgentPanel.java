package org.roach.margia.view;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.roach.margia.command.Command;
import org.roach.margia.command.CommandCaretaker;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.*;
import org.roach.margia.storage.Global;
import org.roach.margia.storage.Options;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * 3D GUI element that displays multiple {@link Musician Musicians} in a
 * {@link SubScene} with a 2D HUD overlay.
 */
public class AgentPanel extends Pane {
    int numMusicians;
    private final Map<Integer, MusicianComponent> musicianComponents = new TreeMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private static final double K_REPULSION = 10000; // Repulsion constant
    private static final double K_SPRING = 0.13; // Spring constant
    private static final double WORLD_HALF_EXTENT = 450.0; // Half-size of 3D simulation cube
    private int tickLengthMillis;

    private Point2D startSelection;
    private Point2D endSelection;
    private boolean isConnecting;
    private EditMode mode = EditMode.SELECT;
    private Point2D dragStart;
    private Point2D dragEnd;

    private List<MusicianComponent> copiedComponents;
    private MusicianComponentOptions copiedComponentOptions;
    private MusicianOptions copiedMusicianOptions;
    private static final Random RANDOM = new SecureRandom();

    private static final double SELECTION_LINE_WIDTH = 2.0;
    private static final double[] SELECTION_LINE_DASHES = new double[] { 10.0, 10.0 };
    private static final Font LOCK_FONT = Font.font("SansSerif", 14);

    private final ObservableList<Musician> selectedMusicians = FXCollections.observableArrayList();
    private final ObservableList<Musician> readOnlySelectedMusicians = FXCollections
            .unmodifiableObservableList(selectedMusicians);

    private final ObjectProperty<MusicianComponent> lastAddedAgent = new SimpleObjectProperty<>();
    private final ObjectProperty<MusicianComponent> lastRemovedAgent = new SimpleObjectProperty<>();
    private boolean isSyncingSelection = false;
    private final MouseHandler mouseHandler = new MouseHandler();

    private final List<Ripple> ripples = new ArrayList<>();

    // 3D Scene Graph & Camera Rig
    private final Group worldRoot = new Group();
    private final Group edgeLayer = new Group();
    private final Group nodeLayer = new Group();
    private final Group cameraPivot = new Group();
    private final Rotate cameraRotateX = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate cameraRotateY = new Rotate(0, Rotate.Y_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private SubScene subScene;

    @FXML
    private Canvas backgroundCanvas;
    @FXML
    private Rectangle clipRect;
    private Timeline animationTimeline;

    private static final double DEFAULT_CAMERA_Z = -950.0;
    private static final double DEFAULT_CAMERA_PITCH = -20.0;
    private static final double DEFAULT_CAMERA_YAW = 15.0;

    private boolean autoOrbit = false;
    private Timeline cameraResetTimeline;

    /**
     * Constructor
     */
    public AgentPanel() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AgentPanel.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load AgentPanel.fxml", e);
        }

        this.backgroundProperty()
                .bind(Bindings.createObjectBinding(
                        () -> Background.fill(Options.getInstance().getUiOptions().backgroundColorProperty().get()),
                        Options.getInstance().getUiOptions().backgroundColorProperty()));
    }

    @FXML
    private void initialize() {
        setup3DScene();

        backgroundCanvas.widthProperty().bind(widthProperty());
        backgroundCanvas.heightProperty().bind(heightProperty());
        backgroundCanvas.setMouseTransparent(true);
        backgroundCanvas.toFront();

        clipRect.widthProperty().bind(widthProperty());
        clipRect.heightProperty().bind(heightProperty());

        widthProperty().addListener((_, _, newWidth) -> Global.setScreenWidth(newWidth.intValue()));
        heightProperty().addListener((_, _, newHeight) -> Global.setScreenHeight(newHeight.intValue()));

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
        Options.getInstance().getUiOptions().threeDModeProperty().addListener((_, _, is3D) -> {
            Platform.runLater(() -> applyDimensionMode(is3D));
        });
        applyDimensionMode(Options.getInstance().getUiOptions().isThreeDMode());
    }

    private void applyDimensionMode(boolean is3D) {
        if (cameraResetTimeline != null) {
            cameraResetTimeline.stop();
        }
        if (is3D) {
            cameraRotateX.setAngle(DEFAULT_CAMERA_PITCH);
            cameraRotateY.setAngle(DEFAULT_CAMERA_YAW);

            for (var mc : musicianComponents.values()) {
                var pos = mc.getPosition();
                if (Math.abs(pos.getZ()) < 0.01) {
                    double zNudge = RANDOM.nextDouble(-35.0, 35.0);
                    mc.getOptions().setPosition(new Point3D(pos.getX(), pos.getY(), zNudge));
                    mc.updateLocation();
                }
            }
        } else {
            autoOrbit = false;
            cameraPivot.setTranslateX(0);
            cameraPivot.setTranslateY(0);
            cameraPivot.setTranslateZ(0);
            cameraRotateX.setAngle(0);
            cameraRotateY.setAngle(0);
            camera.setTranslateZ(DEFAULT_CAMERA_Z);

            for (var mc : musicianComponents.values()) {
                var pos = mc.getPosition();
                mc.getOptions().setPosition(new Point3D(pos.getX(), pos.getY(), 0.0));
                mc.setVelocity(mc.getVelocity().getX(), mc.getVelocity().getY(), 0.0);
                mc.updateLocation();
            }
        }
    }

    private void setup3DScene() {
        setFocusTraversable(true);

        worldRoot.getChildren().addAll(edgeLayer, nodeLayer);

        // Lighting for 3D spheres and edges
        AmbientLight ambientLight = new AmbientLight(Color.color(0.45, 0.45, 0.45));
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-400);
        keyLight.setTranslateY(-500);
        keyLight.setTranslateZ(-800);
        worldRoot.getChildren().addAll(ambientLight, keyLight);

        // Orbital camera rig centered at (0, 0, 0)
        camera.setNearClip(1.0);
        camera.setFarClip(6000.0);
        camera.setTranslateZ(-950.0);
        cameraPivot.getTransforms().addAll(cameraRotateY, cameraRotateX);
        cameraPivot.getChildren().add(camera);
        worldRoot.getChildren().add(cameraPivot);

        subScene = new SubScene(worldRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        // Insert 3D SubScene behind the 2D HUD Canvas
        getChildren().add(0, subScene);
    }

    void init() {
        var defTempo = Options.getInstance().getMusicOptions().getTempo();
        this.tickLengthMillis = 60000 / (defTempo * 24);
        this.numMusicians = MusicianList.getInstance().numMusicians();
        this.musicianComponents.clear();

        nodeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
    }

    void initMusicians() {
        for (var musician : MusicianList.getInstance().getMusicians().values()) {
            addMusicianComponent(musician);
        }

        calcEdges();

        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        animationTimeline = new Timeline(new KeyFrame(Duration.millis(20), _ -> onAnimationTick()));
        animationTimeline.setCycleCount(Animation.INDEFINITE);
        animationTimeline.play();
    }

    private MusicianComponent addMusicianComponent(Musician musician) {
        return addMusicianComponent(musician, null);
    }

    private MusicianComponent addMusicianComponent(Musician musician, Point3D initialPos) {
        var n = new MusicianComponent(musician);
        n.setOnNotePlayedCallback(this::spawnRipple);
        var compOptions = n.getOptions();

        if (initialPos != null) {
            compOptions.setPosition(initialPos);
            n.updateLocation();
        } else if (compOptions.getPosition().equals(Point3D.ZERO)) {
            boolean is3D = Options.getInstance().getUiOptions().isThreeDMode();
            if (is3D) {
                int spawnSpan = (int) (WORLD_HALF_EXTENT * 0.6);
                compOptions.setPosition(new Point3D(RANDOM.nextInt(-spawnSpan, spawnSpan + 1),
                        RANDOM.nextInt(-spawnSpan, spawnSpan + 1), RANDOM.nextInt(-spawnSpan, spawnSpan + 1)));
            } else {
                compOptions.setPosition(new Point3D(RANDOM.nextInt((int) -getWidth() / 2, (int) getWidth() / 2),
                        RANDOM.nextInt((int) -getHeight() / 2, (int) getHeight() / 2), 0));
            }
            n.updateLocation();
        }

        musicianComponents.put(musician.getId(), n);
        Platform.runLater(() -> {
            if (!nodeLayer.getChildren().contains(n)) {
                nodeLayer.getChildren().add(n);
            }
        });
        numMusicians = musicianComponents.size();
        lastAddedAgent.set(n);
        return n;
    }

    private void onAnimationTick() {
        updatePhysics();
        updateEdges3D();

        if (Options.getInstance().getUiOptions().isAnimateBackground()) {
            ripples.removeIf(ripple -> !ripple.update());
        } else if (!ripples.isEmpty()) {
            ripples.clear();
        }

        drawOverlayHUD();

        if (autoOrbit && Options.getInstance().getUiOptions().isThreeDMode()) {
            cameraRotateY.setAngle((cameraRotateY.getAngle() + 0.25) % 360.0);
        }
    }

    private void spawnRipple(MusicianComponent mc, Color color) {
        if (!Options.getInstance().getUiOptions().isAnimateBackground()) {
            return;
        }
        double screenSpan = Math.max(getWidth(), getHeight());
        if (screenSpan <= 0) {
            return;
        }
        if (ripples.size() >= Options.getInstance().getUiOptions().getMaxActiveRipples()) {
            ripples.remove(0);
        }
        var center = mc.getCenter();
        double startRadius = mc.getRadius() * 0.9;
        double maxRadius = screenSpan * Options.getInstance().getUiOptions().getRippleScreenFraction();
        ripples.add(new Ripple(center, startRadius, maxRadius, color));
    }

    private void updateEdges3D() {
        synchronized (edges) {
            for (Edge edge : edges) {
                edge.updateTransform();
            }
        }
    }

    /**
     * Renders 2D HUD elements (perspective ripples, ID numbers, status badges,
     * selection boxes, and connection drag lines) onto the transparent top Canvas.
     */
    private void drawOverlayHUD() {
        double w = backgroundCanvas.getWidth();
        double h = backgroundCanvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        var gc = backgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        if (!ripples.isEmpty()) {
            gc.save();
            gc.setLineDashes(null);

            double fovRad = Math.toRadians(camera.getFieldOfView());
            double focalLength = (h / 2.0) / Math.tan(fovRad / 2.0);

            for (var ripple : ripples) {
                double alpha = ripple.opacity();
                if (alpha <= 0.005) {
                    continue;
                }

                Point3D camSpace = camera.sceneToLocal(worldRoot.localToScene(ripple.center));
                if (camSpace == null || camSpace.getZ() <= camera.getNearClip()) {
                    continue;
                }

                double perspectiveScale = focalLength / camSpace.getZ();
                double screenX = (w / 2.0) + camSpace.getX() * perspectiveScale;
                double screenY = (h / 2.0) + camSpace.getY() * perspectiveScale;
                double screenRadius = ripple.radius * perspectiveScale;

                if (screenRadius <= 0.5) {
                    continue;
                }

                gc.setStroke(ripple.color.deriveColor(0, 1.0, 1.0, alpha));
                gc.setLineWidth(2.2 * Math.clamp(perspectiveScale, 0.4, 2.5));
                gc.strokeOval(screenX - screenRadius, screenY - screenRadius, screenRadius * 2.0, screenRadius * 2.0);
            }
            gc.restore();
        }

        if (MusicianComponent.isShowNumbers()) {
            gc.save();
            for (var mc : musicianComponents.values()) {
                Point2D screenPos = projectToOverlay(mc);
                if (screenPos == null) {
                    continue;
                }
                double radius = mc.getRadius();
                var font = Font.font("Serif", FontWeight.BOLD, Math.max(10.0, 12.0 * radius / 20.0));
                String idText = Integer.toString(mc.getMusician().getId());

                var textHelper = new Text(idText);
                textHelper.setFont(font);
                var bounds = textHelper.getLayoutBounds();

                double textX = screenPos.getX() - bounds.getWidth() / 2.0;
                double textY = screenPos.getY() + bounds.getHeight() / 4.0;

                gc.setFill(Color.color(0, 0, 0, 0.75));
                gc.fillOval(textX - 2.0, textY - bounds.getHeight() + 2.0, bounds.getWidth() + 4.0,
                        bounds.getHeight() + 2.0);

                gc.setFont(font);
                gc.setFill(Color.WHITE);
                gc.fillText(idText, textX, textY);

                String status = "";
                if (mc.isLocked()) {
                    status += "🔒";
                }
                if (!mc.getMusician().isListening()) {
                    status += "🎤\u033D";
                }
                if (!status.isEmpty()) {
                    gc.setFont(LOCK_FONT);
                    gc.setFill(Color.YELLOW);
                    gc.fillText(status, screenPos.getX() - radius * 0.7, screenPos.getY() + radius * 0.9);
                }
            }
            gc.restore();
        }

        // 3. Draw active connection preview line
        if (startSelection != null && endSelection != null) {
            gc.save();
            gc.setStroke(Color.RED);
            gc.setLineWidth(SELECTION_LINE_WIDTH);
            gc.setLineDashes(SELECTION_LINE_DASHES);
            gc.strokeLine(startSelection.getX(), startSelection.getY(), endSelection.getX(), endSelection.getY());
            gc.restore();
        }

        // 4. Draw multi-select rubber-band rectangle
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

    private Point2D projectToOverlay(MusicianComponent mc) {
        return projectWorldToCanvas(mc.getCenter());
    }

    /**
     * Projects a 3D point in worldRoot space into exact 2D pixel coordinates on
     * backgroundCanvas, accounting for the PerspectiveCamera's field of view and
     * depth divide.
     */
    private Point2D projectWorldToCanvas(Point3D worldPoint) {
        double w = backgroundCanvas.getWidth();
        double h = backgroundCanvas.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        // Transform world point into camera-local space (camera looks down +Z from
        // origin)
        Point3D camSpace = camera.sceneToLocal(worldRoot.localToScene(worldPoint));
        if (camSpace == null || camSpace.getZ() <= camera.getNearClip()) {
            return null; // Behind camera or clipped
        }

        // Compute exact focal length from SubScene height and camera vertical FOV
        double fovRad = Math.toRadians(camera.getFieldOfView());
        double focalLength = (h / 2.0) / Math.tan(fovRad / 2.0);
        double scale = focalLength / camSpace.getZ();

        double screenX = (w / 2.0) + camSpace.getX() * scale;
        double screenY = (h / 2.0) + camSpace.getY() * scale;
        return new Point2D(screenX, screenY);
    }

    /**
     * Smoothly animates the 3D camera back to its default angle, pan target, and
     * zoom.
     */
    public void resetCamera() {
        if (!Options.getInstance().getUiOptions().isThreeDMode()) {
            return;
        }
        autoOrbit = false;
        if (cameraResetTimeline != null) {
            cameraResetTimeline.stop();
        }
        cameraResetTimeline = new Timeline(new KeyFrame(Duration.millis(350),
                new KeyValue(cameraPivot.translateXProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(cameraPivot.translateYProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(cameraPivot.translateZProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(cameraRotateX.angleProperty(), DEFAULT_CAMERA_PITCH, Interpolator.EASE_BOTH),
                new KeyValue(cameraRotateY.angleProperty(), DEFAULT_CAMERA_YAW, Interpolator.EASE_BOTH),
                new KeyValue(camera.translateZProperty(), DEFAULT_CAMERA_Z, Interpolator.EASE_BOTH)));
        cameraResetTimeline.play();
    }

    /**
     * Toggles gentle continuous Y-axis camera rotation in 3D mode.
     */
    public void toggleAutoOrbit() {
        if (!Options.getInstance().getUiOptions().isThreeDMode()) {
            return;
        }
        autoOrbit = !autoOrbit;
    }

    private void calcEdges() {
        synchronized (edges) {
            edges.forEach(e -> edgeLayer.getChildren().remove(e.group));
            edges.clear();
            for (var musEntry : MusicianList.getInstance().getMusicians().entrySet()) {
                var id = musEntry.getKey();
                var mus = musEntry.getValue();
                for (var peerId : mus.peerIds()) {
                    var sourceComp = musicianComponents.get(id);
                    var targetComp = musicianComponents.get(peerId);
                    if (sourceComp != null && targetComp != null) {
                        addEdgeInternal(sourceComp, targetComp, Options.getInstance().getUiOptions().getEdgeLength());
                    }
                }
            }
        }
    }

    private void addEdgeInternal(MusicianComponent source, MusicianComponent target, double idealLength) {
        var edge = new Edge(source, target, idealLength);
        edges.add(edge);
        edgeLayer.getChildren().add(edge.group);
    }

    private void removeEdgeInternal(Edge edge) {
        edges.remove(edge);
        edgeLayer.getChildren().remove(edge.group);
    }

    private void updatePhysics() {
        musicianComponents.values().forEach(MusicianComponent::resetForce);
        // Repulsive forces between ALL pairs of nodes in 3D (O(N^2))
        var nodes = new ArrayList<>(musicianComponents.values());
        for (int i = 0; i < nodes.size(); i++) {
            var n1 = nodes.get(i);
            for (int j = i + 1; j < nodes.size(); j++) {
                var n2 = nodes.get(j);
                Point3D diff = n1.getCenter().subtract(n2.getCenter());
                double distance = diff.magnitude();
                if (distance < 0.001) {
                    continue;
                }
                Point3D unitVec = diff.normalize();
                double force = K_REPULSION / (distance * distance);
                Point3D forceVec = unitVec.multiply(force);
                n1.setForce(n1.getForce().add(forceVec));
                n2.setForce(n2.getForce().subtract(forceVec));
            }
        }
        // Spring forces along edges in 3D
        synchronized (edges) {
            for (var edge : edges) {
                MusicianComponent n1 = edge.source;
                MusicianComponent n2 = edge.target;
                Point3D diff = n1.getCenter().subtract(n2.getCenter());
                double distance = diff.magnitude();
                if (distance < 0.001) {
                    continue;
                }
                Point3D unitVec = diff.normalize();
                double displacement = distance - edge.idealLength;
                double force = K_SPRING * displacement;
                Point3D forceVec = unitVec.multiply(force);
                n1.setForce(n1.getForce().subtract(forceVec));
                n2.setForce(n2.getForce().add(forceVec));
            }
        }

        // Central gravity toward (0,0,0) and orbital wind around the Z-axis (clockwise
        // in XY plane)
        boolean is3D = Options.getInstance().getUiOptions().isThreeDMode();
        Point3D swirlAxis = is3D ? new Point3D(0.35, 0.35, 1.0).normalize() : new Point3D(0, 0, 1);

        for (var n1 : nodes) {
            Point3D radialVec = n1.getCenter();
            double dist = radialVec.magnitude();
            if (dist < 0.001) {
                continue;
            }

            double gravity = Options.getInstance().getUiOptions().getGravity();
            double force = n1.getMass() * gravity / dist;
            Point3D forceVec = radialVec.multiply(force);
            n1.setForce(n1.getForce().subtract(forceVec));

            // Cross swirlAxis (0,0,1) with radialVec (x,y,z) -> (-y, x, 0) (clockwise in
            // screen XY)
            Point3D tangent = swirlAxis.crossProduct(radialVec);
            if (tangent.magnitude() > 0.001) {
                Point3D windVec = tangent.normalize().multiply(Options.getInstance().getUiOptions().getWindSpeed());
                n1.setForce(n1.getForce().add(windVec));
            }
        }

        // Update velocities and positions
        for (MusicianComponent node : nodes) {
            node.applyForces();
            if (is3D)
                node.clampPosition(WORLD_HALF_EXTENT);
            else {
                var options = node.getOptions();
                var pos = options.getPosition();
                options.setPosition(new Point3D(Math.clamp(pos.getX(), -getWidth() / 2, getWidth() / 2),
                        Math.clamp(pos.getY(), -getHeight() / 2, getHeight() / 2), 0));
                node.updatePosition();
            }
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

    private void setupMouseHandlers() {
        setOnMousePressed(mouseHandler::mousePressed);
        setOnMouseDragged(mouseHandler::mouseDragged);
        setOnMouseReleased(mouseHandler::mouseReleased);
        setOnMouseMoved(mouseHandler::mouseMoved);
        setOnScroll(mouseHandler::mouseScrolled);
        setOnKeyPressed(mouseHandler::keyPressed);
    }

    private class MouseHandler {
        private MusicianComponent source;
        private MusicianComponent movingComponent;
        private Point2D lastMousePos;
        private boolean isOrbiting = false;
        private boolean isPanning = false;

        void mousePressed(MouseEvent e) {
            requestFocus(); // Ensure AgentPanel receives keyboard shortcuts
            lastMousePos = new Point2D(e.getX(), e.getY());
            boolean is3D = Options.getInstance().getUiOptions().isThreeDMode();

            if (is3D) {
                // Middle-click or Shift+Right-click pans the camera focal point
                if (e.getButton() == MouseButton.MIDDLE
                        || (e.getButton() == MouseButton.SECONDARY && e.isShiftDown())) {
                    isPanning = true;
                    autoOrbit = false;
                    setCursor(Cursor.MOVE);
                    return;
                }
                // Right-click or Alt+Left-click orbits the camera
                if (e.getButton() == MouseButton.SECONDARY || (e.getButton() == MouseButton.PRIMARY && e.isAltDown())) {
                    isOrbiting = true;
                    autoOrbit = false;
                    setCursor(Cursor.CLOSED_HAND);
                    return;
                }
            }

            if (e.getButton() == MouseButton.PRIMARY) {
                leftMouseButtonPressed(e);
            }
        }

        void mouseScrolled(ScrollEvent e) {
            double zoomFactor = e.getDeltaY() > 0 ? 0.9 : 1.1;
            double newZ = Math.clamp(camera.getTranslateZ() * zoomFactor, -3500.0, -150.0);
            camera.setTranslateZ(newZ);
        }

        private void leftMouseButtonPressed(MouseEvent e) {
            if (!e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                leftMousePressedSingleSelectionMode(e);
            }
        }

        private void leftMousePressedSingleSelectionMode(MouseEvent e) {
            var clickPoint = new Point2D(e.getX(), e.getY());
            var mc = findMusicianAt(e);
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
                    startSelection = projectToOverlay(mc);
                    setCursor(Cursor.CROSSHAIR);
                }
                case MOVE -> {
                    mc.setSelected(true);
                    movingComponent = mc;
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

        /**
         * Uses JavaFX 3D raycasting (PickResult) to find the clicked MusicianComponent.
         */
        private MusicianComponent findMusicianAt(MouseEvent e) {
            PickResult pick = e.getPickResult();
            if (pick == null) {
                return null;
            }
            Node picked = pick.getIntersectedNode();
            while (picked != null) {
                if (picked instanceof MusicianComponent mc) {
                    return mc;
                }
                picked = picked.getParent();
            }
            return null;
        }

        void mouseReleased(MouseEvent e) {
            if (isOrbiting || isPanning) {
                isOrbiting = false;
                isPanning = false;
                setCursor(Cursor.DEFAULT);
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                leftMouseButtonReleased(e);
            }
        }

        private void leftMouseButtonReleased(MouseEvent e) {
            if (dragStart != null && dragEnd != null) {
                var rectangle = makeSelectionRectangle(dragStart, dragEnd);
                var selectedComponents = getComponentsInRectangle(rectangle);
                dragStart = null;
                dragEnd = null;
                handleMultipleSelection(selectedComponents);
            } else {
                leftMouseButtonReleasedSingleSelection(e);
            }
        }

        private void handleMultipleSelection(List<MusicianComponent> currentlySelectedMusicians) {
            currentlySelectedMusicians.forEach(mc -> mc.setSelected(true));
            notifyListenersOfSelection();
        }

        private void leftMouseButtonReleasedSingleSelection(MouseEvent e) {
            var mc = findMusicianAt(e);
            switch (mode) {
            case ADD -> {
                if (mc == null) {
                    handleAdd(e);
                }
            }
            case CONNECT -> handleMusicianConnection(mc);
            case MOVE -> handleMusicianMove();
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

        private void handleMusicianMove() {
            if (movingComponent != null) {
                movingComponent.getOptions().setLocked(true);
                movingComponent.setSelected(false);
                setCursor(Cursor.DEFAULT);
            }
            movingComponent = null;
        }

        private void handleMusicianConnection(MusicianComponent target) {
            if (isConnecting && target != null && !target.equals(source)) {
                isConnecting = false;
                synchronized (edges) {
                    edges.stream().filter(edge -> source.equals(edge.source)).filter(edge -> target.equals(edge.target))
                            .findAny().ifPresentOrElse(connection -> {
                                connection.source.getMusician().removePeer(connection.target.getMusician());
                                removeEdgeInternal(connection);
                            }, () -> {
                                source.getMusician().addPeer(target.getMusician());
                                addEdgeInternal(source, target, UiOptions.DEFAULT_EDGE_LENGTH);
                            });
                }
                Options.getInstance().setDirty();
                source.setSelected(false);
                this.source = null;
                startSelection = null;
                endSelection = null;
                AgentPanel.this.setCursor(Cursor.DEFAULT);
            } else if (target == null) {
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
            // Project click offset onto the camera's view plane at Z=0
            double dx = e.getX() - (getWidth() / 2.0);
            double dy = e.getY() - (getHeight() / 2.0);
            Point3D worldPos = cameraPivot.localToParent(dx, dy, 0);
            addMusicianComponent(musician, worldPos);
            Options.getInstance().setDirty();
        }

        void mouseDragged(MouseEvent e) {
            double dx = e.getX() - lastMousePos.getX();
            double dy = e.getY() - lastMousePos.getY();
            lastMousePos = new Point2D(e.getX(), e.getY());

            if (isOrbiting) {
                cameraRotateY.setAngle(cameraRotateY.getAngle() + dx * 0.35);
                cameraRotateX.setAngle(Math.clamp(cameraRotateX.getAngle() - dy * 0.35, -89.0, 89.0));
                return;
            }

            if (isPanning) {
                // Pan cameraPivot opposite to drag direction, scaled by zoom distance
                double distanceScale = Math.abs(camera.getTranslateZ()) / 950.0;
                Point3D localPan = new Point3D(-dx * distanceScale, -dy * distanceScale, 0);
                Point3D worldPan = cameraPivot.localToParent(localPan).subtract(cameraPivot.getTranslateX(),
                        cameraPivot.getTranslateY(), cameraPivot.getTranslateZ());
                cameraPivot.setTranslateX(Math.clamp(cameraPivot.getTranslateX() + worldPan.getX(), -800, 800));
                cameraPivot.setTranslateY(Math.clamp(cameraPivot.getTranslateY() + worldPan.getY(), -800, 800));
                cameraPivot.setTranslateZ(Math.clamp(cameraPivot.getTranslateZ() + worldPan.getZ(), -800, 800));
                return;
            }

            if (mode == EditMode.CONNECT && isConnecting) {
                if (source != null) {
                    startSelection = projectToOverlay(source);
                }
                endSelection = new Point2D(e.getX(), e.getY());
            } else if (mode == EditMode.MOVE && e.isPrimaryButtonDown() && movingComponent != null) {
                var mcOpts = movingComponent.getOptions();
                mcOpts.setLocked(false);
                // Scale screen delta by camera distance and rotate into world space
                double distanceScale = Math.abs(camera.getTranslateZ()) / 950.0;
                Point3D worldDelta = cameraPivot.localToParent(dx * distanceScale, dy * distanceScale, 0);
                mcOpts.setPosition(mcOpts.getPosition().add(worldDelta));
                movingComponent.updateLocation();
                mcOpts.setLocked(true);
            } else if (mode == EditMode.SELECT) {
                dragEnd = new Point2D(e.getX(), e.getY());
            }
        }

        void mouseMoved(MouseEvent e) {
            if (mode == EditMode.CONNECT && isConnecting) {
                if (source != null) {
                    startSelection = projectToOverlay(source);
                }
                endSelection = new Point2D(e.getX(), e.getY());
            }
        }

        private List<MusicianComponent> getComponentsInRectangle(Bounds targetRect) {
            List<MusicianComponent> componentsInArea = new ArrayList<>();
            for (MusicianComponent mc : musicianComponents.values()) {
                Point2D screenPos = projectToOverlay(mc);
                if (screenPos != null && targetRect.contains(screenPos)) {
                    componentsInArea.add(mc);
                }
            }
            return componentsInArea;
        }

        private void deselectAllMusicians() {
            musicianComponents.values().forEach(mc -> mc.setSelected(false));
            notifyListenersOfSelection();
        }

        void keyPressed(KeyEvent e) {
            if (!Options.getInstance().getUiOptions().isThreeDMode()) {
                return;
            }
            switch (e.getCode()) {
            case HOME, R -> {
                if (!e.isControlDown() && !e.isMetaDown()) {
                    resetCamera();
                    e.consume();
                }
            }
            case O -> {
                if (!e.isControlDown() && !e.isMetaDown()) {
                    toggleAutoOrbit();
                    e.consume();
                }
            }
            case EQUALS, PLUS, ADD -> {
                camera.setTranslateZ(Math.clamp(camera.getTranslateZ() * 0.9, -3500.0, -150.0));
                e.consume();
            }
            case MINUS, SUBTRACT -> {
                camera.setTranslateZ(Math.clamp(camera.getTranslateZ() * 1.1, -3500.0, -150.0));
                e.consume();
            }
            case LEFT -> {
                if (e.isAltDown() || selectedMusicians.isEmpty()) {
                    autoOrbit = false;
                    cameraRotateY.setAngle(cameraRotateY.getAngle() - 5.0);
                    e.consume();
                }
            }
            case RIGHT -> {
                if (e.isAltDown() || selectedMusicians.isEmpty()) {
                    autoOrbit = false;
                    cameraRotateY.setAngle(cameraRotateY.getAngle() + 5.0);
                    e.consume();
                }
            }
            case UP -> {
                if (e.isAltDown() || selectedMusicians.isEmpty()) {
                    autoOrbit = false;
                    cameraRotateX.setAngle(Math.clamp(cameraRotateX.getAngle() + 5.0, -89.0, 89.0));
                    e.consume();
                }
            }
            case DOWN -> {
                if (e.isAltDown() || selectedMusicians.isEmpty()) {
                    autoOrbit = false;
                    cameraRotateX.setAngle(Math.clamp(cameraRotateX.getAngle() - 5.0, -89.0, 89.0));
                    e.consume();
                }
            }
            default -> {
                // Let other keys bubble up to MargiaWindow shortcuts
            }
            }
        }
    }

    /**
     * 3D Edge composed of a Cylinder shaft and a custom Cone arrowhead.
     */
    static class Edge {
        private final MusicianComponent source;
        private final MusicianComponent target;
        double idealLength;
        final Group group = new Group();
        private final Cylinder shaft = new Cylinder(1.2, 10.0);
        private final MeshView arrowHead;
        private final PhongMaterial material = new PhongMaterial();

        Edge(MusicianComponent source, MusicianComponent target, double idealLength) {
            this.source = source;
            this.target = target;
            this.idealLength = idealLength;
            arrowHead = createConeMesh(4.5f, 11.0f, 8);
            shaft.setMaterial(material);
            arrowHead.setMaterial(material);
            group.setMouseTransparent(true);
            group.getChildren().addAll(shaft, arrowHead);
            updateTransform();
        }

        void updateTransform() {
            Color edgeColor = source.getColor();
            material.setDiffuseColor(edgeColor);
            material.setSpecularColor(edgeColor.brighter());
            Point3D start = source.getCenter();
            Point3D end = target.getCenter();
            Point3D diff = end.subtract(start);
            double totalDist = diff.magnitude();
            double targetRadius = target.getRadius();
            if (totalDist <= targetRadius + 2.0) {
                group.setVisible(false);
                return;
            }
            group.setVisible(true);
            Point3D dir = diff.normalize();
            Point3D tipPos = end.subtract(dir.multiply(targetRadius));
            double shaftLen = Math.max(1.0, totalDist - targetRadius - 8.0);
            Point3D shaftEnd = start.add(dir.multiply(shaftLen));
            Point3D shaftMid = start.midpoint(shaftEnd);
            // Compute rotation from Y-axis (0,1,0) to edge direction vector
            Point3D yAxis = new Point3D(0, 1, 0);
            Point3D rotAxis = yAxis.crossProduct(dir);
            double angle = Math.toDegrees(Math.acos(Math.clamp(yAxis.dotProduct(dir), -1.0, 1.0)));
            shaft.setHeight(shaftLen);
            shaft.setTranslateX(shaftMid.getX());
            shaft.setTranslateY(shaftMid.getY());
            shaft.setTranslateZ(shaftMid.getZ());
            arrowHead.setTranslateX(tipPos.getX());
            arrowHead.setTranslateY(tipPos.getY());
            arrowHead.setTranslateZ(tipPos.getZ());
            if (rotAxis.magnitude() > 0.0001) {
                Rotate rot = new Rotate(angle, rotAxis);
                shaft.getTransforms().setAll(rot);
                arrowHead.getTransforms().setAll(rot);
            } else {
                shaft.getTransforms().clear();
                arrowHead.getTransforms().clear();
            }
        }

        private static MeshView createConeMesh(float radius, float height, int divisions) {
            TriangleMesh mesh = new TriangleMesh();
            mesh.getTexCoords().addAll(0.5f, 0.5f);
            // Vertex 0: Tip at (0, 0, 0); base vertices at y = -height
            mesh.getPoints().addAll(0f, 0f, 0f);
            for (int i = 0; i < divisions; i++) {
                double theta = 2.0 * Math.PI * i / divisions;
                float x = (float) (radius * Math.cos(theta));
                float z = (float) (radius * Math.sin(theta));
                mesh.getPoints().addAll(x, -height, z);
            }
            // Center of base
            mesh.getPoints().addAll(0f, -height, 0f);
            int baseCenterIdx = divisions + 1;
            for (int i = 0; i < divisions; i++) {
                int curr = i + 1;
                int next = (i + 1) % divisions + 1;
                // Side triangle
                mesh.getFaces().addAll(0, 0, next, 0, curr, 0);
                // Base cap triangle
                mesh.getFaces().addAll(baseCenterIdx, 0, curr, 0, next, 0);
            }
            return new MeshView(mesh);
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
            synchronized (edges) {
                var iter = edges.iterator();
                while (iter.hasNext()) {
                    var edge = iter.next();
                    if (edge.source.equals(mc) || edge.target.equals(mc)) {
                        edgeLayer.getChildren().remove(edge.group);
                        iter.remove();
                    }
                }
            }
            mc.dispose();
            nodeLayer.getChildren().remove(mc);
            Options.getInstance().getMusicians().remove(id);
            Options.getInstance().getUiOptions().getMusicianComponents().remove(id);
            Options.getInstance().getMusicians().values().forEach(m -> m.getPeerIds().remove(Integer.valueOf(id)));
            lastRemovedAgent.set(mc);
        }
        drawOverlayHUD();
    }

    enum EditMode {
        ADD, CONNECT, MOVE, SELECT;
    }

    void selectAll() {
        musicianComponents.values().forEach(mc -> mc.setSelected(true));
        notifyListenersOfSelection();
    }

    private void notifyListenersOfSelection() {
        if (isSyncingSelection) {
            return;
        }
        var selected = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                .map(MusicianComponent::getMusician).toList();
        selectedMusicians.setAll(selected);
    }

    void selectAgentsById(List<? extends Integer> selectedMusicianIds) {
        Platform.runLater(() -> {
            isSyncingSelection = true;
            try {
                musicianComponents.values().forEach(mc -> mc.setSelected(false));
                for (var id : selectedMusicianIds) {
                    var mc = musicianComponents.get(id);
                    if (mc != null) {
                        mc.setSelected(true);
                    }
                }
            } finally {
                isSyncingSelection = false;
            }
        });
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
            var selectedMusiciansLocal = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .toList();
            var deletedMusicianOptions = selectedMusiciansLocal.stream().map(m -> m.getMusician().getId())
                    .collect(Collectors.toMap(id -> id, id -> Options.getInstance().getMusicians().get(id)));
            var deletedMusicianComponentOptions = selectedMusiciansLocal.stream().map(m -> m.getMusician().getId())
                    .collect(Collectors.toMap(id -> id,
                            id -> Options.getInstance().getUiOptions().getMusicianComponents().get(id)));
            var mapOfOthersToThese = new HashMap<Integer, List<Integer>>();
            for (var selectedMusician : selectedMusiciansLocal) {
                var id = selectedMusician.getMusician().getId();
                for (var otherMusicianEntry : musicianComponents.entrySet()) {
                    if (otherMusicianEntry.getValue().getMusician().peerIds().contains(id)) {
                        var list = mapOfOthersToThese.computeIfAbsent(id, _ -> new ArrayList<>());
                        list.add(otherMusicianEntry.getKey());
                    }
                }
            }
            delete(selectedMusiciansLocal);
            if (!undo) {
                CommandCaretaker
                        .pushStack(new Memento(this, List.of(new RestoreDeletedMusiciansCommand(selectedMusiciansLocal,
                                deletedMusicianOptions, deletedMusicianComponentOptions, mapOfOthersToThese))));
            }
        }
    }

    private class RestoreDeletedMusiciansCommand implements Command {
        private final List<MusicianComponent> deletedMusicians;
        private final Map<Integer, MusicianOptions> deletedMusicianOptions;
        private final Map<Integer, MusicianComponentOptions> deletedMusicianComponentOptions;
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
                mc.reattach();
                nodeLayer.getChildren().add(mc);
                mc.updateLocation();
                mc.updateAppearance();
                Options.getInstance().getMusicians().put(id, deletedMusicianOptions.get(id));
                Options.getInstance().getUiOptions().getMusicianComponents().put(id,
                        deletedMusicianComponentOptions.get(id));
                lastAddedAgent.set(mc);
            }
            for (var otherEntry : mapOfOthersToThese.entrySet()) {
                var listOfOthers = otherEntry.getValue();
                for (var otherId : listOfOthers) {
                    musicianComponents.get(otherId).getMusician()
                            .addPeer(musicianComponents.get(otherEntry.getKey()).getMusician());
                }
            }
            calcEdges();
            drawOverlayHUD();
        }
    }

    class ConnectSelectedCommand implements Command {
        @Override
        public void execute(boolean undo) {
            var selectedMusiciansLocal = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .toList();
            synchronized (edges) {
                for (int i = 0; i < selectedMusiciansLocal.size() - 1; i++) {
                    for (int j = i + 1; j < selectedMusiciansLocal.size(); j++) {
                        var mc1 = selectedMusiciansLocal.get(i);
                        var mus1 = mc1.getMusician();
                        var mc2 = selectedMusiciansLocal.get(j);
                        var mus2 = mc2.getMusician();
                        mus1.addPeer(mus2);
                        mus2.addPeer(mus1);
                        addEdgeInternal(mc1, mc2, UiOptions.DEFAULT_EDGE_LENGTH);
                        addEdgeInternal(mc2, mc1, UiOptions.DEFAULT_EDGE_LENGTH);
                    }
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
            var selectedMusiciansLocal = musicianComponents.values().stream().filter(MusicianComponent::isSelected)
                    .toList();
            synchronized (edges) {
                for (int i = 0; i < selectedMusiciansLocal.size() - 1; i++) {
                    for (int j = i + 1; j < selectedMusiciansLocal.size(); j++) {
                        var mc1 = selectedMusiciansLocal.get(i);
                        var mc2 = selectedMusiciansLocal.get(j);
                        var edgeIter = edges.iterator();
                        while (edgeIter.hasNext()) {
                            var edge = edgeIter.next();
                            if ((mc1.equals(edge.source) && mc2.equals(edge.target))
                                    || (mc2.equals(edge.source) && mc1.equals(edge.target))) {
                                edge.source.getMusician().removePeer(edge.target.getMusician());
                                edgeLayer.getChildren().remove(edge.group);
                                edgeIter.remove();
                            }
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
        drawOverlayHUD();
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
        this.musicianComponents.values().forEach(mc -> {
            mc.dispose();
            nodeLayer.getChildren().remove(mc);
        });
        this.musicianComponents.clear();
        synchronized (edges) {
            this.edges.forEach(e -> edgeLayer.getChildren().remove(e.group));
            this.edges.clear();
        }
        this.ripples.clear();
        this.numMusicians = 0;
        drawOverlayHUD();
    }

    void addCircle(int numToAdd) {
        if (numToAdd < 3)
            return;
        var list = IntStream.range(0, numToAdd).mapToObj(_ -> Musician.newInstance()).toList();
        for (Musician m : list) {
            m.getOptions().setMuted(true);
        }
        list.forEach(m -> MusicianList.getInstance().addMusician(m));
        for (int i = 0; i < list.size() - 1; i++) {
            list.get(i).addPeer(list.get(i + 1));
        }
        list.get(list.size() - 1).addPeer(list.get(0));
        var components = list.stream().map(this::addMusicianComponent).toList();
        createEdges(components);
    }

    private void createEdges(List<MusicianComponent> components) {
        synchronized (edges) {
            for (var component : components) {
                for (var peerId : component.getMusician().peerIds()) {
                    var target = musicianComponents.get(peerId);
                    if (target != null) {
                        addEdgeInternal(component, target, Options.getInstance().getUiOptions().getEdgeLength());
                    }
                }
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
                    arr[y][x].getOptions().setMuted(true);
                    MusicianList.getInstance().addMusician(arr[y][x]);
                    list.add(arr[y][x]);
                }
            }
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
         * Connections go from the root of the tree down to the leaves
         */
        DOWN,
        /**
         * Connections go from the leaves up the tree up to the root
         */
        UP,
        /**
         * Connections go from the root of the tree down to the leaves, and from the
         * leaves back up to the root
         */
        BIDIRECTIONAL;
    }

    class AddFanCommand implements Command {
        final int levels;
        final FanDirection direction;

        AddFanCommand(int levels, FanDirection direction) {
            super();
            if (levels < 2 || levels > 10)
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
                arr[y].getOptions().setMuted(true);
                MusicianList.getInstance().addMusician(arr[y]);
            }
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
            var components = Arrays.stream(arr).map(AgentPanel.this::addMusicianComponent).toList();
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
                m.getOptions().setMuted(true);
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

    /**
     * @return {@link ObservableList} of selected musicians
     */
    public ObservableList<Musician> getSelectedMusicians() { return readOnlySelectedMusicians; }

    /**
     * @return JavaFX property for last agent that was added
     */
    public ReadOnlyObjectProperty<MusicianComponent> lastAddedAgentProperty() {
        return lastAddedAgent;
    }

    /**
     * @return JavaFX property for last agent that was removed
     */
    public ReadOnlyObjectProperty<MusicianComponent> lastRemovedAgentProperty() {
        return lastRemovedAgent;
    }

    private static final class Ripple {
        final Point3D center;
        double radius;
        final double maxRadius;
        final Color color;

        Ripple(Point3D center, double startRadius, double maxRadius, Color color) {
            this.center = center;
            this.radius = startRadius;
            this.maxRadius = maxRadius;
            this.color = color;
        }

        boolean update() {
            radius += Options.getInstance().getUiOptions().getRippleExpansionSpeed();
            return radius < maxRadius;
        }

        double opacity() {
            double progress = Math.clamp(radius / maxRadius, 0.0, 1.0);
            return Math.pow(1.0 - progress, 1.8) * 0.55;
        }
    }
}