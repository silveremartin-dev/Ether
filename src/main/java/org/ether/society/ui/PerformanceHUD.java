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
 * Heads-Up Display (HUD) for performance monitoring.
 * Displays FPS, memory usage, and simulation stats.
 */
public class PerformanceHUD extends VBox {

    private final Label fpsLabel;
    private final Label memoryLabel;
    private final Label entitiesLabel;
    private final Label cameraLabel;

    // FPS Calculation
    private static final int FRAME_HISTORY_SIZE = 100;
    private final long[] frameTimes = new long[FRAME_HISTORY_SIZE];
    private int frameTimeIndex = 0;
    private boolean arrayFilled = false;
    private long lastUiUpdate = 0;

    private final Label profilerLabel;

    public PerformanceHUD() {
        // Styling - Top Right alignment, semi-transparent
        setStyle("-fx-background-color: rgba(40, 40, 40, 0.85);" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 0 0 0 10;" + // Rounded bottom-left corner
                "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                "-fx-border-width: 0 0 1 1;" +
                "-fx-border-radius: 0 0 0 10;");

        setPadding(new Insets(10));
        setSpacing(5);
        setAlignment(Pos.TOP_LEFT);
        setMaxWidth(250);

        // Initialize labels
        fpsLabel = createLabel("FPS: --");
        memoryLabel = createLabel("Memory: --");
        entitiesLabel = createLabel("Cells: --");
        cameraLabel = createLabel("Zoom: --");
        profilerLabel = createLabel("Tick: -- ms");

        getChildren().addAll(fpsLabel, memoryLabel, entitiesLabel, cameraLabel, profilerLabel);

        // Ensure it doesn't capture mouse events intended for the map
        setMouseTransparent(true);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Consolas', 'Monaco', monospace;");
        return label;
    }

    /**
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

    private void updateStats(long now, long oldTime) {
        // 1. Calculate FPS
        if (arrayFilled) {
            long elapsedNanos = now - oldTime;
            long elapsedMillis = elapsedNanos / 1_000_000;
            if (elapsedMillis > 0) {
                // We have history for FRAME_HISTORY_SIZE frames
                double fps = 1000.0 * FRAME_HISTORY_SIZE / elapsedMillis;
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

    public void updateSimulationInfo(int cellCount, double zoom, double centerLat, double centerLng) {
        entitiesLabel.setText(String.format("%s %,d", I18n.get("ui.hud.cells"), cellCount));
        cameraLabel.setText(
                String.format("%s %.1fx | %.2f°N, %.2f°E", I18n.get("ui.hud.zoom"), zoom, centerLat, centerLng));
    }

    public void updateProfilerInfo(double avgTickMs, double p95TickMs) {
        if (profilerLabel != null) {
            profilerLabel.setText(String.format("⏱️ Tick: %.1fms (P95: %.1fms)", avgTickMs, p95TickMs));
        }
    }
}

