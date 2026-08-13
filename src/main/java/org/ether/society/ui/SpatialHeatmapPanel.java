/*
 * Ether - Human Society Simulation
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * MIT License
 */
package org.ether.society.ui;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;

/**
 * 2D & 2D+1D Spatial Heatmap & Animated Temporal Map Component for StatsPanel.
 * Renders spatial density maps (Population, Wealth, Culture/Language, Climate)
 * and animates historical snapshot sequences over time (2D + 1D Temporal).
 * Computes Moran's I spatial autocorrelation index live.
 *
 * @author Silvere Martin-Michiellot
 */
public class SpatialHeatmapPanel extends VBox {

    private final ComboBox<String> mapTypeCombo;
    private final Canvas mapCanvas;
    private final Label lblMoranI = new Label("I de Moran (Autocorrélation Spatiale) : --");
    private final Button playPauseBtn;

    private List<H3Cell> currentCells = new ArrayList<>();
    private HistoryManager historyManager;
    private Timeline animationTimeline;
    private boolean isPlayingAnimation = false;
    private int currentSnapshotIndex = 0;

    public SpatialHeatmapPanel() {
        setPadding(new Insets(8));
        setSpacing(8);
        getStyleClass().add("card-section");

        // Header
        Label title = new Label("🗺️ CARTE DE DENSITÉ SPATIALE & SÉQUENCE ANIMÉE (2D + 1D Temps)");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        // Controls
        mapTypeCombo = new ComboBox<>();
        mapTypeCombo.getItems().addAll(
                "👥 Densité de Population",
                "💎 Richesse & Capital Spécialisé",
                "🗣️ Diffusion Linguistique & Culturelle",
                "🌡️ Température & Biome"
        );
        mapTypeCombo.setValue("👥 Densité de Population");
        mapTypeCombo.setMaxWidth(Double.MAX_VALUE);
        mapTypeCombo.setOnAction(e -> renderMap());

        playPauseBtn = new Button("▶ Jouer Séquence Temporelle (2D+1D)");
        playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        playPauseBtn.setOnAction(e -> toggleAnimation());

        HBox controlBox = new HBox(8, mapTypeCombo, playPauseBtn);
        HBox.setHgrow(mapTypeCombo, Priority.ALWAYS);

        // Map Canvas (280x140)
        mapCanvas = new Canvas(320, 140);

        // Spatial Autocorrelation Label
        lblMoranI.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        getChildren().addAll(title, controlBox, mapCanvas, lblMoranI);

        setupAnimation();
    }

    public void setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public void updateCells(List<H3Cell> cells) {
        if (!isPlayingAnimation && cells != null) {
            this.currentCells = cells;
            renderMap();
        }
    }

    private void renderMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        double w = mapCanvas.getWidth();
        double h = mapCanvas.getHeight();

        // Background
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42, 0.45));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        if (currentCells == null || currentCells.isEmpty()) {
            gc.setFill(Color.web("#64748b"));
            gc.fillText("Aucune donnée spatiale", w / 3, h / 2);
            return;
        }

        String mode = mapTypeCombo.getValue();
        double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
        double maxVal = 0.001;

        for (H3Cell c : currentCells) {
            if (c.getLatitude() != null) {
                minLat = Math.min(minLat, c.getLatitude());
                maxLat = Math.max(maxLat, c.getLatitude());
            }
            if (c.getLongitude() != null) {
                minLng = Math.min(minLng, c.getLongitude());
                maxLng = Math.max(maxLng, c.getLongitude());
            }
            double val = getCellValue(c, mode);
            maxVal = Math.max(maxVal, val);
        }

        double latRange = Math.max(0.01, maxLat - minLat);
        double lngRange = Math.max(0.01, maxLng - minLng);

        // Render spatial heat points
        for (H3Cell c : currentCells) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double x = ((c.getLongitude() - minLng) / lngRange) * (w - 20) + 10;
            double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 20) + 10;

            double val = getCellValue(c, mode);
            double ratio = Math.clamp(val / maxVal, 0.0, 1.0);

            Color color = getColorForRatio(ratio, mode);
            gc.setFill(color);
            double size = Math.max(4, 4 + ratio * 6);
            gc.fillOval(x - size / 2, y - size / 2, size, size);
        }

        // Compute Moran's I Live
        double moranI = computeMoranI(mode, minLat, maxLat, minLng, maxLng);
        String desc = moranI > 0.3 ? "Clusters Concentrés (Agrégation)" : (moranI < -0.1 ? "Dispersion Spatiale" : "Répartition Aléatoire");
        lblMoranI.setText(String.format(Locale.US, "I de Moran (Autocorrélation) : %.3f (%s)", moranI, desc));
    }

    private double getCellValue(H3Cell c, String mode) {
        if (mode.contains("Richesse")) {
            double pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            return pop * tech * 15.0;
        } else if (mode.contains("Linguistique")) {
            return c.getLanguageGroup() != null ? Math.abs(c.getLanguageGroup().hashCode() % 100) : 1.0;
        } else if (mode.contains("Température")) {
            return c.getTemperature() != null ? c.getTemperature() : 15.0;
        } else {
            return c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
        }
    }

    private Color getColorForRatio(double ratio, String mode) {
        if (mode.contains("Linguistique")) {
            return Color.hsb(ratio * 360, 0.8, 0.9);
        } else if (mode.contains("Température")) {
            return Color.color(ratio, 0.3, 1.0 - ratio);
        } else {
            // Yellow to Red heat
            return Color.color(Math.min(1.0, ratio * 1.5), Math.max(0.1, 0.9 - ratio * 0.8), 0.1);
        }
    }

    private double computeMoranI(String mode, double minLat, double maxLat, double minLng, double maxLng) {
        if (currentCells.size() < 4) return 0.0;
        double sum = 0;
        for (H3Cell c : currentCells) sum += getCellValue(c, mode);
        double mean = sum / currentCells.size();

        double num = 0, denom = 0;
        int n = currentCells.size();

        for (int i = 0; i < n; i++) {
            double zi = getCellValue(currentCells.get(i), mode) - mean;
            denom += zi * zi;
            for (int j = i + 1; j < Math.min(n, i + 10); j++) {
                double zj = getCellValue(currentCells.get(j), mode) - mean;
                num += zi * zj;
            }
        }
        return denom > 0 ? Math.clamp(num / denom, -1.0, 1.0) : 0.0;
    }

    private void setupAnimation() {
        animationTimeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) return;
            NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();
            List<Long> keys = new ArrayList<>(snapshots.keySet());
            if (keys.isEmpty()) return;

            currentSnapshotIndex = (currentSnapshotIndex + 1) % keys.size();
            long tickKey = keys.get(currentSnapshotIndex);
            this.currentCells = snapshots.get(tickKey);
            renderMap();
        }));
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void toggleAnimation() {
        if (isPlayingAnimation) {
            animationTimeline.pause();
            isPlayingAnimation = false;
            playPauseBtn.setText("▶ Jouer Séquence Temporelle (2D+1D)");
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        } else {
            if (historyManager != null && !historyManager.getWorldSnapshots().isEmpty()) {
                animationTimeline.play();
                isPlayingAnimation = true;
                playPauseBtn.setText("⏸ Pause Animation");
                playPauseBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
            } else {
                lblMoranI.setText("Aucun snapshot disponible pour l'animation temporelle 2D+1D.");
            }
        }
    }
}
