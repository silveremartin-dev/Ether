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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

import java.util.Arrays;
import java.util.List;

/**
 * Enhanced Color Legend component displaying dynamic map statistics (Min, Max, Mean μ, Median M)
 * with distinct visual indicators (Solid Cyan for Mean vs Dashed Amber for Median).
 * Positioned in bottom-right corner of the map stack.
 */
public class ColorLegend extends VBox {

    private static final int CANVAS_WIDTH = 310;
    private static final int CANVAS_HEIGHT = 58;
    private static final double BAR_X = 12.0;
    private static final double BAR_Y = 14.0;
    private static final double BAR_WIDTH = CANVAS_WIDTH - 24.0; // 286.0
    private static final double BAR_HEIGHT = 16.0;

    private final Label titleLabel;
    private final Canvas gradientCanvas;
    private final Label meanBadge;
    private final Label medianBadge;
    private DisplayMode currentMode = DisplayMode.BIOME;

    private double minVal = 0.0;
    private double maxVal = 1.0;
    private double meanVal = 0.0;
    private double medianVal = 0.0;

    public ColorLegend() {
        setSpacing(4);
        setPadding(new Insets(8, 12, 8, 12));
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.92); " +
                "-fx-border-color: rgba(56, 189, 248, 0.45); " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        titleLabel = new Label();
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.web("#38bdf8"));

        gradientCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Sub-badges row for clear Mean vs Median differentiation
        meanBadge = new Label();
        meanBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        meanBadge.setTextFill(Color.web("#38bdf8")); // Cyan

        medianBadge = new Label();
        medianBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        medianBadge.setTextFill(Color.web("#f59e0b")); // Amber

        HBox statsBadgeRow = new HBox(12, meanBadge, medianBadge);
        statsBadgeRow.setAlignment(Pos.CENTER);

        getChildren().addAll(titleLabel, gradientCanvas, statsBadgeRow);
        updateTitle();
        drawLegend();
    }

    public void setDisplayMode(DisplayMode mode) {
        if (mode != null) {
            this.currentMode = mode;
            updateTitle();
            drawLegend();
        }
    }

    public DisplayMode getDisplayMode() {
        return currentMode;
    }

    public void updateStats(double min, double max, double mean, double median) {
        this.minVal = min;
        this.maxVal = max;
        this.meanVal = mean;
        this.medianVal = median;

        meanBadge.setText(String.format("▲ μ (%s) : %.1f", I18n.getOrDefault("sim.legend.mean", "Moyenne"), meanVal));
        medianBadge.setText(String.format("▼ M (%s) : %.1f", I18n.getOrDefault("sim.legend.median", "Médiane"), medianVal));

        drawLegend();
    }

    public void updateFromCanvas(H3MapCanvas mapCanvas) {
        if (mapCanvas == null) return;

        DisplayMode mode = mapCanvas.getDisplayMode();
        if (mode != currentMode) {
            setDisplayMode(mode);
        }

        List<H3Cell> cells = mapCanvas.getCells();
        if (cells == null || cells.isEmpty()) {
            updateStats(0.0, 1.0, 0.0, 0.0);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0.0;
        double[] values = new double[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            double v = mapCanvas.getCellDisplayValue(cells.get(i), i);
            values[i] = v;
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }

        if (min == Double.MAX_VALUE) min = 0.0;
        if (max == -Double.MAX_VALUE) max = 1.0;
        double mean = sum / cells.size();

        Arrays.sort(values);
        double median = (values.length % 2 == 0)
                ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                : values[values.length / 2];

        updateStats(min, max, mean, median);
    }

    private void updateTitle() {
        String modeName = currentMode != null ? currentMode.getDisplayName().toUpperCase() : "BIOME";
        String titlePrefix = I18n.getOrDefault("sim.legend.title", "LÉGENDE — ");
        titleLabel.setText("📊 " + titlePrefix + modeName);
    }

    private void drawLegend() {
        GraphicsContext gc = gradientCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gradientCanvas.getWidth(), gradientCanvas.getHeight());

        if (currentMode == DisplayMode.BIOME) {
            drawBiomeLegend(gc);
            return;
        }

        // 1. Draw Spectrum Bar Gradient
        LinearGradient grad = new LinearGradient(
                BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(30, 58, 138)),
                new Stop(0.25, Color.rgb(6, 182, 212)),
                new Stop(0.50, Color.rgb(34, 197, 94)),
                new Stop(0.75, Color.rgb(234, 179, 8)),
                new Stop(1.00, Color.rgb(239, 68, 68))
        );

        gc.setFill(grad);
        gc.fillRoundRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.4));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, 4, 4);

        double range = Math.max(1e-6, maxVal - minVal);

        // 2. Mean Indicator (Solid Cyan ▲ pointing down + Solid Vertical Line)
        double meanNorm = Math.max(0.0, Math.min(1.0, (meanVal - minVal) / range));
        double meanX = BAR_X + meanNorm * BAR_WIDTH;

        gc.setFill(Color.web("#38bdf8")); // Cyan
        gc.fillPolygon(new double[]{meanX - 4, meanX + 4, meanX}, new double[]{BAR_Y - 6, BAR_Y - 6, BAR_Y}, 3);

        gc.setStroke(Color.web("#38bdf8"));
        gc.setLineWidth(1.8);
        gc.strokeLine(meanX, BAR_Y, meanX, BAR_Y + BAR_HEIGHT);

        // 3. Median Indicator (Dashed Amber ▼ pointing up + Dashed Vertical Line)
        double medNorm = Math.max(0.0, Math.min(1.0, (medianVal - minVal) / range));
        double medX = BAR_X + medNorm * BAR_WIDTH;

        gc.setFill(Color.web("#f59e0b")); // Amber
        gc.fillPolygon(new double[]{medX - 4, medX + 4, medX}, new double[]{BAR_Y + BAR_HEIGHT + 6, BAR_Y + BAR_HEIGHT + 6, BAR_Y + BAR_HEIGHT}, 3);

        gc.setStroke(Color.web("#f59e0b"));
        gc.setLineWidth(1.8);
        gc.setLineDashes(3.0, 3.0); // Dashed line to clearly distinguish median from solid mean
        gc.strokeLine(medX, BAR_Y, medX, BAR_Y + BAR_HEIGHT);
        gc.setLineDashes(null); // Reset dash style

        // 4. Text Labels below bar (Min, Mean, Median, Max)
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));

        // Min (Left)
        gc.setFill(Color.web("#94a3b8"));
        String minStr = String.format("Min: %.1f", minVal);
        gc.fillText(minStr, BAR_X, BAR_Y + BAR_HEIGHT + 18);

        // Max (Right)
        gc.setFill(Color.web("#f87171"));
        String maxStr = String.format("Max: %.1f", maxVal);
        double maxW = maxStr.length() * 6.2;
        gc.fillText(maxStr, BAR_X + BAR_WIDTH - maxW, BAR_Y + BAR_HEIGHT + 18);
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
        double y = 14.0;
        gc.setFont(Font.font("Segoe UI", 10));

        for (int i = 0; i < biomeNames.length; i++) {
            gc.setFill(biomeColors[i]);
            gc.fillRect(x, y, 12, 12);
            gc.setStroke(Color.rgb(255, 255, 255, 0.4));
            gc.strokeRect(x, y, 12, 12);

            gc.setFill(Color.rgb(226, 232, 240));
            gc.fillText(biomeNames[i], x + 15, y + 10);

            x += 56;
        }

        meanBadge.setText("");
        medianBadge.setText("");
    }
}
