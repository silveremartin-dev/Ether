/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * <h1>Performance HUD</h1>
 * <p>
 * User interface component and visualization panel for the Ether simulation platform.<br>
 * Provides interactive rendering, real-time spatial heatmaps, parameter controls, and multi-language localized analytics.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PerformanceHUD extends VBox {

    private final Label fpsLabel;
    private final Label memoryLabel;
    private final Label entitiesLabel;
    private final Label cameraLabel;

    // FPS Calculation
    /* Internal state variable for frame history size (int). */
    private static final int FRAME_HISTORY_SIZE = 100;
    /* Internal state variable for frame times (long[]). */
    private final long[] frameTimes = new long[FRAME_HISTORY_SIZE];
    /* Internal state variable for frame time index (int). */
    private int frameTimeIndex = 0;
    /* Internal state variable for array filled (boolean). */
    private boolean arrayFilled = false;
    /* Internal state variable for last ui update (long). */
    private long lastUiUpdate = 0;

    private final Label profilerLabel;

    /*
     * Performance hud.
     * Enforces physical invariants and updates associated state variables within {@code PerformanceHUD}.
     *
     */
    public PerformanceHUD() {
        getStyleClass().add("hud-panel");

        setPadding(new Insets(10));
        setSpacing(5);
        setAlignment(Pos.TOP_LEFT);
        setMaxWidth(250);

        // Initialize labels
        fpsLabel = createLabel("FPS: --");
        memoryLabel = createLabel("Memory: --");
        entitiesLabel = createLabel("Cells: --");
        cameraLabel = createLabel("Zoom: --");
        profilerLabel = createLabel("Pas : -- ms");

        getChildren().addAll(fpsLabel, memoryLabel, entitiesLabel, cameraLabel, profilerLabel);

        // Ensure it doesn't capture mouse events intended for the map
        setMouseTransparent(true);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hud-label");
        return label;
    }

    /*
     * Call this method every frame from the AnimationTimer.
     * 
     * @param now Timestamp in nanoseconds
     */
    public void registerFrame(long now) {
        long oldTime = frameTimes[frameTimeIndex];
        frameTimes[frameTimeIndex] = now;
        frameTimeIndex = (frameTimeIndex + 1) % FRAME_HISTORY_SIZE;

        if (frameTimeIndex == 0) {
            arrayFilled = true;
        }

        // Update UI roughly every 200ms to avoid flickering
        if (now - lastUiUpdate > 200_000_000) { // 200ms in nanos
            updateStats(now, oldTime);
            lastUiUpdate = now;
        }
    }

    /* Internal state variable for current fps (double). */
    private double currentFps = 0.0;

    /*
     * Get fps.
     * Enforces physical invariants and updates associated state variables within {@code PerformanceHUD}.
     *
     * @return the resulting computation or state reference
     */
    public double getFps() {
        return currentFps;
    }

    private void updateStats(long now, long oldTime) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        // 1. Calculate FPS
        if (arrayFilled) {
            long elapsedNanos = now - oldTime;
            long elapsedMillis = elapsedNanos / 1_000_000;
            if (elapsedMillis > 0) {
                // We have history for FRAME_HISTORY_SIZE frames
                double fps = 1000.0 * FRAME_HISTORY_SIZE / elapsedMillis;
                this.currentFps = fps;
                double frameTime = (double) elapsedMillis / FRAME_HISTORY_SIZE;
                fpsLabel.setText(String.format("%s %.1f (%.1f ms)", I18n.get("ui.hud.fps"), fps, frameTime));

                // Color code FPS
                if (fps < 30)
                    fpsLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-family: 'Consolas';"); // Red
                else if (fps < 55)
                    fpsLabel.setStyle("-fx-text-fill: #feca57; -fx-font-family: 'Consolas';"); // Yellow
                else
                    fpsLabel.setStyle("-fx-text-fill: #1dd1a1; -fx-font-family: 'Consolas';"); // Green
            }
        }

        // 2. Memory Usage
        Runtime runtime = Runtime.getRuntime();
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        memoryLabel.setText(String.format("%s %dMB / %dMB", I18n.get("ui.hud.memory"), usedMem, totalMem));

        // 3. Camera & Entities (Updated via setter to keep this method clean)
    }

    /*
     * Update simulation info.
     * Enforces physical invariants and updates associated state variables within {@code PerformanceHUD}.
     *
     * @param cellCount the cell count parameter (int)
     * @param zoom the zoom parameter (double)
     * @param centerLat the center lat parameter (double)
     * @param centerLng the center lng parameter (double)
     */
    public void updateSimulationInfo(int cellCount, double zoom, double centerLat, double centerLng) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        entitiesLabel.setText(String.format("%s %,d", I18n.get("ui.hud.cells"), cellCount));
        cameraLabel.setText(
                String.format("%s %.1fx | %.2f°N, %.2f°E", I18n.get("ui.hud.zoom"), zoom, centerLat, centerLng));
    }

    /*
     * Update profiler info.
     * Enforces physical invariants and updates associated state variables within {@code PerformanceHUD}.
     *
     * @param avgTickMs the avg tick ms parameter (double)
     * @param p95TickMs the p95tick ms parameter (double)
     */
    public void updateProfilerInfo(double avgTickMs, double p95TickMs) {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (profilerLabel != null) {
            profilerLabel.setText(String.format("⏱️ Pas : %.1fms (P95: %.1fms)", avgTickMs, p95TickMs));
        }
    }
}

