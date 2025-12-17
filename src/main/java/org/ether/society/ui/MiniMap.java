/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Mini-map component showing overview of entire simulation area.
 * Displays current viewport and allows click-to-navigate.
 */
public class MiniMap extends Canvas {

    private List<H3Cell> cells;
    private H3MapCanvas mainCanvas;

    // Map bounds (computed from cells)
    private double minLat, maxLat, minLng, maxLng;

    // Viewport state
    private double viewportCenterLat, viewportCenterLng;
    private double viewportZoom;

    private static final double WIDTH = 200;
    private static final double HEIGHT = 150;

    public MiniMap() {
        super(WIDTH, HEIGHT);

        // Styling
        setStyle("-fx-background-color: rgba(40, 40, 40, 0.9);" +
                "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                "-fx-border-width: 2;");

        // Mouse interaction
        setOnMouseClicked(this::handleClick);
        setOnMouseEntered(e -> setCursor(javafx.scene.Cursor.HAND));
        setOnMouseExited(e -> setCursor(javafx.scene.Cursor.DEFAULT));
    }

    /**
     * Set the cells to display and compute bounds.
     */
    public void setCells(List<H3Cell> cells) {
        this.cells = cells;
        computeBounds();
        renderOverview();
    }

    /**
     * Set reference to main canvas for synchronization.
     */
    public void setMainCanvas(H3MapCanvas mainCanvas) {
        this.mainCanvas = mainCanvas;
    }

    /**
     * Update viewport indicator when main canvas changes.
     */
    public void updateViewport(double zoom, double centerLat, double centerLng) {
        this.viewportZoom = zoom;
        this.viewportCenterLat = centerLat;
        this.viewportCenterLng = centerLng;
        renderOverview(); // Re-render with updated viewport
    }

    /**
     * Compute lat/lng bounds from all cells.
     */
    private void computeBounds() {
        if (cells == null || cells.isEmpty()) {
            minLat = maxLat = minLng = maxLng = 0;
            return;
        }

        minLat = cells.stream().mapToDouble(H3Cell::getLatitude).min().orElse(0);
        maxLat = cells.stream().mapToDouble(H3Cell::getLatitude).max().orElse(0);
        minLng = cells.stream().mapToDouble(H3Cell::getLongitude).min().orElse(0);
        maxLng = cells.stream().mapToDouble(H3Cell::getLongitude).max().orElse(0);
    }

    /**
     * Render the overview map with all cells and viewport indicator.
     */
    private void renderOverview() {
        if (cells == null || cells.isEmpty()) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();

        // Clear background
        gc.setFill(Color.rgb(30, 30, 30, 0.9));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw all cells as dots (simplified)
        drawCells(gc);

        // Draw viewport indicator
        drawViewportIndicator(gc);
    }

    /**
     * Draw all cells as colored dots.
     */
    private void drawCells(GraphicsContext gc) {
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;

        if (latRange == 0 || lngRange == 0)
            return;

        for (H3Cell cell : cells) {
            // Map lat/lng to canvas coordinates
            double x = ((cell.getLongitude() - minLng) / lngRange) * (WIDTH - 10) + 5;
            double y = HEIGHT - ((cell.getLatitude() - minLat) / latRange) * (HEIGHT - 10) - 5;

            // Get biome color (simplified)
            Color color = getBiomeColor(cell.getBiome());
            gc.setFill(color);
            gc.fillOval(x - 0.5, y - 0.5, 1, 1); // 1px dot
        }
    }

    /**
     * Draw viewport indicator rectangle.
     */
    private void drawViewportIndicator(GraphicsContext gc) {
        if (mainCanvas == null)
            return;

        // Calculate viewport bounds based on zoom and center
        // This is an approximation - actual viewport depends on canvas size
        double viewportWidth = (maxLng - minLng) / viewportZoom;
        double viewportHeight = (maxLat - minLat) / viewportZoom;

        double viewMinLng = viewportCenterLng - viewportWidth / 2;
        double viewMaxLng = viewportCenterLng + viewportWidth / 2;
        double viewMinLat = viewportCenterLat - viewportHeight / 2;
        double viewMaxLat = viewportCenterLat + viewportHeight / 2;

        // Map to canvas coordinates
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;

        double x1 = ((viewMinLng - minLng) / lngRange) * (WIDTH - 10) + 5;
        double x2 = ((viewMaxLng - minLng) / lngRange) * (WIDTH - 10) + 5;
        double y1 = HEIGHT - ((viewMaxLat - minLat) / latRange) * (HEIGHT - 10) - 5;
        double y2 = HEIGHT - ((viewMinLat - minLat) / latRange) * (HEIGHT - 10) - 5;

        // Draw rectangle
        gc.setStroke(Color.rgb(255, 255, 0, 0.8)); // Yellow
        gc.setLineWidth(2);
        gc.strokeRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Handle click to navigate.
     */
    private void handleClick(MouseEvent event) {
        if (mainCanvas == null)
            return;

        double x = event.getX();
        double y = event.getY();

        // Convert canvas coordinates to lat/lng
        double lngRange = maxLng - minLng;
        double latRange = maxLat - minLat;

        double lng = minLng + ((x - 5) / (WIDTH - 10)) * lngRange;
        double lat = minLat + ((HEIGHT - y - 5) / (HEIGHT - 10)) * latRange;

        // Update main canvas center
        mainCanvas.setCenterView(lat, lng);
    }

    /**
     * Get simplified biome color.
     */
    private Color getBiomeColor(Biome biome) {
        return switch (biome) {
            case OCEAN -> Color.rgb(41, 128, 185);
            case DEEP_OCEAN -> Color.rgb(21, 67, 96);
            case BEACH -> Color.rgb(241, 196, 15);
            case DESERT -> Color.rgb(230, 126, 34);
            case PLAINS -> Color.rgb(39, 174, 96);
            case FOREST -> Color.rgb(22, 160, 133);
            case JUNGLE -> Color.rgb(0, 128, 0);
            case MOUNTAINS -> Color.rgb(149, 165, 166);
            case HILLS -> Color.rgb(127, 140, 141);
            case TUNDRA -> Color.rgb(189, 195, 199);
            case SNOW -> Color.rgb(236, 240, 241);
        };
    }
}

