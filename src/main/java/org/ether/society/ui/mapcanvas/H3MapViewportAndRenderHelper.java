/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.mapcanvas;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.ui.H3Globe3DSubScene;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.H3MapInteractionHandler;
import org.ether.society.ui.ViewMode;
import org.ether.society.ui.renderers.H3BeaconOverlayRenderer;
import org.ether.society.ui.renderers.H3CellColorResolver;
import org.ether.society.ui.renderers.H3Flat2DRenderer;
import org.ether.society.ui.renderers.H3Globe3DRenderer;
import org.ether.society.ui.renderers.H3VectorLayerRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Viewport camera animation controller, spatial index builder, and multi-pass rendering
 * pipeline orchestrator for {@link H3MapCanvas}.
 *
 * <p>Extracted in accordance with the Single Responsibility Principle (SRP) to decouple
 * smooth sinusoidal camera flight trajectories, 60-Hz spherical auto-rotation timers,
 * spatial cell indexing, and layered canvas compositing from the JavaFX {@link javafx.scene.canvas.Canvas}
 * state container.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3MapViewportAndRenderHelper {

    /** Active 60-Hz spherical globe auto-rotation timer. */
    private AnimationTimer autoRotateTimer;

    /** Active smooth sinusoidal camera flight interpolation timer. */
    private AnimationTimer activeFlyTimer;

    /**
     * Constructs a new viewport and rendering orchestrator instance.
     */
    public H3MapViewportAndRenderHelper() {
    }

    /**
     * Synchronizes the 3D globe auto-rotation {@link AnimationTimer} lifecycle with the current
     * visibility, view mode, and auto-rotation flags of the host canvas.
     *
     * @param canvas          the host {@link H3MapCanvas}
     * @param onMiniMapNotify callback notifying the overview {@link org.ether.society.ui.MiniMap}
     */
    public void updateAutoRotateTimerState(H3MapCanvas canvas, Runnable onMiniMapNotify) {
        if (canvas.isAutoRotating() && canvas.getViewMode() == ViewMode.VIEW_3D && canvas.isTabVisible()) {
            if (autoRotateTimer == null) {
                autoRotateTimer = new AnimationTimer() {
                    private long lastNow = 0;

                    @Override
                    public void handle(long now) {
                        if (!canvas.isAutoRotating() || canvas.getViewMode() != ViewMode.VIEW_3D || !canvas.isTabVisible()) {
                            return;
                        }
                        if (activeFlyTimer != null) {
                            lastNow = now;
                            return;
                        }
                        if (lastNow == 0) {
                            lastNow = now;
                            return;
                        }
                        double deltaSeconds = Math.min(0.1, (now - lastNow) / 1_000_000_000.0);
                        lastNow = now;
                        double deg = canvas.getAutoRotationSpeed() * 60.0 * deltaSeconds;
                        double nextLng = (canvas.getCenterLng() + deg + 180.0) % 360.0 - 180.0;
                        canvas.setCenterLng(nextLng);
                        canvas.draw();
                        onMiniMapNotify.run();
                    }
                };
            }
            autoRotateTimer.start();
        } else if (autoRotateTimer != null) {
            autoRotateTimer.stop();
        }
    }

    /**
     * Executes a 1-second smooth cosine-eased camera flight (\( t = \frac{1 - \cos(\pi \tau)}{2} \))
     * along the shortest longitudinal arc toward the target coordinates and zoom factor.
     *
     * @param canvas          the host {@link H3MapCanvas}
     * @param globe3DSubScene optional hardware 3D sub-scene to synchronize
     * @param targetLat       target geodetic latitude in degrees \( [-90, 90] \)
     * @param targetLng       target geodetic longitude in degrees \( [-180, 180] \)
     * @param targetZoom      target viewport zoom factor
     * @param onMiniMapNotify callback notifying the overview {@link org.ether.society.ui.MiniMap}
     */
    public void flyTo(
            H3MapCanvas canvas,
            H3Globe3DSubScene globe3DSubScene,
            double targetLat,
            double targetLng,
            double targetZoom,
            Runnable onMiniMapNotify) {
        if (activeFlyTimer != null) {
            activeFlyTimer.stop();
        }
        if (globe3DSubScene != null) {
            globe3DSubScene.flyTo(targetLat, targetLng);
        }

        final double startLat = canvas.getCenterLat();
        final double startLng = canvas.getCenterLng();
        final double startZoom = canvas.getZoomFactor();
        final double finalTargetZoom = (targetZoom > 0.1 && targetZoom < 50.0) ? targetZoom : startZoom;

        double dLng = targetLng - startLng;
        while (dLng > 180.0) dLng -= 360.0;
        while (dLng < -180.0) dLng += 360.0;
        final double finalTargetLng = startLng + dLng;

        final long startNs = System.nanoTime();
        final long durationNs = 1_000_000_000L;

        activeFlyTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = (now - startNs) / (double) durationNs;
                if (elapsed >= 1.0) {
                    canvas.setCenterLat(targetLat);
                    canvas.setCenterLng(((finalTargetLng + 180.0) % 360.0) - 180.0);
                    canvas.setZoomFactor(finalTargetZoom);
                    canvas.draw();
                    onMiniMapNotify.run();
                    stop();
                    activeFlyTimer = null;
                } else {
                    double t = 0.5 - 0.5 * Math.cos(elapsed * Math.PI);
                    canvas.setCenterLat(startLat + t * (targetLat - startLat));
                    double currentLng = startLng + t * (finalTargetLng - startLng);
                    canvas.setCenterLng(((currentLng + 180.0) % 360.0) - 180.0);
                    canvas.setZoomFactor(startZoom + t * (finalTargetZoom - startZoom));
                    canvas.draw();
                    onMiniMapNotify.run();
                }
            }
        };
        activeFlyTimer.start();
    }

    /**
     * Executes the complete multi-layer cartographic rendering pipeline onto the canvas graphics context.
     *
     * @param canvas              host {@link H3MapCanvas}
     * @param colorResolver       cell color and hillshade shader resolver
     * @param flat2DRenderer      2D equirectangular projection renderer
     * @param globe3DRenderer     3D orthographic globe renderer
     * @param vectorLayerRenderer contour, flux vector, resource glyph, and legend renderer
     * @param beaconRenderer      event beacon and corner HUD badge renderer
     * @param interactionHandler  mouse picking and hover tooltip controller
     * @param h3ToBufferIndexMap  mapping from H3 64-bit index to DOD {@link WorldBuffer} slot
     * @param currentDateStr      formatted simulation calendar string for HUD display
     */
    public void renderLayers(
            H3MapCanvas canvas,
            H3CellColorResolver colorResolver,
            H3Flat2DRenderer flat2DRenderer,
            H3Globe3DRenderer globe3DRenderer,
            H3VectorLayerRenderer vectorLayerRenderer,
            H3BeaconOverlayRenderer beaconRenderer,
            H3MapInteractionHandler interactionHandler,
            Map<Long, Integer> h3ToBufferIndexMap,
            String currentDateStr) {
        List<H3Cell> cells = canvas.getCells();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        double minLat = canvas.getMinLat();
        double maxLat = canvas.getMaxLat();
        double minLng = canvas.getMinLng();
        double maxLng = canvas.getMaxLng();
        double centerLat = canvas.getCenterLat();
        double centerLng = canvas.getCenterLng();
        double scale = canvas.getScale();
        double offsetX = canvas.getOffsetX();
        double offsetY = canvas.getOffsetY();
        double zoom = canvas.getZoomFactor();
        ViewMode viewMode = canvas.getViewMode();

        if (viewMode == ViewMode.VIEW_3D) {
            globe3DRenderer.draw3D(gc, cells, colorResolver, canvas.isSmoothMap(), canvas.isShowHexGrid(),
                    canvas.isShowFloatingLayers(), canvas.isShowHillshading(), canvas.isShowSolarTerminator(),
                    canvas.getVerticalExaggeration(), zoom, centerLat, centerLng,
                    minLat, maxLat, minLng, maxLng, w, h);
        } else {
            flat2DRenderer.draw2D(gc, cells, colorResolver, globe3DRenderer, canvas.isSmoothMap(),
                    canvas.isShowHexGrid(), canvas.isShowFloatingLayers(), canvas.isShowHillshading(),
                    canvas.isShowSolarTerminator(), minLat, maxLat, minLng, maxLng, scale, offsetX, offsetY, w, h);
        }

        if (canvas.isShowContours()) {
            vectorLayerRenderer.drawContours(gc, true, cells, canvas.getCellMap(), canvas.getH3Service(),
                    viewMode, zoom, minLat, maxLat, minLng, scale, offsetX, offsetY,
                    w, h, centerLat, centerLng, canvas.getVerticalExaggeration());
        }

        if (canvas.isShowFlowVectors()) {
            vectorLayerRenderer.drawFlowVectors(gc, true, cells, canvas.getCellMap(), canvas.getH3Service(),
                    viewMode, zoom, minLng, maxLat, scale, offsetX, offsetY, w, h, centerLat, centerLng);
        }

        if (canvas.isShowResourceOverlay()) {
            vectorLayerRenderer.drawResourceOverlay(gc, true, cells, canvas.getWorldBuffer(), h3ToBufferIndexMap,
                    viewMode, zoom, minLng, maxLat, scale, offsetX, offsetY, w, h, centerLat, centerLng);
        }

        beaconRenderer.drawEventBeacons(gc, canvas.getEventSystem(), viewMode, zoom, centerLat, centerLng,
                minLng, maxLat, scale, offsetX, offsetY, w, h);

        if (canvas.isShowCornerOverlays()) {
            beaconRenderer.drawCornerOverlays(gc, true, canvas.getScenarioName(), currentDateStr, w, h);
        }

        if (canvas.isShowLegendOverlay()) {
            vectorLayerRenderer.drawLegendOverlay(gc, cells, canvas.getPrimaryDisplayMode(), colorResolver, w, h);
        }

        if (canvas.isShowMouseOverInfo() && interactionHandler != null && interactionHandler.getTooltip() != null
                && interactionHandler.getTooltip().isVisible() && interactionHandler.getHoveredCell() != null) {
            interactionHandler.updateTooltip(
                    interactionHandler.getPendingCanvasX(),
                    interactionHandler.getPendingCanvasY(),
                    interactionHandler.getPendingSceneX(),
                    interactionHandler.getPendingSceneY()
            );
        }
    }

    /**
     * Copies and sorts H3 cells monotonically by latitude for deterministic painter's-order rendering.
     *
     * @param input raw collection of H3 cells
     * @return latitude-sorted list of H3 cells
     */
    public static List<H3Cell> buildSortedCells(List<H3Cell> input) {
        List<H3Cell> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingDouble(c -> c != null ? c.getLatitude() : 0.0));
        return sorted;
    }

    /**
     * Builds an \( O(1) \) hash map indexing {@link H3Cell} instances by their 64-bit H3 index.
     *
     * @param cells list of H3 cells
     * @return map of H3 index to {@link H3Cell}
     */
    public static Map<Long, H3Cell> buildCellMap(List<H3Cell> cells) {
        Map<Long, H3Cell> map = new HashMap<>(Math.max(16, cells.size() * 4 / 3));
        for (H3Cell c : cells) {
            if (c != null) {
                map.put(c.getH3Index(), c);
            }
        }
        return map;
    }

    /**
     * Computes geographic bounding coordinates {@code [minLat, maxLat, minLng, maxLng]} over a cell set.
     *
     * @param cells list of H3 cells
     * @param fallbackDefault if {@code true}, defaults to global sphere bounds {@code [-90, 90, -180, 180]} when empty
     * @return 4-element array {@code [minLat, maxLat, minLng, maxLng]}
     */
    public static double[] computeGeographicBounds(List<H3Cell> cells, boolean fallbackDefault) {
        double defMinLat = fallbackDefault ? -90.0 : 0.0;
        double defMaxLat = fallbackDefault ? 90.0 : 0.0;
        double defMinLng = fallbackDefault ? -180.0 : 0.0;
        double defMaxLng = fallbackDefault ? 180.0 : 0.0;
        if (cells == null || cells.isEmpty()) {
            return new double[]{defMinLat, defMaxLat, defMinLng, defMaxLng};
        }
        double minLat = cells.stream().mapToDouble(H3Cell::getLatitude).min().orElse(defMinLat);
        double maxLat = cells.stream().mapToDouble(H3Cell::getLatitude).max().orElse(defMaxLat);
        double minLng = cells.stream().mapToDouble(H3Cell::getLongitude).min().orElse(defMinLng);
        double maxLng = cells.stream().mapToDouble(H3Cell::getLongitude).max().orElse(defMaxLng);
        return new double[]{minLat, maxLat, minLng, maxLng};
    }

    /**
     * Rebuilds the mapping from 64-bit H3 cell index to contiguous DOD {@link WorldBuffer} array index.
     *
     * @param buffer    source {@link WorldBuffer} (nullable)
     * @param targetMap target map to clear and populate
     */
    public static void rebuildBufferIndexMap(WorldBuffer buffer, Map<Long, Integer> targetMap) {
        targetMap.clear();
        if (buffer != null) {
            long[] indexes = buffer.getH3Indexes();
            int cap = buffer.getCapacity();
            for (int k = 0; k < cap; k++) {
                if (indexes[k] != 0L) {
                    targetMap.put(indexes[k], k);
                }
            }
        }
    }
}
