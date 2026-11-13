/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.events.EventSystem;
import org.ether.society.h3.H3Service;
import org.ether.society.ui.mapcanvas.H3MapViewportAndRenderHelper;
import org.ether.society.ui.renderers.H3BeaconOverlayRenderer;
import org.ether.society.ui.renderers.H3CellColorResolver;
import org.ether.society.ui.renderers.H3Flat2DRenderer;
import org.ether.society.ui.renderers.H3Globe3DRenderer;
import org.ether.society.ui.renderers.H3VectorLayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Interactive Planetary Map Canvas coordinating 2D equirectangular and 3D orthographic globe views.
 *
 * <p>Decomposed in accordance with the Single Responsibility Principle (SRP):
 * <ul>
 *   <li>{@link H3CellColorResolver}: Thematic false-color shaders, Lambertian hillshading, and solar terminator</li>
 *   <li>{@link H3Flat2DRenderer}: 2D equirectangular hexagonal projection and latitude culling</li>
 *   <li>{@link H3Globe3DRenderer}: 3D spherical orthographic projection, horizon culling, and atmosphere glow</li>
 *   <li>{@link H3VectorLayerRenderer}: Hypsometric contour isolines, flux vectors, resource glyphs, and histogram legends</li>
 *   <li>{@link H3BeaconOverlayRenderer}: Pulsing event beacons, radar rings, and scenario HUD badges</li>
 *   <li>{@link H3MapViewportAndRenderHelper}: Camera flight trajectories, auto-rotation timers, and frame compositing</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class H3MapCanvas extends Canvas {
    private static final Logger logger = LoggerFactory.getLogger(H3MapCanvas.class);

    // --- Core Data & State ---
    private List<H3Cell> cells;
    private WorldBuffer worldBuffer;
    private Map<Long, H3Cell> cellMap;
    private final Map<Long, Integer> h3ToBufferIndexMap = new HashMap<>();
    private final Map<Long, Integer> previousPopMap = new HashMap<>();
    private H3Service h3Service;
    private EventSystem eventSystem;

    // --- View & Layer Flags ---
    private ViewMode viewMode = ViewMode.VIEW_2D;
    private final Set<DisplayMode> activeDisplayModes = new LinkedHashSet<>(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
    private DisplayMode displayMode = DisplayMode.POPULATION;
    private ScientificColorMap scientificColorMap = ScientificColorMap.TURBO;

    private boolean showContours = false;
    private boolean showFlowVectors = false;
    private boolean showResourceOverlay = false;
    private boolean showHexGrid = true;
    private boolean showCornerOverlays = true;
    private boolean showLegendOverlay = false;
    private boolean tabVisible = true;
    private boolean smoothMap = true;
    private boolean showHillshading = false;
    private boolean showSolarTerminator = false;
    private boolean showFloatingLayers = true;
    private boolean showMouseOverInfo = true;

    // --- Viewport & Camera Geometry ---
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double minLat, maxLat, minLng, maxLng;
    private double centerLat, centerLng;
    private double zoomFactor = 1.0;
    private double verticalExaggeration = 25.0;
    private double reliefScale = 1.0;

    // --- Auto-Rotation ---
    private boolean autoRotating = false;
    private double autoRotationSpeed = 0.4;

    // --- Interactive Overlays & SubScenes ---
    private MiniMap miniMap;
    private H3Globe3DSubScene globe3DSubScene;

    // --- Metadata & Recording State ---
    private String scenarioName = "Scénario Standard";
    private String currentDateStr = "An -100000";
    private boolean isRecordingVideo = false;

    // --- Modular Interaction, Viewport & Renderers ---
    private final H3MapInteractionHandler interactionHandler;
    private final H3MapViewportAndRenderHelper viewportHelper = new H3MapViewportAndRenderHelper();
    private final H3CellColorResolver colorResolver = new H3CellColorResolver(scientificColorMap);
    private final H3Flat2DRenderer flat2DRenderer = new H3Flat2DRenderer();
    private final H3Globe3DRenderer globe3DRenderer = new H3Globe3DRenderer();
    private final H3VectorLayerRenderer vectorLayerRenderer = new H3VectorLayerRenderer();
    private final H3BeaconOverlayRenderer beaconOverlayRenderer = new H3BeaconOverlayRenderer();

    /**
     * Constructs an H3MapCanvas with the given initial viewport dimensions.
     *
     * @param width  initial canvas width in pixels
     * @param height initial canvas height in pixels
     */
    public H3MapCanvas(double width, double height) {
        super(width, height);
        this.h3Service = new H3Service(8);
        this.colorResolver.setH3Service(this.h3Service);
        this.colorResolver.setActiveDisplayModes(this.activeDisplayModes);
        this.colorResolver.setSmoothMap(this.smoothMap);
        this.interactionHandler = new H3MapInteractionHandler(this);
        widthProperty().addListener((obs, oldW, newW) -> draw());
        heightProperty().addListener((obs, oldH, newH) -> draw());
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double height) { return getWidth(); }
    @Override public double prefHeight(double width) { return getHeight(); }
    @Override public double minWidth(double height) { return 1.0; }
    @Override public double minHeight(double width) { return 1.0; }
    @Override public double maxWidth(double height) { return Double.MAX_VALUE; }
    @Override public double maxHeight(double width) { return Double.MAX_VALUE; }

    // --- Layer & Display Configuration ---

    public boolean isShowContours() { return showContours; }
    public void setShowContours(boolean showContours) { this.showContours = showContours; draw(); }
    public void toggleContours(boolean show) { setShowContours(show); }

    public boolean isShowFlowVectors() { return showFlowVectors; }
    public void setShowFlowVectors(boolean showFlowVectors) { this.showFlowVectors = showFlowVectors; draw(); }

    public boolean isShowResourceOverlay() { return showResourceOverlay; }
    public void setShowResourceOverlay(boolean showResourceOverlay) { this.showResourceOverlay = showResourceOverlay; draw(); }

    public EventSystem getEventSystem() { return eventSystem; }
    public void setEventSystem(EventSystem eventSystem) { this.eventSystem = eventSystem; draw(); }

    public boolean isShowCornerOverlays() { return showCornerOverlays; }
    public void setShowCornerOverlays(boolean show) { this.showCornerOverlays = show; draw(); }

    public boolean isShowLegendOverlay() { return showLegendOverlay; }
    public void setShowLegendOverlay(boolean show) { this.showLegendOverlay = show; draw(); }

    public ScientificColorMap getScientificColorMap() { return scientificColorMap; }
    public void setScientificColorMap(ScientificColorMap cmap) {
        this.scientificColorMap = cmap;
        this.colorResolver.setScientificColorMap(cmap);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public boolean isShowHillshading() { return showHillshading; }
    public void setShowHillshading(boolean show) {
        this.showHillshading = show;
        this.colorResolver.setShowHillshading(show);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public boolean isShowSolarTerminator() { return showSolarTerminator; }
    public void setShowSolarTerminator(boolean show) {
        this.showSolarTerminator = show;
        this.colorResolver.setShowSolarTerminator(show);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public void invalidateSmoothCache() { this.globe3DRenderer.invalidateSmoothCache(); }

    public boolean isTabVisible() { return tabVisible; }
    public void setTabVisible(boolean tabVisible) {
        this.tabVisible = tabVisible;
        viewportHelper.updateAutoRotateTimerState(this, this::notifyMiniMap);
        draw();
    }

    public boolean isSmoothMap() { return smoothMap; }
    public void setSmoothMap(boolean smoothMap) {
        this.smoothMap = smoothMap;
        this.colorResolver.setSmoothMap(smoothMap);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public boolean isShowHexGrid() { return showHexGrid; }
    public void setShowHexGrid(boolean showHexGrid) { this.showHexGrid = showHexGrid; draw(); }

    public boolean isShowFloatingLayers() { return showFloatingLayers; }
    public void setShowFloatingLayers(boolean show) {
        this.showFloatingLayers = show;
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public double getVerticalExaggeration() { return verticalExaggeration; }
    public void setVerticalExaggeration(double verticalExaggeration) { this.verticalExaggeration = verticalExaggeration; draw(); }

    public boolean isShowMouseOverInfo() { return showMouseOverInfo; }
    public void setShowMouseOverInfo(boolean show) { this.showMouseOverInfo = show; }

    public ViewMode getViewMode() { return viewMode; }
    public void setViewMode(ViewMode mode) {
        this.viewMode = mode;
        viewportHelper.updateAutoRotateTimerState(this, this::notifyMiniMap);
        draw();
        logger.info("View mode changed to: {}", mode);
    }

    public DisplayMode getDisplayMode() { return getPrimaryDisplayMode(); }
    public void setDisplayMode(DisplayMode mode) {
        if (mode == null) return;
        this.activeDisplayModes.clear();
        this.activeDisplayModes.add(DisplayMode.BIOME);
        if (mode != DisplayMode.BIOME) {
            this.activeDisplayModes.add(mode);
        }
        this.displayMode = mode;
        syncDisplayModesAndRedraw();
    }

    public Set<DisplayMode> getActiveDisplayModes() {
        return Collections.unmodifiableSet(activeDisplayModes);
    }

    public void setActiveDisplayModes(Collection<DisplayMode> modes) {
        this.activeDisplayModes.clear();
        if (modes != null && !modes.isEmpty()) {
            this.activeDisplayModes.addAll(modes);
        } else {
            this.activeDisplayModes.add(DisplayMode.BIOME);
        }
        this.displayMode = getPrimaryDisplayMode();
        syncDisplayModesAndRedraw();
    }

    public void setDisplayModeActive(DisplayMode mode, boolean active) {
        if (mode == null) return;
        if (active) {
            activeDisplayModes.add(mode);
        } else {
            activeDisplayModes.remove(mode);
            if (activeDisplayModes.isEmpty()) {
                activeDisplayModes.add(DisplayMode.BIOME);
            }
        }
        this.displayMode = getPrimaryDisplayMode();
        syncDisplayModesAndRedraw();
    }

    private void syncDisplayModesAndRedraw() {
        this.colorResolver.setActiveDisplayModes(this.activeDisplayModes);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public boolean isDisplayModeActive(DisplayMode mode) { return activeDisplayModes.contains(mode); }

    public DisplayMode getPrimaryDisplayMode() {
        for (DisplayMode dm : activeDisplayModes) {
            if (dm != DisplayMode.BIOME) return dm;
        }
        return activeDisplayModes.isEmpty() ? DisplayMode.BIOME : activeDisplayModes.iterator().next();
    }

    public boolean isOnlyStaticBiome() {
        return activeDisplayModes.size() == 1 && activeDisplayModes.contains(DisplayMode.BIOME);
    }

    // --- Auto-Rotation & Camera Control ---

    public boolean isAutoRotating() { return autoRotating; }
    public void setAutoRotating(boolean autoRotating) {
        this.autoRotating = autoRotating;
        if (globe3DSubScene != null) globe3DSubScene.setAutoRotating(autoRotating);
        viewportHelper.updateAutoRotateTimerState(this, this::notifyMiniMap);
        draw();
    }

    public double getAutoRotationSpeed() { return autoRotationSpeed; }
    public void setAutoRotationSpeed(double autoRotationSpeed) { this.autoRotationSpeed = autoRotationSpeed; }

    public void tickAutoRotation() {
        if (autoRotating && viewMode == ViewMode.VIEW_3D) {
            centerLng = (centerLng + autoRotationSpeed + 180.0) % 360.0 - 180.0;
            draw();
            notifyMiniMap();
        }
    }

    public void resetView() {
        this.zoomFactor = 1.0;
        if (cells != null && !cells.isEmpty()) {
            if (minLat == 0 && maxLat == 0 && minLng == 0 && maxLng == 0) {
                double[] b = H3MapViewportAndRenderHelper.computeGeographicBounds(cells, true);
                minLat = b[0]; maxLat = b[1]; minLng = b[2]; maxLng = b[3];
            }
            this.centerLat = (minLat + maxLat) / 2.0;
            this.centerLng = (minLng + maxLng) / 2.0;
        } else {
            this.centerLat = 0.0;
            this.centerLng = 0.0;
        }
        if (globe3DSubScene != null) globe3DSubScene.resetCamera();
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
        notifyMiniMap();
    }

    public H3Globe3DSubScene getGlobe3DSubScene() {
        if (globe3DSubScene == null) {
            globe3DSubScene = new H3Globe3DSubScene(getWidth() > 0 ? getWidth() : 1280, getHeight() > 0 ? getHeight() : 800);
            if (cells != null && !cells.isEmpty()) globe3DSubScene.updateTerrainMesh(cells);
        }
        return globe3DSubScene;
    }

    public void setReliefScale(double scale) {
        this.reliefScale = scale;
        if (globe3DSubScene != null) {
            globe3DSubScene.setReliefScale(scale);
            if (cells != null && !cells.isEmpty()) globe3DSubScene.updateTerrainMesh(cells);
        }
        draw();
    }

    public double getReliefScale() { return reliefScale; } public double getZoomFactor() { return zoomFactor; }
    public double getCenterLat() { return centerLat; } public double getCenterLng() { return centerLng; }
    public void setMiniMap(MiniMap miniMap) { this.miniMap = miniMap; notifyMiniMap(); }
    public void setCenterView(double lat, double lng) { this.centerLat = Math.max(-90.0, Math.min(90.0, lat)); this.centerLng = Math.max(-180.0, Math.min(180.0, lng)); draw(); notifyMiniMap(); }

    public void centerOnCoordinates(double lat, double lng) { setCenterView(lat, lng); }

    void notifyMiniMap() {
        if (miniMap != null) miniMap.updateViewport(zoomFactor, centerLat, centerLng);
    }

    public double getMinLat() { return minLat; } public double getMaxLat() { return maxLat; }
    public double getMinLng() { return minLng; } public double getMaxLng() { return maxLng; }
    public double getScale() { return scale; } public double getOffsetX() { return offsetX; } public double getOffsetY() { return offsetY; }
    public void setCenterLat(double centerLat) { this.centerLat = centerLat; } public void setCenterLng(double centerLng) { this.centerLng = centerLng; }
    public void setZoomFactor(double zoomFactor) { this.zoomFactor = zoomFactor; }
    public H3BeaconOverlayRenderer getBeaconOverlayRenderer() { return beaconOverlayRenderer; }
    public Map<Long, Integer> getPreviousPopMap() { return previousPopMap; } public WorldBuffer getWorldBuffer() { return worldBuffer; }
    public H3Service getH3Service() { return h3Service; } public Map<Long, H3Cell> getCellMap() { return cellMap; }

    // --- Data Ingestion ---

    public List<H3Cell> getCells() { return cells; }

    public void setCells(List<H3Cell> cells) {
        if (cells == null) {
            this.cells = new ArrayList<>();
            this.cellMap = new HashMap<>();
            this.colorResolver.setCells(this.cells);
            this.colorResolver.setCellMap(this.cellMap);
            return;
        }
        this.cells = H3MapViewportAndRenderHelper.buildSortedCells(cells);
        this.cellMap = H3MapViewportAndRenderHelper.buildCellMap(this.cells);

        if (!cells.isEmpty() && h3Service != null) {
            try {
                int detectedRes = h3Service.getResolution(cells.get(0).getH3Index());
                if (detectedRes != h3Service.getResolution()) {
                    this.h3Service = new H3Service(detectedRes);
                    this.colorResolver.setH3Service(this.h3Service);
                }
            } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(H3MapCanvas.class).debug("Non-fatal condition in H3MapCanvas: {}", ignored.getMessage()); }
        }

        double[] bounds = H3MapViewportAndRenderHelper.computeGeographicBounds(cells, false);
        minLat = bounds[0]; maxLat = bounds[1]; minLng = bounds[2]; maxLng = bounds[3];

        this.colorResolver.setCells(this.cells);
        this.colorResolver.setCellMap(this.cellMap);
        this.globe3DRenderer.invalidateSmoothCache();
        resetView();

        if (globe3DSubScene != null) globe3DSubScene.updateTerrainMesh(cells);
    }

    public void setWorldBuffer(WorldBuffer buffer) {
        this.worldBuffer = buffer;
        H3MapViewportAndRenderHelper.rebuildBufferIndexMap(buffer, this.h3ToBufferIndexMap);
        this.colorResolver.setWorldBuffer(buffer);
        this.colorResolver.setH3ToBufferIndexMap(this.h3ToBufferIndexMap);
        this.globe3DRenderer.invalidateSmoothCache();
        draw();
    }

    public Integer getBufferIndex(long h3Index) { return h3ToBufferIndexMap.get(h3Index); }

    // --- Overlays, Metadata & Video Stubs ---

    public void setTooltipContainer(Pane container) {
        if (interactionHandler != null) interactionHandler.setTooltipContainer(container);
    }

    public void setScenarioName(String name) {
        if (name != null && !name.isBlank()) this.scenarioName = name;
        draw();
    }
    public String getScenarioName() { return scenarioName; }

    public void setCurrentDateStr(String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) this.currentDateStr = dateStr;
    }

    public boolean isRecordingVideo() { return isRecordingVideo; }
    public void startVideoRecording() { this.isRecordingVideo = true; }
    public void stopVideoRecording() { this.isRecordingVideo = false; }
    public void captureTickFrame() {} public void captureTickFrame(long currentTick) {} public void truncateVideoFramesAbove(long tickIndex) {}

    public void setOnHoverCallback(BiConsumer<H3Cell, double[]> callback) {
        if (interactionHandler != null) interactionHandler.setOnHoverCallback(callback);
    }
    public void setOnCellClickedCallback(BiConsumer<Double, Double> callback) {
        if (interactionHandler != null) interactionHandler.setOnCellClickedCallback(callback);
    }

    // --- Main Rendering Pipeline ---

    public void draw() {
        if (!tabVisible || !isVisible() || getWidth() < 1.0 || getHeight() < 1.0) return;
        if (cells == null || cells.isEmpty()) return;

        double scaleX = getWidth() / Math.max(1e-4, maxLng - minLng);
        double scaleY = getHeight() / Math.max(1e-4, maxLat - minLat);
        scale = Math.min(scaleX, scaleY) * 0.9 * zoomFactor;
        offsetX = (getWidth() / 2.0) - (centerLng - minLng) * scale;
        offsetY = (getHeight() / 2.0) - (maxLat - centerLat) * scale;

        viewportHelper.renderLayers(this, colorResolver, flat2DRenderer, globe3DRenderer,
                vectorLayerRenderer, beaconOverlayRenderer, interactionHandler, h3ToBufferIndexMap, currentDateStr);
    }

    // --- Camera Flights & Event Beacons ---

    public void flyTo(double targetLat, double targetLng) {
        double comfortableZoom = (viewMode == ViewMode.VIEW_3D) ? this.zoomFactor : Math.max(2.8, this.zoomFactor);
        flyTo(targetLat, targetLng, comfortableZoom);
    }

    public void flyTo(double targetLat, double targetLng, double targetZoom) {
        viewportHelper.flyTo(this, globe3DSubScene, targetLat, targetLng, targetZoom, this::notifyMiniMap);
    }

    public void pingLocation(double lat, double lng, String title, String type, double magnitude) {
        beaconOverlayRenderer.pingLocation(lat, lng, title, type, magnitude, this::draw);
    }

    public static class BeaconItem extends H3BeaconOverlayRenderer.BeaconItem {
        public BeaconItem(double lat, double lng, String title, String type, double magnitude, long createdAtMs, long durationMs) {
            super(lat, lng, title, type, magnitude, createdAtMs, durationMs);
        }
    }

    public static class ProjectedPoint extends H3VectorLayerRenderer.ProjectedPoint {
        public ProjectedPoint() { super(); }
        public ProjectedPoint(double screenX, double screenY, double z) { super(screenX, screenY, z); }
    }

    // --- Mouse Interaction & Picking Delegation ---

    public void setupMouseHandlers() {
        if (interactionHandler != null) interactionHandler.setupMouseHandlers();
    }

    public double[] getLatLngAt(double mouseX, double mouseY) { return interactionHandler != null ? interactionHandler.getLatLngAt(mouseX, mouseY) : null; }
    public H3Cell findCellAt(double mouseX, double mouseY) { return interactionHandler != null ? interactionHandler.findCellAt(mouseX, mouseY) : null; }
    public void updateTooltip(double canvasX, double canvasY, double sceneX, double sceneY) { if (interactionHandler != null) interactionHandler.updateTooltip(canvasX, canvasY, sceneX, sceneY); }

    // --- Public Color & Metric Delegation ---

    public Color blendColors(Color base, Color overlay, double alpha) { return H3CellColorResolver.blendColors(base, overlay, alpha); }
    public boolean hasThematicOverlay(H3Cell cell) { return colorResolver.hasThematicOverlay(cell, getPrimaryDisplayMode()); }
    public double getCellDisplayValue(H3Cell cell, int unusedIndex) { return colorResolver.getCellDisplayValue(cell, getPrimaryDisplayMode(), getPrimaryDisplayMode()); }
    public double getCellDisplayValue(H3Cell cell, DisplayMode mode, int unusedIndex) { return colorResolver.getCellDisplayValue(cell, mode, getPrimaryDisplayMode()); }
}

