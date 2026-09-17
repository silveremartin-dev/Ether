package org.ether.society.ui;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.*;

/**
 * 2D & 2D+1D Spatial Heatmap & Animated Temporal Map Component for StatsPanel.
 * Renders spatial density maps (Population, Wealth, Culture/Language, Climate)
 * superimposed over real planetary terrain/biome geography and animates historical
 * snapshot sequences over time (2D + 1D Temporal).
 * Computes Moran's I spatial autocorrelation index live.
 *
 * @author Silvere Martin-Michiellot
 */
public class SpatialHeatmapPanel extends VBox {

    private final Label titleLabel;
    private final Label dateLabel;
    private final ComboBox<String> mapTypeCombo;
    private final Canvas mapCanvas;
    private final Label lblMoranI;
    private final Button playPauseBtn;

    // Hover popup – enlarged map (560×280)
    private final Popup hoverPopup = new Popup();
    private final Canvas popupCanvas = new Canvas(560, 280);
    private Timeline popupHideTimer;

    private List<H3Cell> currentCells = new ArrayList<>();
    private HistoryManager historyManager;
    private Timeline animationTimeline;
    private boolean isPlayingAnimation = false;
    private int currentSnapshotIndex = 0;

    public SpatialHeatmapPanel() {
        setPadding(new Insets(8));
        setSpacing(6);
        getStyleClass().add("card-section");

        // Header with Title & Date Badge (Date placed cleanly below title to prevent truncation)
        titleLabel = new Label();
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        dateLabel = new Label("");
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        VBox headerBox = new VBox(2, titleLabel, dateLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Controls
        mapTypeCombo = new ComboBox<>();
        mapTypeCombo.setMaxWidth(Double.MAX_VALUE);
        mapTypeCombo.setStyle("-fx-font-size: 10px;");
        mapTypeCombo.setOnAction(e -> renderMap());

        playPauseBtn = new Button();
        playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        playPauseBtn.setOnAction(e -> toggleAnimation());

        HBox controlBox = new HBox(8, mapTypeCombo, playPauseBtn);
        HBox.setHgrow(mapTypeCombo, Priority.ALWAYS);
        controlBox.setAlignment(Pos.CENTER_LEFT);

        // Map Canvas (320x140)
        mapCanvas = new Canvas(320, 140);

        // Spatial Autocorrelation Label
        lblMoranI = new Label();
        lblMoranI.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        getChildren().addAll(headerBox, controlBox, mapCanvas, lblMoranI);

        setupPopup();
        setupAnimation();
        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    /** Builds the floating hover popup containing a 560×280 enlarged canvas. */
    private void setupPopup() {
        // Styled container for the popup canvas
        VBox popupBox = new VBox(0, popupCanvas);
        popupBox.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-border-color: #38bdf8;" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;"
        );
        popupBox.setPadding(new Insets(0));
        hoverPopup.getContent().add(popupBox);
        hoverPopup.setAutoHide(false);

        // Show popup when mouse enters the small canvas
        mapCanvas.setOnMouseEntered(e -> {
            cancelHideTimer();
            // Position the popup just to the right of the main canvas
            javafx.geometry.Bounds bounds = mapCanvas.localToScreen(mapCanvas.getBoundsInLocal());
            if (bounds != null) {
                double popupX = bounds.getMaxX() + 8;
                double popupY = bounds.getMinY();
                // Prevent going off-screen on the right – nudge left if needed
                javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
                if (popupX + 560 > screen.getMaxX()) {
                    popupX = bounds.getMinX() - 560 - 8;
                }
                hoverPopup.show(mapCanvas, popupX, popupY);
                renderOnPopupCanvas();
            }
        });

        // Start hide-timer when mouse leaves the small canvas
        mapCanvas.setOnMouseExited(e -> scheduleHidePopup());

        // Cancel hide-timer when mouse enters the popup itself
        popupCanvas.setOnMouseEntered(e -> cancelHideTimer());

        // Hide when mouse leaves the popup canvas
        popupCanvas.setOnMouseExited(e -> scheduleHidePopup());
    }

    private void scheduleHidePopup() {
        cancelHideTimer();
        popupHideTimer = new Timeline(new KeyFrame(Duration.millis(200), ev -> hoverPopup.hide()));
        popupHideTimer.setCycleCount(1);
        popupHideTimer.play();
    }

    private void cancelHideTimer() {
        if (popupHideTimer != null) {
            popupHideTimer.stop();
            popupHideTimer = null;
        }
    }

    /** Renders the enlarged version of the map on the popup canvas. */
    private void renderOnPopupCanvas() {
        renderMapOnCanvas(popupCanvas);
    }


    public void updateTexts() {
        titleLabel.setText(I18n.getOrDefault("heatmap.title", "🗺️ Atlas Dynamique des Territoires"));

        int selIdx = mapTypeCombo.getSelectionModel().getSelectedIndex();
        mapTypeCombo.getItems().clear();
        mapTypeCombo.getItems().addAll(
                I18n.getOrDefault("heatmap.mode.population", "👥 Densité de Population"),
                I18n.getOrDefault("heatmap.mode.wealth", "💎 Richesse & Capital Spécialisé"),
                I18n.getOrDefault("heatmap.mode.language", "🗣️ Diffusion Linguistique & Culturelle"),
                I18n.getOrDefault("heatmap.mode.climate", "🌡️ Température & Biome")
        );
        mapTypeCombo.getSelectionModel().select(selIdx >= 0 ? selIdx : 0);

        if (isPlayingAnimation) {
            playPauseBtn.setText(I18n.getOrDefault("heatmap.btn.pause", "⏸ Pause"));
            playPauseBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        } else {
            playPauseBtn.setText(I18n.getOrDefault("heatmap.btn.play", "▶ Lancer la séquence temporelle (2D+1D)"));
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        }

        renderMap();
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

    public void setDateLabel(String text) {
        if (dateLabel != null) {
            dateLabel.setText(text != null ? text : "");
        }
    }

    /** Renders the map on the small in-panel canvas and, if the popup is showing, refreshes it too. */
    private void renderMap() {
        renderMapOnCanvas(mapCanvas);
        if (hoverPopup.isShowing()) {
            renderMapOnCanvas(popupCanvas);
        }
    }

    /** Core rendering routine – canvas-agnostic so it works for both the small and the popup canvas. */
    private void renderMapOnCanvas(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Canvas dark background
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42, 0.90));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        if (currentCells == null || currentCells.isEmpty()) {
            gc.setFill(Color.web("#64748b"));
            gc.fillText(I18n.getOrDefault("heatmap.no_data", "Aucune donnée cartographique"), w / 4, h / 2);
            lblMoranI.setText(I18n.getOrDefault("heatmap.moran_none", "Indice de Moran (Autocorrélation spatiale) : --"));
            return;
        }

        String mode = mapTypeCombo.getValue();
        if (mode == null) mode = I18n.getOrDefault("heatmap.mode.population", "👥 Densité de Population");

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

        // Keep standard planetary bounds if coordinates span global latitude/longitude
        if (minLat < -60 && maxLat > 60) {
            minLat = -90.0;
            maxLat = 90.0;
        }
        if (minLng < -150 && maxLng > 150) {
            minLng = -180.0;
            maxLng = 180.0;
        }

        double latRange = Math.max(0.01, maxLat - minLat);
        double lngRange = Math.max(0.01, maxLng - minLng);

        // Scale dot size up when drawing on the large popup canvas
        double dotScale = (w > 400) ? 1.8 : 1.0;

        // 1. First Pass: Render Real Planetary Terrain & Biome Geography Base
        for (H3Cell c : currentCells) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
            double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

            Color baseBiomeColor = getBiomeBaseColor(c.getBiome());
            gc.setFill(baseBiomeColor);
            gc.fillOval(x - 2 * dotScale, y - 2 * dotScale, 4 * dotScale, 4 * dotScale);
        }

