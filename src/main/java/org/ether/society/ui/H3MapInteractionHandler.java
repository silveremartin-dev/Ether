/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.events.EventSystem;
import org.ether.society.h3.H3Service;
import org.ether.society.ui.renderers.H3BeaconOverlayRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Handles user input interactions, mouse picking, zoom/pan navigation, and tooltip tracking for {@link H3MapCanvas}.
 *
 * <p>De-couples mouse event lifecycle from canvas graphics rendering:
 * <ul>
 *   <li><b>Orthographic 3D Raycasting</b>: Inverts screen $(x, y)$ pixels to spherical geodetic $(\phi, \lambda)$ coordinates
 *       via 3D Euler rotation matrices:
 *       \[
 *         dx = \frac{x - x_c}{R}, \quad dy = \frac{y_c - y}{R}, \quad z_r = \sqrt{1 - (dx^2 + dy^2)}
 *       \]
 *   </li>
 *   <li><b>Equirectangular 2D Inversion</b>: Linear affine transformation from canvas viewport to geographic bounds:
 *       \[
 *         \lambda = \frac{x - \text{offsetX}}{S} + \lambda_{\min}, \quad \phi = \phi_{\max} - \frac{y - \text{offsetY}}{S}
 *       \]
 *   </li>
 *   <li><b>Hexagonal Cell Resolution</b>: Nearest neighbor spatial lookup accelerated by {@link H3Service}.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class H3MapInteractionHandler {
    private static final Logger logger = LoggerFactory.getLogger(H3MapInteractionHandler.class);

    private final H3MapCanvas canvas;

    // Interaction State
    private double dragStartX;
    private double dragStartY;
    private CellTooltip tooltip;
    private Pane tooltipContainer;
    private H3Cell hoveredCell = null;
    private H3Cell pendingHoverCell = null;
    private PauseTransition hoverTimer;
    private double pendingCanvasX, pendingCanvasY, pendingSceneX, pendingSceneY;

    // Callbacks
    private BiConsumer<H3Cell, double[]> onHoverCallback;
    private BiConsumer<Double, Double> onCellClickedCallback;

    /**
     * Constructs a new interaction handler bound to the specified canvas.
     *
     * @param canvas the target {@link H3MapCanvas}
     */
    public H3MapInteractionHandler(H3MapCanvas canvas) {
        this.canvas = canvas;
        setupMouseHandlers();
    }

    /**
     * Initializes mouse event listeners (scroll, press, drag, click, move, and exit) on the canvas.
     */
    public void setupMouseHandlers() {
        hoverTimer = new PauseTransition(Duration.millis(150));
        hoverTimer.setOnFinished(e -> {
            if (pendingHoverCell != null && tooltip != null && tooltipContainer != null) {
                updateTooltip(pendingCanvasX, pendingCanvasY, pendingSceneX, pendingSceneY);
            }
        });

        canvas.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double zoomChange = delta > 0 ? 1.15 : 0.85;
            double oldZoom = canvas.getZoomFactor();
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * zoomChange));

            if (newZoom != oldZoom) {
                double mouseX = event.getX();
                double mouseY = event.getY();
                double w = canvas.getWidth();
                double h = canvas.getHeight();
                double latRange = canvas.getMaxLat() - canvas.getMinLat();
                double lngRange = canvas.getMaxLng() - canvas.getMinLng();
                if (latRange > 0 && lngRange > 0 && w > 0 && h > 0) {
                    double baseScale = Math.min(w / lngRange, h / latRange) * 0.9;
                    double oldScale = baseScale * oldZoom;
                    double newScale = baseScale * newZoom;
                    double mouseLng = canvas.getCenterLng() + (mouseX - w / 2.0) / oldScale;
                    double mouseLat = canvas.getCenterLat() - (mouseY - h / 2.0) / oldScale;
                    double targetLng = mouseLng - (mouseX - w / 2.0) / newScale;
                    double targetLat = mouseLat + (mouseY - h / 2.0) / newScale;
                    canvas.setCenterLat(targetLat);
                    canvas.setCenterLng(targetLng);
                }
                canvas.setZoomFactor(newZoom);
                canvas.draw();
                canvas.notifyMiniMap();
            }
        });

        canvas.setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();
            if (event.isSecondaryButtonDown()) canvas.setCursor(Cursor.CROSSHAIR);
            else if (event.isPrimaryButtonDown()) canvas.setCursor(Cursor.MOVE);
        });

        canvas.setOnMouseDragged(event -> {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            if (canvas.getViewMode() == ViewMode.VIEW_3D) {
                double sensitivity = 0.35 / canvas.getZoomFactor();
                canvas.setCenterLng(Math.clamp(canvas.getCenterLng() - dx * sensitivity, -180.0, 180.0));
                canvas.setCenterLat(Math.clamp(canvas.getCenterLat() - dy * sensitivity, -89.0, 89.0));
            } else {
                canvas.setCenterLng(Math.clamp(canvas.getCenterLng() - dx / canvas.getScale(), -180.0, 180.0));
                canvas.setCenterLat(Math.clamp(canvas.getCenterLat() + dy / canvas.getScale(), -90.0, 90.0));
            }
            dragStartX = event.getX();
            dragStartY = event.getY();
            canvas.draw();
            canvas.notifyMiniMap();
        });

        canvas.setOnMouseReleased(event -> canvas.setCursor(Cursor.DEFAULT));

        canvas.setOnMouseClicked(event -> {
            H3BeaconOverlayRenderer.EventBeaconTarget hit = canvas.getBeaconOverlayRenderer().findHitBeacon(event.getX(), event.getY());
            if (hit != null) {
                canvas.flyTo(hit.lat, hit.lng);
                return;
            }
            if (event.getClickCount() == 2) {
                canvas.resetView();
            } else if (event.getClickCount() == 1) {
                double[] latLng = getLatLngAt(event.getX(), event.getY());
                if (latLng != null && onCellClickedCallback != null) {
                    onCellClickedCallback.accept(latLng[0], latLng[1]);
                }
            }
        });

        canvas.setOnMouseMoved(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double[] latLng = getLatLngAt(mouseX, mouseY);
            if (latLng != null && onHoverCallback != null) {
                onHoverCallback.accept(findCellAt(mouseX, mouseY), latLng);
            }
            if (!canvas.isShowMouseOverInfo()) {
                if (tooltip != null) { tooltip.hide(); hoveredCell = null; }
                return;
            }
            H3Cell cell = findCellAt(mouseX, mouseY);
            if (cell == null || cell != pendingHoverCell) {
                if (tooltip != null) { tooltip.hide(); hoveredCell = null; }
                hoverTimer.stop();
                pendingHoverCell = cell;
                pendingCanvasX = mouseX; pendingCanvasY = mouseY;
                pendingSceneX = event.getSceneX(); pendingSceneY = event.getSceneY();
                if (cell != null) hoverTimer.playFromStart();
            } else {
                pendingCanvasX = mouseX; pendingCanvasY = mouseY;
                pendingSceneX = event.getSceneX(); pendingSceneY = event.getSceneY();
            }
        });

        canvas.setOnMouseExited(event -> {
            if (hoverTimer != null) hoverTimer.stop();
            pendingHoverCell = null;
            if (tooltip != null) { tooltip.hide(); hoveredCell = null; }
        });
    }

    /**
     * Computes the geodetic latitude and longitude coordinates under the specified canvas pixel position.
     *
     * @param mouseX x coordinate on canvas
     * @param mouseY y coordinate on canvas
     * @return array containing {@code [latitude, longitude]} in degrees, or {@code null} if outside spherical disc
     */
    public double[] getLatLngAt(double mouseX, double mouseY) {
        if (canvas.getViewMode() == ViewMode.VIEW_3D) {
            double radius = Math.min(canvas.getWidth(), canvas.getHeight()) * 0.45 * canvas.getZoomFactor();
            double cx = canvas.getWidth() / 2.0;
            double cy = canvas.getHeight() / 2.0;
            double dx = (mouseX - cx) / radius;
            double dy = (cy - mouseY) / radius;
            double r2 = dx * dx + dy * dy;
            if (r2 > 1.0) return null;

            double zr = Math.sqrt(Math.max(0.0, 1.0 - r2));
            double radRotationY = Math.toRadians(-canvas.getCenterLng());
            double radTilt = Math.toRadians(canvas.getCenterLat());
            double cosT = Math.cos(radTilt), sinT = Math.sin(radTilt);
            double cosR = Math.cos(radRotationY), sinR = Math.sin(radRotationY);

            double yrt = dy * cosT - zr * sinT;
            double zrt = dy * sinT + zr * cosT;
            double xr = dx * cosR - zrt * sinR;
            double zrOrig = dx * sinR + zrt * cosR;

            return new double[]{
                Math.toDegrees(Math.asin(Math.clamp(yrt, -1.0, 1.0))),
                Math.toDegrees(Math.atan2(xr, zrOrig))
            };
        } else {
            double lng = ((mouseX - canvas.getOffsetX()) / canvas.getScale()) + canvas.getMinLng();
            double lat = canvas.getMaxLat() - ((mouseY - canvas.getOffsetY()) / canvas.getScale());
            if (lat < canvas.getMinLat() || lat > canvas.getMaxLat() || lng < canvas.getMinLng() || lng > canvas.getMaxLng()) return null;
            return new double[]{lat, lng};
        }
    }

    /**
     * Resolves the {@link H3Cell} directly underneath the given canvas pixel coordinate.
     *
     * @param mouseX x coordinate on canvas
     * @param mouseY y coordinate on canvas
     * @return closest {@link H3Cell}, or {@code null} if no cell matches within proximity threshold
     */
    public H3Cell findCellAt(double mouseX, double mouseY) {
        List<H3Cell> cells = canvas.getCells();
        if (cells == null || cells.isEmpty()) return null;
        double[] latLng = getLatLngAt(mouseX, mouseY);
        if (latLng == null) return null;
        double lat = latLng[0], lng = latLng[1];

        H3Service h3Service = canvas.getH3Service();
        Map<Long, H3Cell> cellMap = canvas.getCellMap();
        if (h3Service != null && cellMap != null) {
            long h3Index = h3Service.latLngToCell(lat, lng);
            if (cellMap.containsKey(h3Index)) return cellMap.get(h3Index);
        }

        H3Cell closest = null;
        double minDistSq = Double.MAX_VALUE;
        double maxDistThreshold = (canvas.getMaxLat() - canvas.getMinLat() + 1.0) / Math.max(10, Math.sqrt(cells.size())) * 3.5;
        double threshSq = maxDistThreshold * maxDistThreshold;

        for (H3Cell c : cells) {
            double dLat = c.getLatitude() - lat;
            double dLng = c.getLongitude() - lng;
            double distSq = dLat * dLat + dLng * dLng;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                closest = c;
            }
        }
        return minDistSq <= threshSq ? closest : null;
    }

    /**
     * Refreshes and repositions the cell tooltip at the specified coordinates.
     *
     * @param canvasX x coordinate on canvas
     * @param canvasY y coordinate on canvas
     * @param sceneX  scene x coordinate
     * @param sceneY  scene y coordinate
     */
    public void updateTooltip(double canvasX, double canvasY, double sceneX, double sceneY) {
        H3Cell cell = findCellAt(canvasX, canvasY);
        if (cell != null && tooltip != null) {
            if (cell != hoveredCell) {
                canvas.getPreviousPopMap().putIfAbsent(cell.getH3Index(), cell.getPopulation() != null ? cell.getPopulation() : 0);
                hoveredCell = cell;
            }
            Integer prevPop = canvas.getPreviousPopMap().get(cell.getH3Index());
            Integer idx = canvas.getBufferIndex(cell.getH3Index());
            EventSystem eventSystem = canvas.getEventSystem();
            var activeEvents = (eventSystem != null) ? eventSystem.getActiveEvents() : null;
            tooltip.updateCell(cell, canvas.getWorldBuffer(), idx, prevPop, activeEvents);

            double contW = tooltipContainer != null ? tooltipContainer.getWidth() : canvas.getWidth();
            double contH = tooltipContainer != null ? tooltipContainer.getHeight() : canvas.getHeight();
            tooltip.position(canvasX, canvasY, contW, contH);
        } else if (tooltip != null) {
            tooltip.hide();
            hoveredCell = null;
        }
    }

    /**
     * Binds the tooltip container overlay.
     *
     * @param container pane hosting the tooltip
     */
    public void setTooltipContainer(Pane container) {
        this.tooltipContainer = container;
        this.tooltip = new CellTooltip();
        this.tooltip.setMouseTransparent(true);
        StackPane.setAlignment(tooltip, Pos.TOP_LEFT);
        container.getChildren().add(tooltip);
    }

    public CellTooltip getTooltip() { return tooltip; }
    public H3Cell getHoveredCell() { return hoveredCell; }
    public double getPendingCanvasX() { return pendingCanvasX; }
    public double getPendingCanvasY() { return pendingCanvasY; }
    public double getPendingSceneX() { return pendingSceneX; }
    public double getPendingSceneY() { return pendingSceneY; }

    public void setOnHoverCallback(BiConsumer<H3Cell, double[]> callback) { this.onHoverCallback = callback; }
    public void setOnCellClickedCallback(BiConsumer<Double, Double> callback) { this.onCellClickedCallback = callback; }
}
