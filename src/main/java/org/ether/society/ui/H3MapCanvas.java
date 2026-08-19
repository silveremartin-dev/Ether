/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.culture.CultureEngine;
import org.ether.society.culture.CultureVector;
import org.ether.society.flux.FluxEngine;
import org.ether.society.model.Biome;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.uber.h3core.util.LatLng;

/**
 * Canvas for rendering H3 hexagonal cells.
 * Supports both 2D (flat) and 3D (isometric with elevation) rendering modes.
 * 
 * Mouse Controls:
 * - Left Click + Drag: Pan
 * - Right Click + Drag: Rotate (3D only)
 * - Mouse Wheel: Zoom
 */
public class H3MapCanvas extends Canvas {
    private static final Logger logger = LoggerFactory.getLogger(H3MapCanvas.class);

    private List<H3Cell> cells;
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private Map<Long, H3Cell> cellMap; // Fast lookup for neighbors
    private ViewMode viewMode = ViewMode.VIEW_2D;
    private DisplayMode displayMode = DisplayMode.BIOME;
    private boolean showContours = false; // Toggle for contour lines
    private boolean showFlowVectors = false; // Toggle for flux & movement vectors
    private boolean showResourceOverlay = false; // Toggle for resource deposits & capital overlay
    private boolean showHexGrid = true; // Toggle for H3 hexagonal cell grid outlines

    public boolean isShowContours() { return showContours; }
    public void setShowContours(boolean showContours) {
        this.showContours = showContours;
        draw();
    }

    public boolean isShowFlowVectors() { return showFlowVectors; }
    public void setShowFlowVectors(boolean showFlowVectors) {
        this.showFlowVectors = showFlowVectors;
        draw();
    }

    public boolean isShowResourceOverlay() { return showResourceOverlay; }
    public void setShowResourceOverlay(boolean showResourceOverlay) {
        this.showResourceOverlay = showResourceOverlay;
        draw();
    }
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // Bounds for coordinate conversion
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;

    // Mouse interaction state
    private double zoomFactor = 1.0;
    private double dragStartX;
    private double dragStartY;
    
    // 3D camera rotation


    // Tooltip
    private CellTooltip tooltip;
    private Pane tooltipContainer;
    private H3Service h3Service;
    private H3Cell hoveredCell = null;
    private boolean showLegendOverlay = false;
    private final Map<Long, Integer> h3ToBufferIndexMap = new HashMap<>();
    private org.ether.society.events.EventSystem eventSystem;

    public org.ether.society.events.EventSystem getEventSystem() { return eventSystem; }
    public void setEventSystem(org.ether.society.events.EventSystem eventSystem) {
        this.eventSystem = eventSystem;
        draw();
    }

    public boolean isShowLegendOverlay() { return showLegendOverlay; }
    public void setShowLegendOverlay(boolean show) { 
        this.showLegendOverlay = show; 
        draw(); 
    }

    private boolean tabVisible = true;
    private boolean smoothMap = true;
    private ScientificColorMap scientificColorMap = ScientificColorMap.TURBO;
    private boolean showHillshading = false;
    private boolean showSolarTerminator = false;
    private boolean enableHierarchicalLOD = true;

    public ScientificColorMap getScientificColorMap() { return scientificColorMap; }
    public void setScientificColorMap(ScientificColorMap cmap) {
        this.scientificColorMap = cmap;
        draw();
    }

    public boolean isShowHillshading() { return showHillshading; }
    public void setShowHillshading(boolean show) {
        this.showHillshading = show;
        draw();
    }

    public boolean isShowSolarTerminator() { return showSolarTerminator; }
    public void setShowSolarTerminator(boolean show) {
        this.showSolarTerminator = show;
        draw();
    }

    public boolean isEnableHierarchicalLOD() { return enableHierarchicalLOD; }
    public void setEnableHierarchicalLOD(boolean enable) {
        this.enableHierarchicalLOD = enable;
        draw();
    }

    // Zero-allocation primitive rendering buffers for 3D globe mode (60 FPS optimization)
    private double[] polyZBuf = new double[0];
    private double[][] polyPxBuf = new double[0][6];
    private double[][] polyPyBuf = new double[0][6];
    private Color[] polyColorBuf = new Color[0];
    private boolean[] polyBorderBuf = new boolean[0];
    private Integer[] polyIndexBuf = new Integer[0];

    public boolean isTabVisible() { return tabVisible; }
    public void setTabVisible(boolean tabVisible) {
        this.tabVisible = tabVisible;
        draw();
    }

    public boolean isSmoothMap() { return smoothMap; }
    public void setSmoothMap(boolean smoothMap) {
        this.smoothMap = smoothMap;
        draw();
    }

    public boolean isShowHexGrid() { return showHexGrid; }
    public void setShowHexGrid(boolean showHexGrid) {
        this.showHexGrid = showHexGrid;
        draw();
    }

    // 3D isometric parameters
    // private static final double ISO_ANGLE = Math.toRadians(30);
    private static final double ELEVATION_SCALE = 0.05; // px per meter
    private double verticalExaggeration = 25.0; // 25x exaggeration for 3D terrain relief
    private boolean autoRotating = false;
    private double autoRotationSpeed = 0.3; // degrees per frame tick

    public double getVerticalExaggeration() { return verticalExaggeration; }
    public void setVerticalExaggeration(double verticalExaggeration) { 
        this.verticalExaggeration = verticalExaggeration; 
        draw(); 
    }

    private boolean showMouseOverInfo = true;

    public boolean isShowMouseOverInfo() { return showMouseOverInfo; }
    public void setShowMouseOverInfo(boolean show) { this.showMouseOverInfo = show; }

    public boolean isAutoRotating() { return autoRotating; }
    public void setAutoRotating(boolean autoRotating) {
        this.autoRotating = autoRotating;
        if (globe3DSubScene != null) {
            globe3DSubScene.setAutoRotating(autoRotating);
        }
        draw();
    }

    public double getAutoRotationSpeed() { return autoRotationSpeed; }
    public void setAutoRotationSpeed(double autoRotationSpeed) {
        this.autoRotationSpeed = autoRotationSpeed;
    }

    public void tickAutoRotation() {
        if (autoRotating && viewMode == ViewMode.VIEW_3D) {
            centerLng = (centerLng + autoRotationSpeed + 180.0) % 360.0 - 180.0;
            draw();
            notifyMiniMap();
        }
    }

    // Center view tracking
    private double centerLat;
    private double centerLng;

    // Mini-map reference (for updates)
    private MiniMap miniMap;

    // Engines for visualization

    private CultureEngine cultureEngine;
    private org.ether.society.agents.AgentManager agentManager;

    public H3MapCanvas(double width, double height) {
        super(width, height);
        this.h3Service = new H3Service(8); // Resolution 8
        setupMouseHandlers();
        widthProperty().addListener((obs, oldW, newW) -> draw());
        heightProperty().addListener((obs, oldH, newH) -> draw());
    }

    /**
     * Set the tooltip container (must be called after construction).
     */
    public void setTooltipContainer(Pane container) {
        this.tooltipContainer = container;
        this.tooltip = new CellTooltip();
        container.getChildren().add(tooltip);
    }

    public void setEngines(FluxEngine fluxEngine, CultureEngine cultureEngine,
            org.ether.society.agents.AgentManager agentManager) {

        this.cultureEngine = cultureEngine;
        this.agentManager = agentManager;
    }

    // Hover timer for 2-second cell tooltip delay
    private javafx.animation.PauseTransition hoverTimer;
    private H3Cell pendingHoverCell = null;
    private double pendingCanvasX, pendingCanvasY, pendingSceneX, pendingSceneY;

    // Video Recording & Overlay Metadata
    private String scenarioName = "Scénario Standard";
    private String currentDateStr = "An -100000 - M.01 D.01";
    private boolean isRecordingVideo = false;
    private java.io.File videoSessionDir = null;
    private long frameCounter = 0;

    public void setScenarioName(String name) {
        if (name != null && !name.isBlank()) {
            this.scenarioName = name;
        }
        draw();
    }

    public String getScenarioName() { return scenarioName; }