        // 2. Second Pass: Render Heatmap / Density Overlay
        for (H3Cell c : currentCells) {
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double val = getCellValue(c, mode);
            if (val <= 0 && isPopulationOrWealthMode(mode)) continue;

            double x = ((c.getLongitude() - minLng) / lngRange) * (w - 16) + 8;
            double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (h - 16) + 8;

            double ratio = Math.clamp(val / maxVal, 0.0, 1.0);
            Color heatColor = getColorForRatio(ratio, mode);
            gc.setFill(heatColor);

            double size = Math.max(3.5, 3.5 + ratio * 6.5) * dotScale;
            gc.fillOval(x - size / 2, y - size / 2, size, size);
        }

        // Compute Moran's I Live (only update label from the main canvas render)
        if (canvas == mapCanvas) {
            double moranI = computeMoranI(mode, minLat, maxLat, minLng, maxLng);
            String descKey = moranI > 0.3 ? "heatmap.moran.clusters" : (moranI < -0.1 ? "heatmap.moran.dispersed" : "heatmap.moran.random");
            String descFallback = moranI > 0.3 ? "Clusters Concentrés (Agrégation)" : (moranI < -0.1 ? "Dispersion Spatiale" : "Répartition Aléatoire");
            String desc = I18n.getOrDefault(descKey, descFallback);
            String labelPattern = I18n.getOrDefault("heatmap.moran.label", "Indice de Moran (Autocorrélation) : %.3f (%s)");
            lblMoranI.setText(String.format(Locale.US, labelPattern, moranI, desc));
        }
    }

    private boolean isPopulationOrWealthMode(String mode) {
        if (mode == null) return true;
        String m = mode.toLowerCase();
        return m.contains("pop") || m.contains("wealth") || m.contains("rich") || m.contains("densit");
    }

    private Color getBiomeBaseColor(Biome biome) {
        if (biome == null) return Color.rgb(30, 41, 59, 0.6);
        return switch (biome) {
            case OCEAN -> Color.rgb(14, 45, 80, 0.7);
            case DEEP_OCEAN -> Color.rgb(10, 30, 60, 0.85);
            case BEACH -> Color.rgb(180, 160, 100, 0.8);
            case DESERT -> Color.rgb(160, 110, 50, 0.8);
            case PLAINS -> Color.rgb(45, 95, 45, 0.8);
            case SAVANNAH -> Color.rgb(130, 140, 50, 0.8);
            case FOREST -> Color.rgb(25, 75, 40, 0.85);
            case JUNGLE -> Color.rgb(15, 85, 30, 0.85);
            case MOUNTAINS -> Color.rgb(110, 110, 120, 0.85);
            case HILLS -> Color.rgb(90, 100, 80, 0.8);
            case TUNDRA -> Color.rgb(130, 140, 140, 0.8);
            case SNOW, GLACIER -> Color.rgb(200, 215, 225, 0.85);
            case LAKE -> Color.rgb(20, 70, 120, 0.75);
        };
    }

    private double getCellValue(H3Cell c, String mode) {
        if (mode == null) return 0.0;
        String m = mode.toLowerCase();
        if (m.contains("richesse") || m.contains("wealth") || m.contains("reichtum") || m.contains("riqueza") || m.contains("财富")) {
            double pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            return pop * tech * 15.0;
        } else if (m.contains("linguistique") || m.contains("language") || m.contains("sprache") || m.contains("lingüística") || m.contains("语言")) {
            return c.getLanguageGroup() != null ? Math.abs(c.getLanguageGroup().hashCode() % 100) : 1.0;
        } else if (m.contains("température") || m.contains("temperature") || m.contains("temperatur") || m.contains("temperatura") || m.contains("温度")) {
            return c.getTemperature() != null ? c.getTemperature() : 15.0;
        } else {
            return c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
        }
    }

    private Color getColorForRatio(double ratio, String mode) {
        String m = mode != null ? mode.toLowerCase() : "";
        if (m.contains("linguistique") || m.contains("language") || m.contains("sprache") || m.contains("lingüística") || m.contains("语言")) {
            return Color.hsb(ratio * 360, 0.85, 0.95, 0.85);
        } else if (m.contains("température") || m.contains("temperature") || m.contains("temperatur") || m.contains("temperatura") || m.contains("温度")) {
            return Color.color(ratio, 0.2, 1.0 - ratio, 0.85);
        } else if (m.contains("richesse") || m.contains("wealth")) {
            return Color.color(0.2, Math.min(1.0, 0.4 + ratio * 0.6), Math.min(1.0, ratio * 1.2), 0.88);
        } else {
            // High visibility Demographic Heatmap: Yellow -> Orange -> Crimson Red with radiant alpha
            return Color.color(
                Math.min(1.0, 0.3 + ratio * 0.7),
                Math.max(0.1, 0.9 - ratio * 0.8),
                0.1,
                Math.max(0.4, Math.min(0.95, 0.3 + ratio * 0.65))
            );
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
            if (dateLabel != null) {
                dateLabel.setText(String.format("Tick: %,d (%d/%d)", tickKey, currentSnapshotIndex + 1, keys.size()));
            }
            renderMap();
        }));
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void toggleAnimation() {
        if (isPlayingAnimation) {
            animationTimeline.pause();
            isPlayingAnimation = false;
            playPauseBtn.setText(I18n.getOrDefault("heatmap.btn.play", "▶ Lancer la séquence temporelle (2D+1D)"));
            playPauseBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        } else {
            if (historyManager != null && !historyManager.getWorldSnapshots().isEmpty()) {
                animationTimeline.play();
                isPlayingAnimation = true;
                playPauseBtn.setText(I18n.getOrDefault("heatmap.btn.pause", "⏸ Pause"));
                playPauseBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
            } else {
                lblMoranI.setText(I18n.getOrDefault("heatmap.no_snapshot", "Veuillez lancer la simulation pour animer la séquence temporelle."));
            }
        }
    }
}
