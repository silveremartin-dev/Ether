/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

import java.util.*;

/**
 * Enhanced GIS-grade Multi-Layer Color Legend component.
 * Displays stacked, responsive statistical legend blocks for all active map layers
 * (Min, Max, Mean μ, Median M) with distinct indicators (Cyan for Mean, Dashed Amber for Median)
 * and interactive folding/collapse support.
 */
public class ColorLegend extends VBox {

    private static final int CANVAS_WIDTH = 220;
    private static final int CANVAS_HEIGHT = 38;
    private static final double BAR_X = 6.0;
    private static final double BAR_Y = 6.0;
    private static final double BAR_WIDTH = CANVAS_WIDTH - 12.0; // 208.0
    private static final double BAR_HEIGHT = 10.0;

    private final HBox headerRow;
    private final Label titleLabel;
    private final Button toggleCollapseBtn;
    private final VBox layersContainer;

    private Set<DisplayMode> activeModes = new LinkedHashSet<>(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
    private DisplayMode primaryMode = DisplayMode.POPULATION;
    private ScientificColorMap scientificColorMap = ScientificColorMap.TURBO;

    private boolean collapsed = false;

    // Cache per-mode stats to avoid reallocations
    private static class ModeStats {
        double min = 0.0;
        double max = 1.0;
        double mean = 0.0;
        double median = 0.0;
    }
    private final Map<DisplayMode, ModeStats> statsMap = new EnumMap<>(DisplayMode.class);

    public ColorLegend() {
        setSpacing(4);
        setPadding(new Insets(6, 8, 6, 8));
        setMaxWidth(CANVAS_WIDTH + 24);
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.88); " +
                "-fx-border-color: rgba(56, 189, 248, 0.45); " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.55), 10, 0, 0, 3);");

        // Header with title and collapse toggle
        titleLabel = new Label();
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        titleLabel.setTextFill(Color.web("#38bdf8"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleCollapseBtn = new Button("−");
        toggleCollapseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 0 4; -fx-cursor: hand;");
        toggleCollapseBtn.setOnAction(e -> toggleCollapse());

        headerRow = new HBox(4, titleLabel, spacer, toggleCollapseBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        layersContainer = new VBox(6);

        getChildren().addAll(headerRow, layersContainer);
        updateTitle();
        rebuildLegendCards();
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        layersContainer.setVisible(!collapsed);
        layersContainer.setManaged(!collapsed);
        toggleCollapseBtn.setText(collapsed ? "+" : "−");
    }

    public void setScientificColorMap(ScientificColorMap cmap) {
        if (cmap != null && cmap != this.scientificColorMap) {
            this.scientificColorMap = cmap;
            rebuildLegendCards();
        }
    }

    public void setDisplayMode(DisplayMode mode) {
        if (mode != null) {
            this.primaryMode = mode;
            this.activeModes = new LinkedHashSet<>(List.of(mode));
            updateTitle();
            rebuildLegendCards();
        }
    }

    public DisplayMode getDisplayMode() {
        return primaryMode;
    }

    public void setActiveDisplayModes(Collection<DisplayMode> modes) {
        if (modes != null) {
            this.activeModes = new LinkedHashSet<>(modes);
            if (!this.activeModes.isEmpty()) {
                this.primaryMode = this.activeModes.iterator().next();
            }
            updateTitle();
            rebuildLegendCards();
        }
    }

    public void updateStats(double min, double max, double mean, double median) {
        ModeStats st = statsMap.computeIfAbsent(primaryMode, k -> new ModeStats());
        st.min = min;
        st.max = max;
        st.mean = mean;
        st.median = median;
        rebuildLegendCards();
    }

    public void updateFromCanvas(H3MapCanvas mapCanvas) {
        if (mapCanvas == null) return;

        Set<DisplayMode> canvasActiveModes = mapCanvas.getActiveDisplayModes();
        if (canvasActiveModes != null && !canvasActiveModes.equals(this.activeModes)) {
            this.activeModes = new LinkedHashSet<>(canvasActiveModes);
            this.primaryMode = mapCanvas.getPrimaryDisplayMode();
            updateTitle();
        }

        ScientificColorMap cmap = mapCanvas.getScientificColorMap();
        if (cmap != null && cmap != scientificColorMap) {
            setScientificColorMap(cmap);
        }

        List<H3Cell> cells = mapCanvas.getCells();
        if (cells == null || cells.isEmpty()) {
            statsMap.clear();
            rebuildLegendCards();
            return;
        }

        int cellCount = cells.size();
        for (DisplayMode mode : activeModes) {
            if (mode == DisplayMode.BIOME) continue;

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double sum = 0.0;
            double[] values = new double[cellCount];

            for (int i = 0; i < cellCount; i++) {
                double v = mapCanvas.getCellDisplayValue(cells.get(i), mode, i);
                values[i] = v;
                if (v < min) min = v;
                if (v > max) max = v;
                sum += v;
            }

            if (min == Double.MAX_VALUE) min = 0.0;
            if (max == -Double.MAX_VALUE) max = 1.0;
            double mean = sum / cellCount;

            Arrays.sort(values);
            double median = (values.length % 2 == 0)
                    ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                    : values[values.length / 2];

            ModeStats st = statsMap.computeIfAbsent(mode, k -> new ModeStats());
            st.min = min;
            st.max = max;
            st.mean = mean;
            st.median = median;
        }

        rebuildLegendCards();
    }

    private void updateTitle() {
        int count = activeModes != null ? activeModes.size() : 0;
        String titlePrefix = I18n.getOrDefault("sim.legend.title", "LÉGENDE SIG");
        if (count > 1) {
            titleLabel.setText(String.format("📊 %s (%d %s)", titlePrefix, count, I18n.getOrDefault("sim.legend.layers", "calques")));
        } else if (count == 1) {
            DisplayMode single = activeModes.iterator().next();
            titleLabel.setText("📊 " + titlePrefix + " — " + single.getDisplayName().toUpperCase());
        } else {
            titleLabel.setText("📊 " + titlePrefix);
        }
    }

    private void rebuildLegendCards() {
        layersContainer.getChildren().clear();

        if (activeModes == null || activeModes.isEmpty()) {
            Label noLayer = new Label(I18n.getOrDefault("sim.layer.none", "Aucun calque"));
            noLayer.setFont(Font.font("Segoe UI", 9));
            noLayer.setTextFill(Color.web("#94a3b8"));
            layersContainer.getChildren().add(noLayer);
            return;
        }

        for (DisplayMode mode : activeModes) {
            VBox card = createCardForMode(mode);
            layersContainer.getChildren().add(card);
        }
    }

    private VBox createCardForMode(DisplayMode mode) {
        VBox card = new VBox(2);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.65); -fx-padding: 4 6; -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.20); -fx-border-radius: 6;");

        // Card header with mode name and stats summary
        Label modeTitle = new Label(mode.getDisplayName());
        modeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        modeTitle.setTextFill(Color.web("#e2e8f0"));

        if (mode == DisplayMode.BIOME) {
            Canvas biomeCanvas = new Canvas(CANVAS_WIDTH, 20);
            drawBiomeLegend(biomeCanvas.getGraphicsContext2D());
            card.getChildren().addAll(modeTitle, biomeCanvas);
            return card;
        }

        ModeStats st = statsMap.getOrDefault(mode, new ModeStats());
        String unit = getUnitForMode(mode);

        Label statsBadge = new Label(String.format("▲ μ: %.1f%s  ▼ M: %.1f%s", st.mean, unit, st.median, unit));
        statsBadge.setFont(Font.font("Segoe UI", 8.5));
        statsBadge.setTextFill(Color.web("#38bdf8"));

        HBox cardTopRow = new HBox(4, modeTitle);
        cardTopRow.setAlignment(Pos.CENTER_LEFT);

        Canvas gradCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        drawGradientBar(gradCanvas.getGraphicsContext2D(), st, unit);

        card.getChildren().addAll(cardTopRow, statsBadge, gradCanvas);
        return card;
    }

    private String getUnitForMode(DisplayMode mode) {
        if (mode == null) return "";
        return switch (mode) {
            case POPULATION -> " hab";
            case TEMPERATURE -> "°C";
            case PRECIPITATION -> " mm";
            case INEQUALITY -> "";
            case TECHNOLOGY -> " lvl";
            case WATER, WOOD -> " t";
            default -> "";
        };
    }

    private void drawGradientBar(GraphicsContext gc, ModeStats st, String unit) {
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 1. Draw Spectrum Bar Gradient based on current ScientificColorMap
        List<Stop> stops = new ArrayList<>();
        int stopCount = 8;
        for (int s = 0; s <= stopCount; s++) {
            double t = (double) s / stopCount;
            Color sc = (scientificColorMap != null) ? scientificColorMap.getColor(t) : Color.color(t, 1.0 - t, 0.5);
            stops.add(new Stop(t, sc));
        }

        LinearGradient grad = new LinearGradient(
                BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y, false, CycleMethod.NO_CYCLE, stops
        );

        gc.setFill(grad);
        gc.fillRoundRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, 3, 3);
        gc.setStroke(Color.rgb(255, 255, 255, 0.35));
        gc.setLineWidth(0.8);
        gc.strokeRoundRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, 3, 3);

        double range = Math.max(1e-6, st.max - st.min);

        // 2. Mean Indicator (Solid Cyan ▲ pointing down + Solid Vertical Line)
        double meanNorm = Math.max(0.0, Math.min(1.0, (st.mean - st.min) / range));
        double meanX = BAR_X + meanNorm * BAR_WIDTH;

        gc.setFill(Color.web("#38bdf8")); // Cyan
        gc.fillPolygon(new double[]{meanX - 2.5, meanX + 2.5, meanX}, new double[]{BAR_Y - 4, BAR_Y - 4, BAR_Y}, 3);

        gc.setStroke(Color.web("#38bdf8"));
        gc.setLineWidth(1.2);
        gc.strokeLine(meanX, BAR_Y, meanX, BAR_Y + BAR_HEIGHT);

        // 3. Median Indicator (Dashed Amber ▼ pointing up + Dashed Vertical Line)
        double medNorm = Math.max(0.0, Math.min(1.0, (st.median - st.min) / range));
        double medX = BAR_X + medNorm * BAR_WIDTH;

        gc.setFill(Color.web("#f59e0b")); // Amber
        gc.fillPolygon(new double[]{medX - 2.5, medX + 2.5, medX}, new double[]{BAR_Y + BAR_HEIGHT + 4, BAR_Y + BAR_HEIGHT + 4, BAR_Y + BAR_HEIGHT}, 3);

        gc.setStroke(Color.web("#f59e0b"));
        gc.setLineWidth(1.2);
        gc.setLineDashes(2.0, 2.0);
        gc.strokeLine(medX, BAR_Y, medX, BAR_Y + BAR_HEIGHT);
        gc.setLineDashes(null);

        // 4. Text Labels below bar (Min, Max)
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8.5));

        // Min (Left)
        gc.setFill(Color.web("#94a3b8"));
        String minStr = String.format("Min: %.1f%s", st.min, unit);
        gc.fillText(minStr, BAR_X, BAR_Y + BAR_HEIGHT + 13);

        // Max (Right)
        gc.setFill(Color.web("#f87171"));
        String maxStr = String.format("Max: %.1f%s", st.max, unit);
        double maxW = maxStr.length() * 5.0;
        gc.fillText(maxStr, BAR_X + BAR_WIDTH - maxW, BAR_Y + BAR_HEIGHT + 13);
    }

    private void drawBiomeLegend(GraphicsContext gc) {
        String[] biomeNames = {
            I18n.getBiomeDisplayName(org.ether.society.model.Biome.OCEAN),
            I18n.getBiomeDisplayName(org.ether.society.model.Biome.PLAINS),
            I18n.getBiomeDisplayName(org.ether.society.model.Biome.FOREST),
            I18n.getBiomeDisplayName(org.ether.society.model.Biome.DESERT),
            I18n.getBiomeDisplayName(org.ether.society.model.Biome.SNOW)
        };
        Color[] biomeColors = {
                Color.rgb(25, 50, 150),
                Color.rgb(124, 252, 0),
                Color.rgb(34, 139, 34),
                Color.rgb(237, 201, 175),
                Color.rgb(255, 250, 250)
        };

        double x = BAR_X;
        double y = 4.0;
        gc.setFont(Font.font("Segoe UI", 8.5));

        for (int i = 0; i < biomeNames.length; i++) {
            gc.setFill(biomeColors[i]);
            gc.fillRect(x, y, 8, 8);
            gc.setStroke(Color.rgb(255, 255, 255, 0.4));
            gc.strokeRect(x, y, 8, 8);

            gc.setFill(Color.rgb(226, 232, 240));
            String name = biomeNames[i];
            if (name.length() > 5) name = name.substring(0, 4) + ".";
            gc.fillText(name, x + 10, y + 8);

            x += 41;
        }
    }
}