    public void setCurrentDateStr(String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) {
            this.currentDateStr = dateStr;
        }
    }

    public boolean isRecordingVideo() { return isRecordingVideo; }

    public void startVideoRecording() {
        this.isRecordingVideo = true;
        logger.info("Video export mode activated via decoupled VideoExportService.");
    }

    public void stopVideoRecording() {
        this.isRecordingVideo = false;
        logger.info("Video export mode deactivated.");
    }

    public void captureTickFrame() {}
    public void captureTickFrame(long currentTick) {}
    public void truncateVideoFramesAbove(long tickIndex) {}

    private void setupMouseHandlers() {
        hoverTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        hoverTimer.setOnFinished(e -> {
            if (pendingHoverCell != null && tooltip != null && tooltipContainer != null) {
                updateTooltip(pendingCanvasX, pendingCanvasY, pendingSceneX, pendingSceneY);
            }
        });

        // Mouse wheel zoom (Google Maps style towards mouse position)
        setOnScroll(event -> {
            double delta = event.getDeltaY();
            double zoomChange = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * zoomChange));

            if (newZoom != oldZoom) {
                double mouseX = event.getX();
                double mouseY = event.getY();
                double w = getWidth();
                double h = getHeight();

                double latRange = maxLat - minLat;
                double lngRange = maxLng - minLng;
                if (latRange > 0 && lngRange > 0 && w > 0 && h > 0) {
                    double baseScale = Math.min(w / lngRange, h / latRange) * 0.9;
                    double oldScale = baseScale * oldZoom;
                    double newScale = baseScale * newZoom;

                    double mouseLng = centerLng + (mouseX - w / 2.0) / oldScale;
                    double mouseLat = centerLat - (mouseY - h / 2.0) / oldScale;

                    centerLng = mouseLng - (mouseX - w / 2.0) / newScale;
                    centerLat = mouseLat + (mouseY - h / 2.0) / newScale;
                }

                zoomFactor = newZoom;
                draw();
                notifyMiniMap();
                logger.debug("Zoom: {}x (Center: {}, {})", String.format("%.2f", zoomFactor), centerLat, centerLng);
            }
        });

        // Mouse press - determine mode
        setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();

            if (event.isSecondaryButtonDown()) {
                setCursor(javafx.scene.Cursor.CROSSHAIR);
            } else if (event.isPrimaryButtonDown()) {
                setCursor(javafx.scene.Cursor.MOVE);
            }
        });

        // Mouse drag - Rotate globe on its axis in 3D without shifting center on screen
        setOnMouseDragged(event -> {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;

            if (viewMode == ViewMode.VIEW_3D) {
                double sensitivity = 0.35 / zoomFactor;
                centerLng -= dx * sensitivity;
                centerLat += dy * sensitivity;
                centerLat = Math.max(-89.0, Math.min(89.0, centerLat));
                centerLng = Math.max(-180.0, Math.min(180.0, centerLng));

                dragStartX = event.getX();
                dragStartY = event.getY();
                draw();
                return;
            }

            // 2D Flat Map Panning
            centerLng -= dx / scale;
            centerLat += dy / scale;
            centerLat = Math.max(minLat, Math.min(maxLat, centerLat));
            centerLng = Math.max(minLng, Math.min(maxLng, centerLng));

            dragStartX = event.getX();
            dragStartY = event.getY();
            draw();
            notifyMiniMap();
        });

        // Mouse release
        setOnMouseReleased(event -> {
            setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Click on event beacon overlay or double-click to reset, or single click to update coordinates
        setOnMouseClicked(event -> {
            synchronized (activeBeaconTargets) {
                for (EventBeaconTarget target : activeBeaconTargets) {
                    if (event.getX() >= target.x && event.getX() <= target.x + target.width &&
                        event.getY() >= target.y && event.getY() <= target.y + target.height) {
                        flyTo(target.lat, target.lng);
                        return;
                    }
                }
            }
            if (event.getClickCount() == 2) {
                zoomFactor = 1.0;
                centerLat = (minLat + maxLat) / 2.0;
                centerLng = (minLng + maxLng) / 2.0;
                draw();
                notifyMiniMap();
                logger.info("Double-click: reset H3 map view to full centered perspective.");
            } else if (event.getClickCount() == 1) {
                double[] latLng = getLatLngAt(event.getX(), event.getY());
                if (latLng != null && onCellClickedCallback != null) {
                    onCellClickedCallback.accept(latLng[0], latLng[1]);
                }
            }
        });

        // Mouse move for tooltip - disabled when showMouseOverInfo is false
        setOnMouseMoved(event -> {
            if (!showMouseOverInfo) {
                if (tooltip != null) {
                    tooltip.hide();
                    hoveredCell = null;
                }
                return;
            }
            H3Cell cell = findCellAt(event.getX(), event.getY());
            if (cell == null || cell != pendingHoverCell) {
                if (tooltip != null) {
                    tooltip.hide();
                    hoveredCell = null;
                }
                hoverTimer.stop();
                pendingHoverCell = cell;
                pendingCanvasX = event.getX();
                pendingCanvasY = event.getY();
                pendingSceneX = event.getSceneX();
                pendingSceneY = event.getSceneY();

                if (cell != null) {
                    hoverTimer.playFromStart();
                }
            } else {
                pendingCanvasX = event.getX();
                pendingCanvasY = event.getY();
                pendingSceneX = event.getSceneX();
                pendingSceneY = event.getSceneY();
            }
        });

        // Hide tooltip when leaving canvas
        setOnMouseExited(event -> {
            if (hoverTimer != null) hoverTimer.stop();
            pendingHoverCell = null;
            if (tooltip != null) {
                tooltip.hide();
                hoveredCell = null;
            }
        });
    }

    public void setViewMode(ViewMode mode) {
        this.viewMode = mode;
        this.showLegendOverlay = true; // Ensure rainbow spectrum legend box is always displayed
        draw();
        logger.info("View mode changed to: {}", mode);
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        draw();
        logger.info("Display mode changed to: {}", mode);
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    // Toggle for contours
    public void toggleContours(boolean show) {
        this.showContours = show;
        draw();
    }

    public void resetView() {
        this.zoomFactor = 1.0;
        if (cells != null && !cells.isEmpty()) {
            this.centerLat = (minLat + maxLat) / 2.0;
            this.centerLng = (minLng + maxLng) / 2.0;
        }
        if (globe3DSubScene != null) {
            globe3DSubScene.resetCamera();
        }
        draw();
        notifyMiniMap();
    }

    private H3Globe3DSubScene globe3DSubScene;
    private double reliefScale = 1.0;

    public H3Globe3DSubScene getGlobe3DSubScene() {
        if (globe3DSubScene == null) {
            globe3DSubScene = new H3Globe3DSubScene(getWidth() > 0 ? getWidth() : 1280, getHeight() > 0 ? getHeight() : 800);
            if (cells != null && !cells.isEmpty()) {
                globe3DSubScene.updateTerrainMesh(cells);
            }
        }
        return globe3DSubScene;
    }

    public void setReliefScale(double scale) {
        this.reliefScale = scale;
        if (globe3DSubScene != null) {
            globe3DSubScene.setReliefScale(scale);
            if (cells != null && !cells.isEmpty()) {
                globe3DSubScene.updateTerrainMesh(cells);
            }
        }
        draw();
    }

    public double getReliefScale() {
        return reliefScale;
    }

    public List<H3Cell> getCells() { return cells; }

    public void setCells(List<H3Cell> cells) {
        // Sort by latitude for faster culling
        this.cells = new ArrayList<>(cells); // Copy to allow sorting
        this.cells.sort(Comparator.comparingDouble(H3Cell::getLatitude));
        
        // Build map for fast lookup
        this.cellMap = new HashMap<>(); // Standard mapping in O(N)
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        // Compute lat/lng bounds
        minLat = cells.stream().mapToDouble(H3Cell::getLatitude).min().orElse(0);
        maxLat = cells.stream().mapToDouble(H3Cell::getLatitude).max().orElse(0);
        minLng = cells.stream().mapToDouble(H3Cell::getLongitude).min().orElse(0);
        maxLng = cells.stream().mapToDouble(H3Cell::getLongitude).max().orElse(0);

        resetView();

        if (globe3DSubScene != null) {
            globe3DSubScene.updateTerrainMesh(cells);
        }

        logger.info("H3 Canvas initialized with {} cells. Bounds: lat[{}, {}], lng[{}, {}]",
                cells.size(), minLat, maxLat, minLng, maxLng);
    }

    public void centerOnCoordinates(double lat, double lng) {
        this.centerLat = Math.max(-90.0, Math.min(90.0, lat));
        this.centerLng = Math.max(-180.0, Math.min(180.0, lng));
        draw();
        logger.info("Canvas view re-centered to Lat: {}, Lng: {}", centerLat, centerLng);
    }

    public void setWorldBuffer(org.ether.society.core.dod.WorldBuffer buffer) {
        this.worldBuffer = buffer;
        this.h3ToBufferIndexMap.clear();
        if (buffer != null) {
            long[] indexes = buffer.getH3Indexes();
            int cap = buffer.getCapacity();
            for (int k = 0; k < cap; k++) {
                if (indexes[k] != 0L) {
                    h3ToBufferIndexMap.put(indexes[k], k);
                }
            }
        }
        draw();
    }

    public void draw() {
        if (!tabVisible || !isVisible() || getWidth() < 1.0 || getHeight() < 1.0) return;
        if (cells == null || cells.isEmpty()) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());

        // Calculate scale to fit all cells with zoom
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;
        double scaleX = getWidth() / lngRange;
        double scaleY = getHeight() / latRange;
        scale = Math.min(scaleX, scaleY) * 0.9 * zoomFactor; // Apply zoom

        // Calculate offsets to center the view on centerLat/centerLng
        // Formula: ScreenPos = (WorldPos - WorldMin) * Scale + Offset
        // We want ScreenPos(Center) = ScreenCenter
        // Offset = ScreenCenter - (WorldCenter - WorldMin) * Scale
        
        offsetX = (getWidth() / 2.0) - (centerLng - minLng) * scale;
        // height/2 = (maxLat - centerLat) * scale + offsetY
        offsetY = (getHeight() / 2.0) - (maxLat - centerLat) * scale;

        if (viewMode == ViewMode.VIEW_3D) {
            draw3D(gc, minLat, maxLat, minLng, maxLng);
        } else {
            draw2D(gc, minLat, maxLat, minLng, maxLng);
        }

        if (agentManager != null) {
            drawAgents(gc);
        }
        
        if (showContours) {
            drawContours(gc);
        }

        if (showFlowVectors) {
            drawFlowVectors(gc);
        }

        if (showResourceOverlay) {
            drawResourceOverlay(gc);
        }

        // Draw legend overlay box with rainbow spectrum, min/max, mean (μ) and median (M) indicators
        if (showLegendOverlay) {
            drawLegendOverlay(gc);
        }

        drawEventBeacons(gc);
        drawCornerOverlays(gc);

        logger.debug("Drew {} cells in {} mode", cells.size(), viewMode);
    }

    private void drawCornerOverlays(GraphicsContext gc) {
        double h = getHeight();
        double w = getWidth();
        if (h < 60 || w < 220) return;

        // Bottom-Left: Scenario Name
        String scenText = "🎬 " + (scenarioName != null ? scenarioName : "Scénario Ether");
        gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 12));
        double scenWidth = Math.max(170, scenText.length() * 8.0 + 24);

        gc.setFill(Color.rgb(15, 23, 42, 0.88));
        gc.fillRoundRect(14, h - 42, scenWidth, 28, 8, 8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.85));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(14, h - 42, scenWidth, 28, 8, 8);

        gc.setFill(Color.rgb(241, 245, 249));
        gc.fillText(scenText, 22, h - 23);

        // Bottom-Right: Scrolling Date
        String dateText = "📅 " + (currentDateStr != null ? currentDateStr : "An --");
        double dateWidth = Math.max(150, dateText.length() * 8.5 + 24);
        double dateX = w - dateWidth - 14;

        gc.setFill(Color.rgb(15, 23, 42, 0.88));
        gc.fillRoundRect(dateX, h - 42, dateWidth, 28, 8, 8);
        gc.setStroke(isRecordingVideo ? Color.rgb(239, 68, 68, 0.9) : Color.rgb(74, 222, 128, 0.85));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(dateX, h - 42, dateWidth, 28, 8, 8);

        gc.setFill(isRecordingVideo ? Color.rgb(254, 202, 202) : Color.rgb(241, 245, 249));
        gc.fillText(dateText, dateX + 12, h - 23);

        // REC Indicator on Top-Right when video recording is active
        if (isRecordingVideo) {
            long now = System.currentTimeMillis();
            boolean blink = (now % 1000) < 500;
            if (blink) {
                gc.setFill(Color.rgb(239, 68, 68));
                gc.fillOval(w - 85, 14, 11, 11);
            }
            gc.setFill(Color.rgb(248, 113, 113));
            gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 12));
            gc.fillText("REC 1:1", w - 68, 24);
        }
    }

    private static class EventBeaconTarget {
        double x, y, width, height;
        double lat, lng;
    }

    private final List<EventBeaconTarget> activeBeaconTargets = new ArrayList<>();
    private javafx.animation.AnimationTimer activeFlyTimer;

    public void flyTo(double targetLat, double targetLng) {
        if (activeFlyTimer != null) {
            activeFlyTimer.stop();
        }

        final double startLat = this.centerLat;
        final double startLng = this.centerLng;

        double dLng = targetLng - startLng;
        while (dLng > 180.0) dLng -= 360.0;
        while (dLng < -180.0) dLng += 360.0;
        final double finalTargetLng = startLng + dLng;

        final long startNs = System.nanoTime();
        final long durationNs = 1_000_000_000L; // 1 second smooth flight

        activeFlyTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = (now - startNs) / (double) durationNs;
                if (elapsed >= 1.0) {
                    centerLat = targetLat;
                    centerLng = ((finalTargetLng + 180.0) % 360.0) - 180.0;
                    draw();
                    notifyMiniMap();
                    stop();
                    activeFlyTimer = null;
                } else {
                    double t = 0.5 - 0.5 * Math.cos(elapsed * Math.PI);
                    centerLat = startLat + t * (targetLat - startLat);
                    double currentLng = startLng + t * (finalTargetLng - startLng);
                    centerLng = ((currentLng + 180.0) % 360.0) - 180.0;
                    draw();
                    notifyMiniMap();
                }
            }
        };
        activeFlyTimer.start();
        logger.info("Flying camera to lat={}, lng={}", String.format("%.2f", targetLat), String.format("%.2f", targetLng));
    }

    private void drawEventBeacons(GraphicsContext gc) {
        if (eventSystem == null) return;
        List<org.ether.society.events.ActiveEvent> events = eventSystem.getActiveEvents();
        
        synchronized (activeBeaconTargets) {
            activeBeaconTargets.clear();
        }

        if (events == null || events.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (org.ether.society.events.ActiveEvent event : events) {
            if (event.isExpired()) continue;

            double lat = event.getLatitude();
            double lng = event.getLongitude();

            double screenX, screenY;

            if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
                double cx = getWidth() / 2;
                double cy = getHeight() / 2;

                double radLat = Math.toRadians(lat);
                double radLng = Math.toRadians(lng - centerLng);

                double cosLat = Math.cos(radLat);
                double x3d = radius * cosLat * Math.sin(radLng);
                double z3d = radius * cosLat * Math.cos(radLng);
                double y3d = -radius * Math.sin(radLat);

                double radTilt = Math.toRadians(centerLat);
                double yRotated = y3d * Math.cos(radTilt) - z3d * Math.sin(radTilt);
                double zRotated = y3d * Math.sin(radTilt) + z3d * Math.cos(radTilt);

                if (zRotated < -radius * 0.1) continue; // Behind sphere

                screenX = cx + x3d;
                screenY = cy + yRotated;
            } else {
                screenX = (lng - minLng) * scale + offsetX;
                screenY = (maxLat - lat) * scale + offsetY;
            }

            if (screenX < -50 || screenX > getWidth() + 50 || screenY < -50 || screenY > getHeight() + 50) {
                continue;
            }

            // Pulse & magnitude scaling (core sphere proportional to event magnitude)
            double mag = event.getMagnitude() > 0 ? event.getMagnitude() : 5.0;
            double coreRadius = Math.clamp(6.0 + (mag * 3.5), 6.0, 48.0);
            
            long elapsedMs = event.getCreatedAtMs() > 0 ? now - event.getCreatedAtMs() : 0;
            boolean isBlinking = elapsedMs >= 0 && elapsedMs < 8000; // Pulsing ring stops blinking after 8s max

            Color eventColor = switch (event.getType()) {
                case "VOLCANO", "METEOR", "NUCLEAR_WINTER" -> Color.rgb(239, 68, 68); // Red
                case "FLOOD", "ECOLOGICAL" -> Color.rgb(14, 165, 233); // Cyan
                case "FAMINE" -> Color.rgb(234, 179, 8); // Yellow
                case "PANDEMIC" -> Color.rgb(168, 85, 247); // Purple
                case "EARTHQUAKE" -> Color.rgb(249, 115, 22); // Orange
                case "GOD_MODE" -> Color.rgb(236, 72, 153); // Pink
                default -> Color.rgb(34, 197, 94); // Green
            };

            // Inner glowing core sphere (sized by magnitude)
            gc.setFill(eventColor);
            gc.fillOval(screenX - coreRadius / 2.0, screenY - coreRadius / 2.0, coreRadius, coreRadius);

            // Outer pulsing ring (fades and stops after 8 seconds)
            if (isBlinking) {
                double cycle = (elapsedMs % 1500) / 1500.0; // 0.0 to 1.0
                double pulseRadius = coreRadius + cycle * (coreRadius * 1.6);
                double alpha = Math.max(0.0, 1.0 - cycle);

                gc.setStroke(Color.color(eventColor.getRed(), eventColor.getGreen(), eventColor.getBlue(), alpha));
                gc.setLineWidth(2.5);
                gc.strokeOval(screenX - pulseRadius / 2.0, screenY - pulseRadius / 2.0, pulseRadius, pulseRadius);
            }

            // Title & Fly-To Banner
            double titleWidth = Math.min(180, event.getTitle().length() * 7 + 10);
            double flyBtnWidth = 54;
            double totalWidth = titleWidth + flyBtnWidth + 6;

            gc.setFill(Color.rgb(15, 23, 42, 0.90));
            gc.fillRect(screenX + 8, screenY - 18, totalWidth, 20);
            gc.setStroke(eventColor);
            gc.setLineWidth(1.2);
            gc.strokeRect(screenX + 8, screenY - 18, totalWidth, 20);

            // Event Title
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 10));
            gc.fillText(event.getTitle(), screenX + 12, screenY - 4);

            // Fly-To Action Button
            double btnX = screenX + 12 + titleWidth;
            double btnY = screenY - 16;
            gc.setFill(Color.rgb(14, 165, 233, 0.95)); // Vibrant cyan
            gc.fillRoundRect(btnX, btnY, flyBtnWidth, 16, 4, 4);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 9));
            gc.fillText("✈ FLY TO", btnX + 5, btnY + 11);

            // Record target hit box
            EventBeaconTarget target = new EventBeaconTarget();
            target.x = screenX - 10;
            target.y = screenY - 20;
            target.width = totalWidth + 25;
            target.height = 32;
            target.lat = lat;
            target.lng = lng;

            synchronized (activeBeaconTargets) {
                activeBeaconTargets.add(target);
            }
        }
    }

    private java.util.function.BiConsumer<H3Cell, double[]> onHoverCallback;
    private java.util.function.BiConsumer<Double, Double> onCellClickedCallback;

    public void setOnHoverCallback(java.util.function.BiConsumer<H3Cell, double[]> callback) {
        this.onHoverCallback = callback;
    }

    public void setOnCellClickedCallback(java.util.function.BiConsumer<Double, Double> callback) {
        this.onCellClickedCallback = callback;
    }

    /**
     * Compute geographical coordinates (lat, lng) from canvas pixel position.
     * Supports both 2D Equirectangular projection and 3D Globe orthographic view.
     */
    public double[] getLatLngAt(double mouseX, double mouseY) {
        if (viewMode == ViewMode.VIEW_3D) {
            double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;

            double dx = mouseX - cx;
            double dy = cy - mouseY;
            double r2 = dx * dx + dy * dy;
            if (r2 > radius * radius) {
                return null;
            }

            double xr = dx / radius;
            double yrt = dy / radius;
            double zrt = Math.sqrt(Math.max(0.0, 1.0 - xr * xr - yrt * yrt));

            double radRotationY = Math.toRadians(-centerLng);
            double radTilt = Math.toRadians(centerLat);

            double cosT = Math.cos(radTilt);
            double sinT = Math.sin(radTilt);
            double y = yrt * cosT + zrt * sinT;
            double zr = -yrt * sinT + zrt * cosT;

            double cosR = Math.cos(radRotationY);
            double sinR = Math.sin(radRotationY);
            double x = xr * cosR - zr * sinR;
            double z = xr * sinR + zr * cosR;

            double lat = Math.toDegrees(Math.asin(Math.clamp(y, -1.0, 1.0)));
            double lng = Math.toDegrees(Math.atan2(x, z));
            return new double[]{lat, lng};
        } else {
            double lng = ((mouseX - offsetX) / scale) + minLng;
            double lat = maxLat - ((mouseY - offsetY) / scale);
            if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
                return null;
            }
            return new double[]{lat, lng};
        }
    }

    private final Map<Long, Integer> previousPopMap = new HashMap<>();

    /**
     * Update tooltip based on mouse position.
     */
    private void updateTooltip(double canvasX, double canvasY, double sceneX, double sceneY) {
        H3Cell cell = findCellAt(canvasX, canvasY);

        if (onHoverCallback != null) {
            double lat = cell != null ? cell.getLatitude() : 0.0;
            double lng = cell != null ? cell.getLongitude() : 0.0;
            onHoverCallback.accept(cell, new double[]{lat, lng});
        }

        if (cell != null) {
            if (cell != hoveredCell) {
                // Save previous pop state if missing
                if (!previousPopMap.containsKey(cell.getH3Index())) {
                    previousPopMap.put(cell.getH3Index(), cell.getPopulation() != null ? cell.getPopulation() : 0);
                }
                hoveredCell = cell;
            }

            Integer prevPop = previousPopMap.get(cell.getH3Index());

            if (worldBuffer != null) {
                int index = cells.indexOf(cell); 
                tooltip.updateFromBuffer(worldBuffer, index);
            } else {
                tooltip.updateCell(cell, prevPop);
            }
            tooltip.position(sceneX, sceneY,
                    tooltipContainer.getWidth(),
                    tooltipContainer.getHeight());
        } else {
            tooltip.hide();
            hoveredCell = null;
        }
    }

    /**
     * Find the H3 cell at the given canvas coordinates.
     * Supports exact inverse 3D ray projection on 3D spherical Globe mode.
     */
    private H3Cell findCellAt(double mouseX, double mouseY) {
        if (cells == null || cells.isEmpty()) {
            return null;
        }

        double lat, lng;
        if (viewMode == ViewMode.VIEW_3D) {
            double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;

            double dx = mouseX - cx;
            double dy = cy - mouseY;
            double r2 = dx * dx + dy * dy;
            if (r2 > radius * radius) {
                return null; // Outside sphere disk
            }

            double xr = dx / radius;
            double yrt = dy / radius;
            double zrt = Math.sqrt(Math.max(0.0, 1.0 - xr * xr - yrt * yrt));

            double radRotationY = Math.toRadians(-centerLng);
            double radTilt = Math.toRadians(centerLat);

            double cosT = Math.cos(radTilt);
            double sinT = Math.sin(radTilt);
            double y = yrt * cosT + zrt * sinT;
            double zr = -yrt * sinT + zrt * cosT;

            double cosR = Math.cos(radRotationY);
            double sinR = Math.sin(radRotationY);
            double x = xr * cosR - zr * sinR;
            double z = xr * sinR + zr * cosR;

            lat = Math.toDegrees(Math.asin(Math.clamp(y, -1.0, 1.0)));
            lng = Math.toDegrees(Math.atan2(x, z));
        } else {
            // Convert canvas coordinates to lat/lng (reverse of draw() transform)
            lng = ((mouseX - offsetX) / scale) + minLng;
            lat = maxLat - ((mouseY - offsetY) / scale);

            // Check if within bounds
            if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
                return null;
            }
        }

        // Get H3 index at this location
        long h3Index = h3Service.latLngToCell(lat, lng);

        // Fast lookup via cellMap
        if (cellMap != null && !cellMap.isEmpty()) {
            return cellMap.get(h3Index);
        }

        // Fallback search in dataset
        return cells.stream()
                .filter(c -> c.getH3Index() == h3Index)
                .findFirst()
                .orElse(null);
    }

    private void draw2D(GraphicsContext gc, double minLat, double maxLat, double minLng, double maxLng) {
        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (getHeight() - offsetY) / scale;

        double cullMaxLat = Math.min(90, viewTopLat + 10.0);
        double cullMinLat = Math.max(-90, viewBottomLat - 10.0);

        int startIndex = findLatIndex(cullMinLat);

        double lngSpan = Math.max(1.0, maxLng - minLng);
        double cellSpacing = (lngSpan / Math.sqrt(Math.max(1, cells.size()))) * scale;
        double cellSize = Math.max(5.0, cellSpacing * 1.45);
        boolean renderHexBorders = showHexGrid && !smoothMap && cellSpacing > 3.0;

        for (int i = startIndex; i < cells.size(); i++) {
            H3Cell cell = cells.get(i);
            if (cell.getLatitude() > cullMaxLat)
                break;

            double x = (cell.getLongitude() - minLng) * scale + offsetX;
            double y = (maxLat - cell.getLatitude()) * scale + offsetY;

            if (x < -cellSize * 2 || x > getWidth() + cellSize * 2 || y < -cellSize * 2 || y > getHeight() + cellSize * 2) {
                continue;
            }

            Color color = smoothMap ? getIdwInterpolatedColor(cell, i) : getBufferOrCellColor(cell);
            if (showHillshading || showSolarTerminator) {
                double f = computeHillshadeFactor(cell) * computeSolarTerminatorFactor(cell);
                color = Color.color(
                        Math.clamp(color.getRed() * f, 0.0, 1.0),
                        Math.clamp(color.getGreen() * f, 0.0, 1.0),
                        Math.clamp(color.getBlue() * f, 0.0, 1.0),
                        color.getOpacity()
                );
            }
            gc.setFill(color);

            double radiusY = Math.max(1.0, cellSize / 2.0);
            double cosLat = Math.max(0.20, Math.cos(Math.toRadians(cell.getLatitude())));
            double radiusX = Math.max(radiusY * 0.45, (cellSize / 2.0) * cosLat);

            // In smooth map mode, slightly dilate polygons to ensure a continuous surface without gaps
            double scaleFactor = smoothMap ? 1.05 : 1.0;
            drawHexCell2D(gc, x, y, radiusX * scaleFactor, radiusY * scaleFactor);

            if (renderHexBorders) {
                gc.setStroke(Color.rgb(15, 23, 42, 0.35));
                gc.setLineWidth(0.8);
                strokeHexCell2D(gc, x, y, radiusX, radiusY);
            }
        }
    }

    private void drawHexCell2D(GraphicsContext gc, double cx, double cy, double radius) {
        drawHexCell2D(gc, cx, cy, radius, radius);
    }

    private void drawHexCell2D(GraphicsContext gc, double cx, double cy, double radiusX, double radiusY) {
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            xs[i] = cx + radiusX * Math.cos(angle);
            ys[i] = cy + radiusY * Math.sin(angle);
        }
        gc.fillPolygon(xs, ys, 6);
    }

    private void strokeHexCell2D(GraphicsContext gc, double cx, double cy, double radius) {
        strokeHexCell2D(gc, cx, cy, radius, radius);
    }

    private void strokeHexCell2D(GraphicsContext gc, double cx, double cy, double radiusX, double radiusY) {
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            xs[i] = cx + radiusX * Math.cos(angle);
            ys[i] = cy + radiusY * Math.sin(angle);
        }
        gc.strokePolygon(xs, ys, 6);
    }

    private int findLatIndex(double targetLat) {
        int low = 0;
        int high = cells.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = cells.get(mid).getLatitude();

            if (midVal < targetLat)
                low = mid + 1;
            else if (midVal > targetLat)
                high = mid - 1;
            else
                return mid;
        }
        return low;
    }

    private void draw3D(GraphicsContext gc, double minLat, double maxLat, double minLng, double maxLng) {
        if (cells == null || cells.isEmpty()) return;

        double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;

        double radRotationY = Math.toRadians(-centerLng);
        double radTilt = Math.toRadians(centerLat);

        // 1. Draw Ocean Base Sphere (Deep navy gradient body)
        RadialGradient oceanGrad = new RadialGradient(
                0, 0, cx - radius * 0.25, cy - radius * 0.25, radius * 1.25, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(16, 50, 95)),
                new Stop(0.65, Color.rgb(10, 30, 65)),
                new Stop(1.0, Color.rgb(4, 15, 40))
        );
        gc.setFill(oceanGrad);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // 2. Draw Atmosphere Glow Rings
        gc.setLineWidth(1.5);
        gc.setStroke(Color.rgb(56, 189, 248, 0.40));
        gc.strokeOval(cx - radius - 1, cy - radius - 1, (radius + 1) * 2, (radius + 1) * 2);

        gc.setLineWidth(4.0);
        gc.setStroke(Color.rgb(14, 165, 233, 0.22));
        gc.strokeOval(cx - radius - 4, cy - radius - 4, (radius + 4) * 2, (radius + 4) * 2);

        gc.setLineWidth(9.0);
        gc.setStroke(Color.rgb(56, 189, 248, 0.08));
        gc.strokeOval(cx - radius - 8, cy - radius - 8, (radius + 8) * 2, (radius + 8) * 2);

        // 3. Fast Zero-Allocation Buffer Check
        int cellCount = cells.size();
        if (polyZBuf.length < cellCount) {
            polyZBuf = new double[cellCount];
            polyPxBuf = new double[cellCount][6];
            polyPyBuf = new double[cellCount][6];
            polyColorBuf = new Color[cellCount];
            polyBorderBuf = new boolean[cellCount];
            polyIndexBuf = new Integer[cellCount];
        }

        double hexRadiusRad = Math.sqrt(4.0 * Math.PI / Math.max(1, cellCount)) * 0.58;
        double screenCellRadius = (radius / Math.sqrt(Math.max(1, cellCount))) * 0.95;

        // In continuous map mode, slightly expand hex tangent radius to ensure seamless coverage without background gaps
        double effectiveHexRad = smoothMap ? hexRadiusRad * 1.04 : hexRadiusRad;
        double tanHexRad = Math.tan(effectiveHexRad);

        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);

        double radCamLat = Math.toRadians(centerLat);
        double viewWidth = getWidth();
        double viewHeight = getHeight();

        int visibleCount = 0;

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = cells.get(i);
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();

            // Hierarchical Frustum / Backface Culling
            double radLat1 = Math.toRadians(lat);
            double radLngDiff = Math.toRadians(lng - centerLng);
            double dotProd = Math.sin(radLat1) * Math.sin(radCamLat) + Math.cos(radLat1) * Math.cos(radCamLat) * Math.cos(radLngDiff);
            if (dotProd < -0.05) continue; // Skip back hemisphere

            double latRad = Math.toRadians(lat);
            double lngRad = Math.toRadians(lng);
            double nx = Math.cos(latRad) * Math.sin(lngRad);
            double ny = Math.sin(latRad);
            double nz = Math.cos(latRad) * Math.cos(lngRad);

            double xr = nx * cosR + nz * sinR;
            double zr = -nx * sinR + nz * cosR;
            double yrt = ny * cosT - zr * sinT;
            double zrt = ny * sinT + zr * cosT;

            if (zrt <= 0.02) continue;

            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double elevationRatio = (elev / 6371000.0) * verticalExaggeration;
            double r = radius * (1.0 + elevationRatio);

            double cxCenter = cx + xr * r;
            double cyCenter = cy - yrt * r;

            // Viewport Screen Bounds Culling
            if (cxCenter < -40 || cxCenter > viewWidth + 40 || cyCenter < -40 || cyCenter > viewHeight + 40) {
                continue;
            }

            double[] px = polyPxBuf[visibleCount];
            double[] py = polyPyBuf[visibleCount];

            // Accurate 3D spherical local tangent frame (prevents fish-scale overlapping artifacts at low LOD)
            double ux, uy, uz;
            if (Math.abs(ny) < 0.99) {
                double uLen = Math.hypot(nz, nx);
                ux = -nz / uLen; uy = 0.0; uz = nx / uLen;
            } else {
                double uLen = Math.hypot(nz, ny);
                ux = 0.0; uy = -nz / uLen; uz = ny / uLen;
            }
            double vx = ny * uz - nz * uy;
            double vy = nz * ux - nx * uz;
            double vz = nx * uy - ny * ux;

            for (int k = 0; k < 6; k++) {
                double angleRad = (Math.PI / 3.0) * k - (Math.PI / 6.0);
                double cosA = Math.cos(angleRad);
                double sinA = Math.sin(angleRad);

                double dx = tanHexRad * (cosA * ux + sinA * vx);
                double dy = tanHexRad * (cosA * uy + sinA * vy);
                double dz = tanHexRad * (cosA * uz + sinA * vz);

                double vx3 = nx + dx;
                double vy3 = ny + dy;
                double vz3 = nz + dz;
                double vLen = Math.sqrt(vx3 * vx3 + vy3 * vy3 + vz3 * vz3);
                vx3 /= vLen; vy3 /= vLen; vz3 /= vLen;

                double vxr = vx3 * cosR + vz3 * sinR;
                double vzr = -vx3 * sinR + vz3 * cosR;
                double vyrt = vy3 * cosT - vzr * sinT;

                px[k] = cx + vxr * r;
                py[k] = cy - vyrt * r;
            }

            Color baseColor = smoothMap ? getIdwInterpolatedColor(cell, i) : getBufferOrCellColor(cell);
            double lightFactor = 0.45 + 0.55 * Math.max(0.0, zrt);
            if (showHillshading) {
                lightFactor *= computeHillshadeFactor(cell);
            }
            if (showSolarTerminator) {
                lightFactor *= computeSolarTerminatorFactor(cell);
            }
            Color shadedColor = Color.color(
                    Math.clamp(baseColor.getRed() * lightFactor, 0.0, 1.0),
                    Math.clamp(baseColor.getGreen() * lightFactor, 0.0, 1.0),
                    Math.clamp(baseColor.getBlue() * lightFactor, 0.0, 1.0),
                    baseColor.getOpacity()
            );

            polyZBuf[visibleCount] = zrt;
            polyColorBuf[visibleCount] = shadedColor;
            polyBorderBuf[visibleCount] = showHexGrid && (screenCellRadius > 1.4);
            polyIndexBuf[visibleCount] = visibleCount;

            visibleCount++;
        }

        final int count = visibleCount;
        Arrays.sort(polyIndexBuf, 0, count, (a, b) -> Double.compare(polyZBuf[a], polyZBuf[b]));

        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, radius, radius, 0, 360);
        gc.closePath();
        gc.clip();

        // Continuous mesh rendering with clean depth ordering and optional borders
        for (int idx = 0; idx < count; idx++) {
            int i = polyIndexBuf[idx];
            gc.setFill(polyColorBuf[i]);
            gc.fillPolygon(polyPxBuf[i], polyPyBuf[i], 6);

            if (polyBorderBuf[i]) {
                gc.setStroke(Color.rgb(0, 0, 0, 0.20));
                gc.setLineWidth(0.5);
                gc.strokePolygon(polyPxBuf[i], polyPyBuf[i], 6);
            }
        }

        gc.restore();

        // Soft limb shadow edge overlay
        RadialGradient limbGradient = new RadialGradient(
                0, 0, cx, cy, radius, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 0, 0.0)),
                new Stop(0.85, Color.rgb(0, 0, 0, 0.05)),
                new Stop(0.98, Color.rgb(0, 0, 0, 0.45)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.70))
        );
        gc.setFill(limbGradient);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    /**
     * Compute Inverse Distance Weighting (IDW) spatial continuous color interpolation.
     * Blends a cell's color with its immediate spatial neighbors to create a mathematically smooth scalar field.
     */
    private Color getIdwInterpolatedColor(H3Cell cell, int cellIndex) {
        Color baseCol = getBufferOrCellColor(cell);
        if (!smoothMap || cells == null || cells.isEmpty()) return baseCol;

        int neighborRange = 3;
        int minIdx = Math.max(0, cellIndex - neighborRange);
        int maxIdx = Math.min(cells.size() - 1, cellIndex + neighborRange);

        double totalWeight = 1.0;
        double rWeighted = baseCol.getRed();
        double gWeighted = baseCol.getGreen();
        double bWeighted = baseCol.getBlue();

        double cLat = cell.getLatitude();
        double cLng = cell.getLongitude();

        for (int j = minIdx; j <= maxIdx; j++) {
            if (j == cellIndex) continue;
            H3Cell neighbor = cells.get(j);
            double dLat = neighbor.getLatitude() - cLat;
            double dLng = neighbor.getLongitude() - cLng;
            double distSq = dLat * dLat + dLng * dLng;

            if (distSq < 25.0) {
                double w = 1.0 / (distSq + 0.15);
                Color nCol = getBufferOrCellColor(neighbor);
                rWeighted += nCol.getRed() * w;
                gWeighted += nCol.getGreen() * w;
                bWeighted += nCol.getBlue() * w;
                totalWeight += w;
            }
        }

        return Color.color(
                Math.clamp(rWeighted / totalWeight, 0.0, 1.0),
                Math.clamp(gWeighted / totalWeight, 0.0, 1.0),
                Math.clamp(bWeighted / totalWeight, 0.0, 1.0),
                baseCol.getOpacity()
        );
    }

    private double computeHillshadeFactor(H3Cell cell) {
        if (cell == null || !showHillshading) return 1.0;
        double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
        if (elev <= 0) return 1.0;

        double latRad = Math.toRadians(cell.getLatitude());
        double lngRad = Math.toRadians(cell.getLongitude());

        double slopeX = Math.sin(lngRad * 4.0) * (elev / 8848.0) * 0.40;
        double slopeY = Math.cos(latRad * 4.0) * (elev / 8848.0) * 0.40;
        double light = (-0.5 * slopeX + 0.5 * slopeY + 0.707) / Math.sqrt(slopeX * slopeX + slopeY * slopeY + 1.0);
        return Math.clamp(0.70 + light * 0.45, 0.55, 1.45);
    }

    private double computeSolarTerminatorFactor(H3Cell cell) {
        if (cell == null || !showSolarTerminator) return 1.0;
        double latRad = Math.toRadians(cell.getLatitude());
        double lngRad = Math.toRadians(cell.getLongitude());
        double solarLngRad = Math.toRadians(-centerLng);

        double cosZenith = Math.cos(latRad) * Math.cos(lngRad - solarLngRad);
        if (cosZenith < -0.08) return 0.35;
        if (cosZenith > 0.08) return 1.0;
        return 0.35 + (cosZenith + 0.08) / 0.16 * 0.65;
    }

    private void drawAgents(GraphicsContext gc) {
        if (agentManager == null || agentManager.getAgents() == null) return;

        double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        double radRotationY = Math.toRadians(-centerLng);
        double radTilt = Math.toRadians(centerLat);

        for (org.ether.society.agents.Agent agent : agentManager.getAgents()) {
            H3Cell cell = cellMap != null ? cellMap.get(agent.getH3Index()) : cells.stream()
                    .filter(c -> c.getH3Index() == agent.getH3Index())
                    .findFirst()
                    .orElse(null);

            if (cell == null)
                continue;

            double x, y;
            if (viewMode == ViewMode.VIEW_3D) {
                double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
                ProjectedPoint pp = project3D(cell.getLatitude(), cell.getLongitude(), elev, radRotationY, radTilt, radius, cx, cy);
                if (pp == null || pp.z <= 0.02) continue; // Behind sphere limb
                x = pp.screenX;
                y = pp.screenY;
            } else {
                x = (cell.getLongitude() - minLng) * scale + offsetX;
                y = (maxLat - cell.getLatitude()) * scale + offsetY;
            }

            // Draw Agent
            gc.setFill(getAgentColor(agent.getType()));
            gc.fillOval(x - 3, y - 3, 6, 6);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(x - 3, y - 3, 6, 6);
        }
    }

    private Color getAgentColor(org.ether.society.agents.AgentType type) {
        return switch (type) {
            case SCOUT -> Color.YELLOW;
            case TRADER -> Color.ORANGE;
            case SETTLER -> Color.MAGENTA;
        };
    }

    private Color getCellColor(H3Cell cell) {
        boolean isWater = cell != null && (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN || (cell.getElevation() != null && cell.getElevation() <= 0));
        return switch (displayMode) {
            case MALTHUSIAN_PRESSURE -> getMalthusianPressureColor(cell);
            case MINERAL_RESOURCES -> getMineralResourcesColor(cell);
            case MINING_EXPLOITATION -> getMiningExploitationColor(cell);
            case BIOME -> getBiomeColor(cell.getBiome());
            case POPULATION -> getPopulationColor(cell != null && cell.getPopulation() != null ? cell.getPopulation() : 0, isWater);
            case FOOD -> getFoodColor(cell.getFoodResource());
            case TEMPERATURE -> getTemperatureColor(cell.getTemperature());
            case TECHNOLOGY -> getTechColor(cell.getTechnologyLevel());
            case WATER -> getWaterColor(cell.getWaterResource());
            case WOOD -> getWoodColor(cell.getWoodResource());
            case INEQUALITY -> getGiniColor(cell.getGiniIndex());
            case CAPACITY -> getCapacityColor(cell);
            case MIGRATION -> getMigrationColor(cell);
            case FLUX -> getFluxPressureColor(cell);
            case CULTURE -> getCultureColor(cell);
            case POLITICAL -> getPoliticalColor(cell);
            case ASABIYYAH -> getAsabiyyahColor(cell);
            case AGE_PYRAMID -> getAgePyramidColor(cell);
            case ALBEDO -> getAlbedoColor(cell);
            case EPIDEMIC -> getEpidemicColor(cell);
            case FRICTION -> getFrictionColor(cell);
            case ENERGY_CAPACITY -> getEnergyCapacityColor(cell);
            case ENTROPY_POLLUTION -> getEntropyPollutionColor(cell);
            case SOIL_QUALITY -> getSoilQualityColor(cell);
            case BIODIVERSITY -> getBiodiversityColor(cell);
            case HEALTH_LIFE_EXPECTANCY -> getHealthLifeExpectancyColor(cell);
            case EDUCATION_LEVEL -> getEducationLevelColor(cell);
            case HAPPINESS -> getHappinessColor(cell);
            case CONFLICT -> getConflictColor(cell);
            case INSTITUTIONAL_MATURITY -> getInstitutionalMaturityColor(cell);
            case GDP_WEALTH -> getGdpWealthColor(cell);
            case ELITE_DENSITY -> getEliteDensityColor(cell);
            case COLLECTIVE_MEMORY -> getCollectiveMemoryColor(cell);
            case COLLAPSE_RISK -> getCollapseRiskColor(cell);
        };
    }

    private Color getMalthusianPressureColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 40, 90);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 45, 60);

        double capK = computeCarryingCapacity(cell);
        double ratio = capK > 0 ? (double) pop / capK : 2.0;

        if (ratio < 0.5) return Color.rgb(16, 185, 129); // Vert / Abondance (< 50%)
        else if (ratio < 1.0) return Color.rgb(234, 179, 8);  // Jaune / Équilibré (50%-100%)
        else if (ratio < 1.5) return Color.rgb(249, 115, 22); // Orange / Tension Malthusienne (100%-150%)
        else return Color.rgb(239, 68, 68);  // Rouge / Surpopulation Critique (> 150%)
    }

    private double computeCarryingCapacity(H3Cell c) {
        if (c == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        Biome b = c.getBiome();
        if (b == Biome.DESERT || b == Biome.TUNDRA || b == Biome.SNOW) baseCap *= 0.1;
        else if (b == Biome.PLAINS || b == Biome.FOREST) baseCap *= 1.5;
        else if (b == Biome.JUNGLE) baseCap *= 0.8;

        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }
        double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
        baseCap *= Math.max(0.5, tech * 0.8);
        return Math.max(10.0, baseCap);
    }

    private Color getMineralResourcesColor(H3Cell cell) {
        if (cell == null || cell.getElevation() <= 0) return Color.rgb(15, 30, 70);
        double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
        double precious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
        double clay = cell.getResourceClay() != null ? cell.getResourceClay() : 0.0;
        double totalMinerals = metal + precious * 3.0 + clay * 0.5;

        if (totalMinerals <= 10.0) return Color.rgb(45, 55, 72); // Slate / Unmineralized

        double norm = Math.min(1.0, totalMinerals / 600.0);
        if (precious > 50.0) {
            // Gold / Cyan spark for precious deposits
            return Color.rgb((int)(220 + norm * 35), (int)(180 + norm * 70), (int)(20 + norm * 80));
        }
        // Copper / Bronze / Iron metallic gradient (Slate -> Bronze -> Bright Cyan-Copper)
        int r = (int) (60 + norm * 180);
        int g = (int) (120 + norm * 110);
        int b = (int) (160 - norm * 100);
        return Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
    }

    private Color getMiningExploitationColor(H3Cell cell) {
        if (cell == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
        double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

        // Active mining requires both available mineral deposits and invested capital/tools
        double miningActivity = Math.min(capital, metal * 0.8 + 10.0);
        if (miningActivity <= 2.0) return Color.rgb(35, 45, 60); // Inactive

        double norm = Math.min(1.0, miningActivity / 200.0);
        // Mining activity: Dark Bronze -> Bright Amber -> Glowing Crimson/Gold
        int r = (int) (120 + norm * 135);
        int g = (int) (70 + norm * 120);
        int b = (int) (20 + norm * 40);
        return Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(b, 0, 255));
    }

    private Color getAsabiyyahColor(H3Cell cell) {
        if (cell.getOwner() == null) return Color.rgb(40, 40, 50);
        double asabiyyah = cell.getOwner().getAsabiyyah();
        return Color.color(1.0 - asabiyyah, asabiyyah, 0.2); // Green (high cohesion) to Red (instability)
    }

    private Color getAgePyramidColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 30, 40);
        double seniorRatio = (double) (cell.getPop65to79() + cell.getPop80Plus()) / pop;
        return Color.color(seniorRatio, 0.4, 1.0 - seniorRatio); // Blue (young) to Magenta/Purple (aging)
    }

    private Color getAlbedoColor(H3Cell cell) {
        double albedo = cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
        return Color.gray(Math.clamp(albedo, 0.05, 0.95)); // Grayscale reflectance
    }

    private Color getEpidemicColor(H3Cell cell) {
        int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
        if (infected == 0) return getBiomeColor(cell.getBiome()).desaturate();
        double norm = Math.min(1.0, infected / 200.0);
        return Color.rgb(255, (int) ((1 - norm) * 100), (int) ((1 - norm) * 100)); // Vivid red outbreak
    }

    private Color getFrictionColor(H3Cell cell) {
        double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
        double norm = Math.min(1.0, (friction - 0.5) / 8.0);
        return Color.color(norm, 1.0 - norm, 0.1); // Green (easy plain) to Red (impassable mountain)
    }

    private Color getEnergyCapacityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 55);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double norm = Math.min(1.0, tech / 10.0);
        return Color.color(0.1 + norm * 0.9, 0.4 + norm * 0.5, 0.2); // Solar Yellow/Green gradient
    }

    private Color getEntropyPollutionColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(10, 20, 40);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double pollution = (pop * 0.05 + tech * 1.5) / 500.0;
        double norm = Math.min(1.0, pollution);
        return Color.color(norm, 0.2, 0.8 * (1.0 - norm)); // Purple to Toxic Lime Green
    }

    private Color getSoilQualityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 30, 60);
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 100.0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 100.0;
        double npk = Math.min(1.0, (wood + food) / 1200.0);
        return Color.color(0.4 * (1.0 - npk), 0.2 + npk * 0.7, 0.1); // Arid Brown to Rich Soil Green
    }

    private Color getBiodiversityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 45, 80);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 100.0;
        double pristine = Math.max(0.0, Math.min(1.0, (wood / 800.0) - (pop / 1000.0)));
        return Color.color(0.1, 0.3 + pristine * 0.6, 0.2 + pristine * 0.3); // Pristine Deep Forest Green
    }

    private Color getHealthLifeExpectancyColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 40, 50);
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double life = Math.min(1.0, (food / Math.max(1, pop)) / 5.0);
        return Color.color(1.0 - life, life * 0.8 + 0.2, 0.4); // Red (famine/short life) to Vivid Cyan/Green
    }

    private Color getEducationLevelColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 20, 40);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double edu = Math.min(1.0, tech / 8.0);
        return Color.color(0.2 + edu * 0.6, 0.3, 0.5 + edu * 0.5); // Indigo to Bright Violet
    }

    private Color getHappinessColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(20, 30, 50);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
        double happy = Math.max(0.0, 1.0 - gini * 1.5);
        return Color.color(1.0 - happy, happy, 0.2); // Red (unhappy) to Gold/Green (happy)
    }

    private Color getConflictColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
        double conflict = Math.min(1.0, gini * 2.0);
        return Color.color(conflict, 0.8 * (1.0 - conflict), 0.1); // Green (peace) to Crimson Red (war)
    }

    private Color getInstitutionalMaturityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 20, 40);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double mat = Math.min(1.0, tech / 10.0);
        return Color.color(0.4 + mat * 0.5, 0.2, 0.6 + mat * 0.4); // Dark Blue to Royal Purple
    }

    private Color getGdpWealthColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double gdp = Math.min(1.0, (pop * tech) / 2500.0);
        return Color.color(gdp, gdp * 0.8, 0.2 * (1.0 - gdp)); // Slate to Glowing Gold
    }

    private Color getEliteDensityColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.2;
        double elite = Math.min(1.0, gini * 1.8);
        return Color.color(0.2 + elite * 0.8, 0.2, 0.5 + elite * 0.5); // Slate to Imperial Purple/Gold
    }

    private Color getCollectiveMemoryColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        double mem = Math.min(1.0, tech / 10.0);
        return Color.color(0.1, 0.5 + mem * 0.5, 0.8 + mem * 0.2); // Slate to Vivid Cyan/White
    }

    private Color getCollapseRiskColor(H3Cell cell) {
        if (cell == null || cell.getElevation() == null || cell.getElevation() <= 0) return Color.rgb(15, 25, 45);
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double capK = computeCarryingCapacity(cell);
        double ratio = capK > 0 ? (double) pop / capK : 1.0;
        double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.2;
        double risk = Math.min(1.0, Math.max(0.0, (ratio - 0.8) * 2.0 + gini));
        return Color.color(risk, 1.0 - risk, 0.1); // Green (safe) to Crimson (collapse risk)
    }

    private Color getBufferOrCellColor(H3Cell cell) {
        if (cell == null) return Color.BLACK;
        if (worldBuffer != null && !h3ToBufferIndexMap.isEmpty()) {
            Integer idx = h3ToBufferIndexMap.get(cell.getH3Index());
            if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
                int index = idx;
                Biome cellBiome = org.ether.society.model.Biome.values()[worldBuffer.getBiomes()[index]];
                boolean isWater = (cellBiome == Biome.OCEAN || cellBiome == Biome.DEEP_OCEAN || worldBuffer.getElevation()[index] <= 0);

                return switch (displayMode) {
                    case BIOME -> getBiomeColor(cellBiome);
                    case POPULATION -> getPopulationColor((int)worldBuffer.getBiomassHuman()[index], isWater);
                    case FOOD -> isWater ? Color.rgb(10, 20, 50) : getFoodColor(worldBuffer.getFoodResource()[index]);
                    case TEMPERATURE -> getTemperatureColor(worldBuffer.getTemperature()[index]);
                    case TECHNOLOGY -> isWater ? Color.rgb(10, 20, 50) : getTechColor((double)worldBuffer.getTechnologyLevel()[index]);
                    case WATER -> getWaterColor((double)worldBuffer.getWaterResource()[index]);
                    case WOOD -> isWater ? Color.rgb(10, 20, 50) : getWoodColor((double)worldBuffer.getWoodResource()[index]);
                    case INEQUALITY -> isWater ? Color.rgb(10, 20, 50) : getGiniColor((double)worldBuffer.getGiniIndex()[index]);
                    case FLUX -> isWater ? Color.rgb(10, 20, 50) : getPriceColor(worldBuffer.getLocalPrice()[index]);
                    default -> getCellColor(cell);
                };
            }
        }
        return getCellColor(cell);
    }

    private Color getPriceColor(float price) {
        // Higher price = more red
        float normalized = Math.min(1.0f, price / 10.0f);
        return Color.color(normalized, 1.0f - normalized, 0);
    }

    private Color getPoliticalColor(H3Cell cell) {
        if (cell.getOwner() == null) {
            // Unclaimed territory - just biome but dimmer or specific color
            return getBiomeColor(cell.getBiome()).desaturate().darker();
            // Or Color.GRAY for neutral? Let's use darker biome to show it's 'wild'.
        }
        return cell.getOwner().getColor();
    }

    private Color getBiomeColor(Biome biome) {
        if (biome == null)
            return Color.GRAY;

        return switch (biome) {
            case OCEAN -> Color.rgb(25, 50, 150);
            case DEEP_OCEAN -> Color.rgb(15, 30, 100);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(237, 201, 175);
            case PLAINS -> Color.rgb(124, 252, 0);
            case SAVANNAH -> Color.rgb(180, 200, 70);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 0);
            case MOUNTAINS -> Color.rgb(139, 137, 137);
            case HILLS -> Color.rgb(160, 160, 120);
            case TUNDRA -> Color.rgb(221, 221, 187);
            case SNOW -> Color.rgb(255, 250, 250);
            case GLACIER -> Color.rgb(220, 240, 255);
            case LAKE -> Color.rgb(25, 50, 150);
        };
    }

    /**
     * Get population density color (blue -> cyan -> green -> yellow -> red).
     */
    private Color getPopulationColor(int population) {
        return getPopulationColor(population, false);
    }

    private Color getPopulationColor(int population, boolean isWater) {
        if (isWater) {
            return Color.rgb(12, 24, 50); // Dark ocean blue for ocean/water cells
        }
        if (population <= 0) {
            return Color.rgb(25, 35, 45); // Dark land gray for unpopulated land
        }

        // Log scale for better visualization (most populations < 100)
        double normalized = Math.log1p(population) / Math.log1p(500); // 500 as max expected
        normalized = Math.min(1.0, Math.max(0.0, normalized));

        // Vibrant Heat map: Deep Blue -> Cyan -> Green -> Yellow -> Bright Red
        if (normalized < 0.25) {
            double t = normalized / 0.25;
            return Color.rgb(0, (int) (120 + t * 135), 255); // Deep Blue to Cyan
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, 255, (int) (255 * (1 - t))); // Cyan to Bright Green
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, 0); // Green to Yellow
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0); // Yellow to Red
        }
    }

    /**
     * Get food resource color (brown -> yellow -> green).
     */
    private Color getFoodColor(double food) {
        if (food < 10) {
            return Color.rgb(60, 40, 20); // Dark brown for no food
        }

        double normalized = Math.min(1.0, food / 800.0); // 800 as high food level

        if (normalized < 0.5) {
            double t = normalized / 0.5;
            return Color.rgb((int) (139 - t * 100), (int) (69 + t * 186), 19); // Brown to Yellow-green
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (39 * (1 - t)), (int) (139 + t * 116), (int) (34 * (1 - t))); // Yellow-green to
                                                                                                 // Bright green
        }
    }

    /**
     * Get temperature color (cool blue -> warm red).
     */
    private Color getTemperatureColor(double temp) {
        // Normalize -30 to +45Â°C to 0-1 range
        double normalized = (temp + 30) / 75.0;
        normalized = Math.max(0, Math.min(1.0, normalized));

        if (normalized < 0.25) {
            return Color.rgb(0, 0, (int) (128 + normalized * 4 * 127)); // Dark blue to bright blue
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, (int) (t * 255), 255); // Blue to Cyan
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, (int) (255 * (1 - t))); // Cyan to Yellow
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0); // Yellow to Red
        }
    }

    /**
     * Get technology level color (dark -> purple -> bright).
     */
    private Color getTechColor(Double techLevel) {
        if (techLevel == null || techLevel <= 0) {
            return Color.rgb(30, 30, 30); // Dark for no tech
        }

        // Tech levels 0-10 mapped to color gradient
        double normalized = Math.min(1.0, techLevel / 10.0);

        // Dark purple -> bright cyan progression
        int r = (int) (80 + normalized * 100);
        int g = (int) (20 + normalized * 235);
        int b = (int) (150 + normalized * 105);

        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Get water resource color (brown -> blue).
     */
    private Color getWaterColor(Double water) {
        if (water == null || water < 10) {
            return Color.rgb(139, 90, 43); // Dry brown
        }

        double normalized = Math.min(1.0, water / 1000.0);

        // Brown -> Light blue -> Deep blue
        int r = (int) (139 - normalized * 119);
        int g = (int) (90 + normalized * 100);
        int b = (int) (43 + normalized * 172);

        return Color.rgb(Math.max(0, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Get wood resource color (light -> dark green).
     */
    private Color getWoodColor(Double wood) {
        if (wood == null || wood < 10) {
            return Color.rgb(100, 80, 60); // Barren brown
        }

        double normalized = Math.min(1.0, wood / 1000.0);

        // Light brown -> Green -> Dark green
        int r = (int) (100 - normalized * 80);
        int g = (int) (80 + normalized * 100);
        int b = (int) (60 - normalized * 40);

        return Color.rgb(Math.max(0, r), Math.min(180, g), Math.max(0, b));
    }

    /**
     * Get Gini inequality color (green=equal, red=unequal).
     */
    private Color getGiniColor(Double gini) {
        if (gini == null || gini < 0.01) {
            return Color.rgb(50, 50, 50); // No data
        }

        // Gini 0 (perfect equality) = green, Gini 1 (maximum inequality) = red
        double normalized = Math.min(1.0, gini);

        int r = (int) (normalized * 255);
        int g = (int) ((1 - normalized) * 200);
        int b = 50;

        return Color.rgb(r, g, b);
    }

    /**
     * Get carrying capacity color.
     */
    private Color getCapacityColor(H3Cell cell) {
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;

        // Capacity based on food and water
        double capacity = (food / 100.0) * (0.3 + Math.min(1.0, water / 1000.0) * 0.7);
        double normalized = Math.min(1.0, capacity / 10.0);

        if (normalized < 0.2) {
            return Color.rgb(80, 40, 40); // Low capacity - dark red
        } else if (normalized < 0.5) {
            double t = (normalized - 0.2) / 0.3;
            return Color.rgb(80 + (int) (t * 100), 40 + (int) (t * 80), 40); // Orange
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (180 - t * 130), (int) (120 + t * 100), (int) (40 + t * 40)); // Yellow to Green
        }
    }

    /**
     * Get migration pressure color.
     */
    private Color getMigrationColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;

        if (pop == 0) {
            return Color.rgb(30, 30, 30); // No population
        }

        // Migration pressure: high population, low food = outbound (red)
        // Low population, high food = inbound attraction (blue)
        double foodPerCapita = food / Math.max(1, pop);
        double pressure = 1.0 - Math.min(1.0, foodPerCapita / 10.0);

        if (pressure > 0.7) {
            // High outbound pressure - red
            return Color.rgb(200, 50, 50);
        } else if (pressure > 0.4) {
            // Moderate - yellow
            return Color.rgb(200, 200, 50);
        } else if (pressure > 0.2) {
            // Low - neutral
            return Color.rgb(100, 150, 100);
        } else {
            // Attractive - blue (people want to come here)
            return Color.rgb(50, 100, 200);

        }
    }

    /**
     * Get flux pressure color (Blue=Supply, Red=Demand).
     */
    private Color getFluxPressureColor(H3Cell cell) {
        // We assume pressure is roughly -1 to 1.
        // But we don't have access to pressure directly unless we query FluxEngine.
        // Actually, FluxEngine stores it in a map. We didn't expose a getter for
        // pressureMap.
        // Workaround: Calculate it on the fly or just visualize Food/Pop ratio as
        // proxy.
        // Real implementation should expose pressure.
        // For now, visualize Food / Consumption ratio.

        double food = cell.getFoodResource();
        double consumption = cell.getPopulation() * 2.0; // approx

        if (consumption == 0)
            return Color.GREEN; // Surplus potential

        double ratio = food / consumption;
        // Ratio > 1.0 = Surplus (Blue/Green)
        // Ratio < 1.0 = Deficit (Red)

        if (ratio >= 2.0)
            return Color.BLUE; // High surplus
        if (ratio >= 1.0) {
            // 1.0 to 2.0 blends Green to Blue
            double t = Math.min(1.0, ratio - 1.0);
            return Color.rgb(0, (int) ((1 - t) * 255), (int) (t * 255));
        }

        // Deficit: 0.0 to 1.0 blends Red to Yellow/Green
        // 0.0 = Starvation (Red)
        // 1.0 = Subsistence (Green)
        double t = Math.max(0, ratio);
        return Color.rgb((int) ((1 - t) * 255), (int) (t * 255), 0);
    }

    /**
     * Get cultural identity color (RGB vector).
     */
    private Color getCultureColor(H3Cell cell) {
        if (cultureEngine == null)
            return Color.GRAY;

        CultureVector vec = cultureEngine.getCulture(cell.getH3Index());
        if (vec == null)
            return Color.GRAY;

        return Color.color(vec.getRed(), vec.getGreen(), vec.getBlue());
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public double getCenterLat() {
        return centerLat;
    }

    public double getCenterLng() {
        return centerLng;
    }

    /**
     * Set the mini-map reference for synchronization.
     */
    public void setMiniMap(MiniMap miniMap) {
        this.miniMap = miniMap;
    }

    /**
     * Set the center view position (called by mini-map click).
     */
    public void setCenterView(double lat, double lng) {
        this.centerLat = lat;
        this.centerLng = lng;

        draw();

        // Update mini-map
        if (miniMap != null) {
            miniMap.updateViewport(zoomFactor, centerLat, centerLng);
        }

        logger.debug("Center view set to: {}, {}", lat, lng);
    }

    /**
     * Notify mini-map of viewport changes.
     */
    private void notifyMiniMap() {
        if (miniMap != null) {
            miniMap.updateViewport(zoomFactor, centerLat, centerLng);
        }
    }
    /**
     * Draw subtle coordinate overlay badge (Center Lat/Lng, Bounds, Zoom) on canvas.
     */
    private void drawCoordinateOverlay(GraphicsContext gc) {
        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (getHeight() - offsetY) / scale;
        double viewLeftLng = minLng + (0 - offsetX) / scale;
        double viewRightLng = minLng + (getWidth() - offsetX) / scale;

        double cTopLat = Math.min(90.0, Math.max(-90.0, viewTopLat));
        double cBotLat = Math.min(90.0, Math.max(-90.0, viewBottomLat));
        double cLeftLng = Math.min(180.0, Math.max(-180.0, viewLeftLng));
        double cRightLng = Math.min(180.0, Math.max(-180.0, viewRightLng));

        String coordsText = String.format(
            "📍 Centre: %.2f°N, %.2f°E | Bornes: [%.2f°N, %.2f°E] → [%.2f°N, %.2f°E] | Zoom: %.1fx",
            centerLat, centerLng, cTopLat, cLeftLng, cBotLat, cRightLng, zoomFactor
        );

        gc.setFont(javafx.scene.text.Font.font("Consolas", 11));
        gc.setFill(Color.rgb(15, 23, 42, 0.8));
        gc.fillRoundRect(12, 12, 540, 24, 8, 8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.4));
        gc.setLineWidth(1);
        gc.strokeRoundRect(12, 12, 540, 24, 8, 8);

        gc.setFill(Color.rgb(226, 232, 240));
        gc.fillText(coordsText, 20, 28);
    }

    private void drawContours(GraphicsContext gc) {
        if (!showContours || cells == null || cellMap == null || zoomFactor < 0.85) return;
        
        gc.setStroke(Color.rgb(56, 189, 248, 0.75));
        gc.setLineWidth(Math.max(1.0, 1.2 * Math.sqrt(zoomFactor)));
        
        double maxElev = 0;
        for (H3Cell c : cells) {
            double e = c.getElevation() != null ? c.getElevation() : 0;
            if (e > maxElev) maxElev = e;
        }
        double adaptiveZoomScale = Math.max(0.5, zoomFactor);
        double step = (maxElev > 100) ? Math.min(1000.0, (maxElev / 6.0) / adaptiveZoomScale) : (50.0 / adaptiveZoomScale);
        if (step < 20) step = 20;
        
        // Optimize: Iterate only visible cells if possible, but O(N) is fast enough for N<100k
        for (H3Cell cell : cells) {
            // Culling for 2D
             if (viewMode == ViewMode.VIEW_2D) {
                 double lat = cell.getLatitude();
                 double top = maxLat - (0 - offsetY) / scale;
                 double bottom = maxLat - (getHeight() - offsetY) / scale;
                 if (lat > top + 10 || lat < bottom - 10) continue;
            }

            double elev = cell.getElevation() != null ? cell.getElevation() : 0;
            int level = (int) (elev / step);
            
            try {
                // We need to check all neighbors to find boundaries
                List<Long> neighbors = h3Service.getNeighbors(cell.getH3Index());
                for (Long nIdx : neighbors) {
                    H3Cell neighbor = cellMap.get(nIdx);
                    if (neighbor == null) continue; // Ignore map boundary edge cells to prevent parallel lines across latitudes
                    
                    double nElev = neighbor.getElevation() != null ? neighbor.getElevation() : 0;
                    int nLevel = (int) (nElev / step);
                    
                    if (level > nLevel) {
                        long edge = h3Service.getDirectedEdge(cell.getH3Index(), nIdx);
                        List<LatLng> boundary = h3Service.getEdgeBoundary(edge);
                        
                        if (boundary != null && boundary.size() >= 2) {
                            if (viewMode == ViewMode.VIEW_2D) {
                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);
                                if (Math.abs(p1.lng - p2.lng) > 180.0) continue; // Prevent antimeridian wrap streak lines
                                
                                double x1 = (p1.lng - minLng) * scale + offsetX;
                                double y1 = (maxLat - p1.lat) * scale + offsetY;
                                double x2 = (p2.lng - minLng) * scale + offsetX;
                                double y2 = (maxLat - p2.lat) * scale + offsetY;
                                
                                gc.strokeLine(x1, y1, x2, y2);
                            } else if (viewMode == ViewMode.VIEW_3D) {
                                double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
                                double cx = getWidth() / 2;
                                double cy = getHeight() / 2;
                                double radRotationY = Math.toRadians(-centerLng);
                                double radTilt = Math.toRadians(centerLat);

                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);

                                ProjectedPoint pp1 = project3D(p1.lat, p1.lng, level * step, radRotationY, radTilt, radius, cx, cy);
                                ProjectedPoint pp2 = project3D(p2.lat, p2.lng, level * step, radRotationY, radTilt, radius, cx, cy);

                                if (pp1 != null && pp2 != null) {
                                    gc.strokeLine(pp1.screenX, pp1.screenY, pp2.screenX, pp2.screenY);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore H3 errors
            }
        }
    }

    private void drawFlowVectors(GraphicsContext gc) {
        if (!showFlowVectors || cells == null || cellMap == null || zoomFactor < 0.3) return;

        gc.setLineWidth(1.5);
        gc.setStroke(Color.rgb(251, 146, 60, 0.85)); // Vibrant neon amber/orange for vector field
        gc.setFill(Color.rgb(251, 146, 60, 0.90));

        double arrowLength = Math.max(8.0, 14.0 * zoomFactor);

        for (H3Cell cell : cells) {
            double flux = cell.getFluxPressure();
            if (flux <= 0.001) continue;

            // Compute vector direction toward highest flux gradient neighbor
            List<Long> neighbors = h3Service != null ? h3Service.getNeighbors(cell.getH3Index()) : null;
            if (neighbors == null || neighbors.isEmpty()) continue;

            double maxNeighborFlux = flux;
            H3Cell targetNeighbor = null;

            for (Long nIdx : neighbors) {
                H3Cell neighbor = cellMap.get(nIdx);
                if (neighbor != null && neighbor.getFluxPressure() > maxNeighborFlux) {
                    maxNeighborFlux = neighbor.getFluxPressure();
                    targetNeighbor = neighbor;
                }
            }

            double angle;
            if (targetNeighbor != null) {
                double dLat = targetNeighbor.getLatitude() - cell.getLatitude();
                double dLng = targetNeighbor.getLongitude() - cell.getLongitude();
                angle = Math.atan2(-dLat, dLng);
            } else {
                angle = Math.toRadians((cell.getH3Index() % 360));
            }

            if (viewMode == ViewMode.VIEW_2D) {
                double lat = cell.getLatitude();
                double lng = cell.getLongitude();

                double cx = (lng - minLng) * scale + offsetX;
                double cy = (maxLat - lat) * scale + offsetY;

                if (cx < -20 || cx > getWidth() + 20 || cy < -20 || cy > getHeight() + 20) continue;

                double endX = cx + arrowLength * Math.cos(angle);
                double endY = cy + arrowLength * Math.sin(angle);

                gc.strokeLine(cx, cy, endX, endY);
                double headAngle1 = angle + Math.toRadians(150);
                double headAngle2 = angle - Math.toRadians(150);
                double headLen = arrowLength * 0.35;
                gc.strokeLine(endX, endY, endX + headLen * Math.cos(headAngle1), endY + headLen * Math.sin(headAngle1));
                gc.strokeLine(endX, endY, endX + headLen * Math.cos(headAngle2), endY + headLen * Math.sin(headAngle2));
            } else if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
                double screenCx = getWidth() / 2.0;
                double screenCy = getHeight() / 2.0;
                double radRotationY = Math.toRadians(-centerLng);
                double radTilt = Math.toRadians(centerLat);

                ProjectedPoint pt = project3D(cell.getLatitude(), cell.getLongitude(), cell.getElevation() != null ? cell.getElevation() : 0, radRotationY, radTilt, radius, screenCx, screenCy);
                if (pt != null) {
                    double endX = pt.screenX + arrowLength * Math.cos(angle);
                    double endY = pt.screenY + arrowLength * Math.sin(angle);
                    gc.strokeLine(pt.screenX, pt.screenY, endX, endY);
                }
            }
        }
    }

    private void drawResourceOverlay(GraphicsContext gc) {
        if (!showResourceOverlay || cells == null) return;

        double markerSize = Math.max(3.0, 6.0 * zoomFactor);

        for (H3Cell cell : cells) {
            double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0;
            double precious = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0;
            double aquifer = cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer() : 0;

            if (metal <= 0 && precious <= 0 && capital <= 0 && aquifer <= 0) continue;

            double cx = 0, cy = 0;
            boolean visible = false;

            if (viewMode == ViewMode.VIEW_2D) {
                double lat = cell.getLatitude();
                double lng = cell.getLongitude();
                cx = (lng - minLng) * scale + offsetX;
                cy = (maxLat - lat) * scale + offsetY;
                if (cx >= -20 && cx <= getWidth() + 20 && cy >= -20 && cy <= getHeight() + 20) {
                    visible = true;
                }
            } else if (viewMode == ViewMode.VIEW_3D) {
                double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
                double screenCx = getWidth() / 2.0;
                double screenCy = getHeight() / 2.0;
                double radRotationY = Math.toRadians(-centerLng);
                double radTilt = Math.toRadians(centerLat);

                ProjectedPoint pt = project3D(cell.getLatitude(), cell.getLongitude(), cell.getElevation() != null ? cell.getElevation() : 0, radRotationY, radTilt, radius, screenCx, screenCy);
                if (pt != null) {
                    cx = pt.screenX;
                    cy = pt.screenY;
                    visible = true;
                }
            }

            if (!visible) continue;

            if (precious > 0 || metal > 20) {
                // Gold / Metal glyph (diamond marker)
                gc.setFill(precious > 0 ? Color.rgb(234, 179, 8, 0.85) : Color.rgb(148, 163, 184, 0.85));
                double[] xPoints = {cx, cx + markerSize, cx, cx - markerSize};
                double[] yPoints = {cy - markerSize, cy, cy + markerSize, cy};
                gc.fillPolygon(xPoints, yPoints, 4);
            } else if (aquifer > 50) {
                // Aquifer glyph (cyan dot)
                gc.setFill(Color.rgb(6, 182, 212, 0.80));
                gc.fillOval(cx - markerSize * 0.7, cy - markerSize * 0.7, markerSize * 1.4, markerSize * 1.4);
            } else if (capital > 50) {
                // Capital / Infrastructure glyph (purple square)
                gc.setFill(Color.rgb(168, 85, 247, 0.80));
                gc.fillRect(cx - markerSize * 0.6, cy - markerSize * 0.6, markerSize * 1.2, markerSize * 1.2);
            }
        }
    }

    private static class ProjectedPoint {
        double screenX, screenY, z;
    }

    private ProjectedPoint project3D(double lat, double lng, double elevation, double radRotationY, double radTilt, double radius, double cx, double cy) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        // Spherical to Cartesian
        double x = Math.cos(latRad) * Math.sin(lngRad);
        double y = Math.sin(latRad);
        double z = Math.cos(latRad) * Math.cos(lngRad);

        // Apply Y-axis rotation (Longitude)
        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double xr = x * cosR + z * sinR;
        double zr = -x * sinR + z * cosR;

        // Apply X-axis tilt (Latitude)
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);
        double yrt = y * cosT - zr * sinT;
        double zrt = y * sinT + zr * cosT;

        // Backface culling
        if (zrt <= 0.02) return null;

        // Screen projection with heightmap 3D vertical exaggeration factor (default 25x)
        double elevationRatio = (elevation / 6371000.0) * verticalExaggeration;
        double r = radius * (1.0 + elevationRatio);

        ProjectedPoint p = new ProjectedPoint();
        p.screenX = cx + xr * r;
        p.screenY = cy - yrt * r;
        p.z = zrt;
        return p;
    }

    private void drawLegendOverlay(GraphicsContext gc) {
        if (cells == null || cells.isEmpty()) return;

        double w = getWidth();
        double h = getHeight();

        double legendWidth = 320.0;
        double legendHeight = 112.0;
        double marginRight = 30.0;
        double marginBottom = 35.0;
        double lx = Math.max(10.0, w - legendWidth - marginRight);
        double ly = Math.max(10.0, h - legendHeight - marginBottom);

        // Background box with glassmorphism styling
        gc.setFill(Color.rgb(15, 23, 42, 0.94));
        gc.fillRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);
        gc.setStroke(Color.rgb(56, 189, 248, 0.50));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);

        // Header label
        gc.setFill(Color.rgb(56, 189, 248));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 11));
        gc.fillText(org.ether.society.i18n.I18n.getOrDefault("sim.legend.title", "LÉGENDE — ") + displayMode.getDisplayName().toUpperCase(), lx + 12, ly + 18);

        // Compute stats (Min, Max, Mean, Median)
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        double sum = 0.0;
        double[] values = new double[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            double v = getCellDisplayValue(cells.get(i), i);
            values[i] = v;
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
            sum += v;
        }

        if (minVal == Double.MAX_VALUE) minVal = 0.0;
        if (maxVal == -Double.MAX_VALUE) maxVal = 1.0;
        double meanVal = sum / cells.size();

        java.util.Arrays.sort(values);
        double medianVal = (values.length % 2 == 0)
                ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                : values[values.length / 2];

        double range = Math.max(1e-6, maxVal - minVal);
        double barX = lx + 12;
        double barWidth = legendWidth - 24;

        // --- Compact Distribution Histogram (20 Bins) ---
        int numBins = 20;
        int[] binCounts = new int[numBins];
        int maxBinCount = 1;
        for (double v : values) {
            int binIdx = (int) Math.clamp(((v - minVal) / range) * numBins, 0, numBins - 1);
            binCounts[binIdx]++;
            if (binCounts[binIdx] > maxBinCount) {
                maxBinCount = binCounts[binIdx];
            }
        }

        double histoY = ly + 46;
        double histoMaxH = 18.0;
        double binW = barWidth / numBins;

        gc.setFill(Color.rgb(56, 189, 248, 0.45));
        for (int b = 0; b < numBins; b++) {
            if (binCounts[b] == 0) continue;
            double bh = ((double) binCounts[b] / maxBinCount) * histoMaxH;
            gc.fillRect(barX + b * binW, histoY - bh, binW - 0.8, bh);
        }

        // Spectrum color bar
        double barY = ly + 48;
        double barHeight = 14;

        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(
                barX, barY, barX + barWidth, barY, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, Color.rgb(30, 58, 138)),
                new javafx.scene.paint.Stop(0.25, Color.rgb(6, 182, 212)),
                new javafx.scene.paint.Stop(0.50, Color.rgb(34, 197, 94)),
                new javafx.scene.paint.Stop(0.75, Color.rgb(234, 179, 8)),
                new javafx.scene.paint.Stop(1.0, Color.rgb(239, 68, 68))
        );

        gc.setFill(grad);
        gc.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.4));
        gc.strokeRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        // Mean Indicator (Cyan triangle pointing down above bar)
        double meanNorm = Math.clamp((meanVal - minVal) / range, 0.0, 1.0);
        double meanX = barX + meanNorm * barWidth;
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillPolygon(new double[]{meanX - 4, meanX + 4, meanX}, new double[]{barY - 5, barY - 5, barY}, 3);

        // Median Indicator (Amber triangle pointing up below bar)
        double medNorm = Math.clamp((medianVal - minVal) / range, 0.0, 1.0);
        double medX = barX + medNorm * barWidth;
        gc.setFill(Color.rgb(245, 158, 11));
        gc.fillPolygon(new double[]{medX - 4, medX + 4, medX}, new double[]{barY + barHeight + 5, barY + barHeight + 5, barY + barHeight}, 3);

        // Explicit Statistics Line (Min, Mean, Median, Max)
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9.5));
        
        // Min Label (Gray)
        gc.setFill(Color.rgb(148, 163, 184));
        gc.fillText(String.format("Min: %.1f", minVal), barX, ly + 88);

        // Mean Label (Cyan μ)
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillText(String.format("μ: %.1f", meanVal), barX + barWidth * 0.28, ly + 88);

        // Median Label (Amber x̃)
        gc.setFill(Color.rgb(245, 158, 11));
        gc.fillText(String.format("x̃: %.1f", medianVal), barX + barWidth * 0.54, ly + 88);

        // Max Label (Red)
        gc.setFill(Color.rgb(248, 113, 113));
        String maxStr = String.format("Max: %.1f", maxVal);
        gc.fillText(maxStr, barX + barWidth - (maxStr.length() * 5.8), ly + 88);
    }

    public double getCellDisplayValue(H3Cell cell, int unusedIndex) {
        if (worldBuffer != null && cell != null && !h3ToBufferIndexMap.isEmpty()) {
            Integer idx = h3ToBufferIndexMap.get(cell.getH3Index());
            if (idx != null && idx >= 0 && idx < worldBuffer.getCapacity()) {
                int index = idx;
                return switch (displayMode) {
                    case POPULATION -> worldBuffer.getBiomassHuman()[index];
                    case FOOD -> worldBuffer.getFoodResource()[index];
                    case TEMPERATURE -> worldBuffer.getTemperature()[index];
                    case TECHNOLOGY -> worldBuffer.getTechnologyLevel()[index];
                    case WATER -> worldBuffer.getWaterResource()[index];
                    case WOOD -> worldBuffer.getWoodResource()[index];
                    case INEQUALITY -> worldBuffer.getGiniIndex()[index];
                    case FLUX -> worldBuffer.getLocalPrice()[index];
                    default -> cell.getElevation() != null ? cell.getElevation() : 0.0;
                };
            }
        }
        if (cell == null) return 0.0;
        return switch (displayMode) {
            case POPULATION -> cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0;
            case FOOD -> cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            case TEMPERATURE -> cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            case TECHNOLOGY -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            case MALTHUSIAN_PRESSURE -> cell.getPopulation() != null && computeCarryingCapacity(cell) > 0 ? (double) cell.getPopulation() / computeCarryingCapacity(cell) : 0.0;
            case MINERAL_RESOURCES -> (cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0) + (cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() * 3.0 : 0.0);
            case MINING_EXPLOITATION -> Math.min(cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0, (cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0) + 10.0);
            case WATER -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
            case WOOD -> cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            case INEQUALITY -> cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
            case FLUX -> cell.getFluxPressure();
            case ALBEDO -> cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
            case FRICTION -> cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            case EPIDEMIC -> cell.getEpidemicInfected() != null ? cell.getEpidemicInfected().doubleValue() : 0.0;
            case AGE_PYRAMID -> (cell.getPop65to79() != null && cell.getPop80Plus() != null) ? (double)(cell.getPop65to79() + cell.getPop80Plus()) : 0.0;
            case ASABIYYAH -> cell.getOwner() != null ? cell.getOwner().getAsabiyyah() : 0.0;
            default -> cell.getElevation() != null ? cell.getElevation() : 0.0;
        };
    }
}
