/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Color legend component showing gradient scale for current display mode.
 * Positioned in bottom-left corner of the map.
 */
public class ColorLegend extends javafx.scene.layout.HBox {

    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 15;

    private final Canvas gradientCanvas;
    private DisplayMode currentMode = DisplayMode.BIOME;

    public ColorLegend() {
        setPadding(new Insets(5));
        setStyle("-fx-background-color: rgba(30, 30, 30, 0.8);" +
                "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;");
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setSpacing(10);

        gradientCanvas = new Canvas(BAR_WIDTH + 40, BAR_HEIGHT + 20); // Extra space for text
        getChildren().add(gradientCanvas);

        drawLegend();
    }

    /**
     * Update legend for current display mode.
     */
    public void setDisplayMode(DisplayMode mode) {
        this.currentMode = mode;
        drawLegend();
    }

    private void drawLegend() {
        GraphicsContext gc = gradientCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gradientCanvas.getWidth(), gradientCanvas.getHeight());

        gc.setFont(Font.font("Segoe UI", 10));
        gc.setFill(Color.WHITE);

        if (currentMode == DisplayMode.BIOME) {
            drawBiomeLegendHover(gc);
        } else {
            drawGradientLegend(gc);
        }
    }

    private void drawGradientLegend(GraphicsContext gc) {
        // Draw horizontal gradient
        for (int x = 0; x < BAR_WIDTH; x++) {
            double value = x / (double) BAR_WIDTH;
            Color color = getGradientColor(value);
            gc.setStroke(color);
            gc.strokeLine(x, 0, x, BAR_HEIGHT);
        }

        // Labels
        String[] labels = getLabels(); // Low, Mid, High
        gc.fillText(labels[2], 0, BAR_HEIGHT + 12);
        gc.fillText(labels[1], BAR_WIDTH / 2 - 10, BAR_HEIGHT + 12);
        gc.fillText(labels[0], BAR_WIDTH - 20, BAR_HEIGHT + 12);

        // Title
        gc.fillText(currentMode.getDisplayName(), BAR_WIDTH + 5, 12);
    }

    private void drawBiomeLegendHover(GraphicsContext gc) {
        // Compact Biome Legend: Just render standard "Earth" colors explanation
        // Or keep it simple. Let's draw small squares horizontally.

        String[] biomeNames = { "Ocean", "Plains", "Forest", "Desert", "Snow" };
        Color[] biomeColors = {
                Color.rgb(25, 50, 150),
                Color.rgb(124, 252, 0),
                Color.rgb(34, 139, 34),
                Color.rgb(237, 201, 175),
                Color.rgb(255, 250, 250)
        };

        double x = 0;
        for (int i = 0; i < biomeNames.length; i++) {
            gc.setFill(biomeColors[i]);
            gc.fillRect(x, 2, 12, 12);
            gc.setStroke(Color.GRAY);
            gc.strokeRect(x, 2, 12, 12);

            gc.setFill(Color.WHITE);
            gc.fillText(biomeNames[i], x + 15, 12);

            x += 60; // Spacing
        }
    }

    private Color getGradientColor(double normalized) {
        normalized = Math.max(0, Math.min(1.0, normalized));

        return switch (currentMode) {
            case POPULATION -> getPopulationGradient(normalized);
            case FOOD -> getFoodGradient(normalized);
            case TEMPERATURE -> getTemperatureGradient(normalized);
            default -> Color.GRAY;
        };
    }

    private Color getPopulationGradient(double normalized) {
        // Blue -> Cyan -> Green -> Yellow -> Red
        if (normalized < 0.25) {
            double t = normalized / 0.25;
            return Color.rgb(0, (int) (t * 255), 255);
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, 255, (int) (255 * (1 - t)));
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, 0);
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0);
        }
    }

    private Color getFoodGradient(double normalized) {
        // Brown -> Yellow-green -> Bright green
        if (normalized < 0.5) {
            double t = normalized / 0.5;
            return Color.rgb((int) (139 - t * 100), (int) (69 + t * 186), 19);
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (39 * (1 - t)), (int) (139 + t * 116), (int) (34 * (1 - t)));
        }
    }

    private Color getTemperatureGradient(double normalized) {
        // Cold blue -> Warm red
        if (normalized < 0.25) {
            return Color.rgb(0, 0, (int) (128 + normalized * 4 * 127));
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, (int) (t * 255), 255);
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, (int) (255 * (1 - t)));
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0);
        }
    }

    private String[] getLabels() {
        return switch (currentMode) {
            case POPULATION -> new String[] { "500+", "50", "0" };
            case FOOD -> new String[] { "800", "400", "0" };
            case TEMPERATURE -> new String[] { "45Â°C", "10Â°C", "-30Â°C" };
            default -> new String[] { "High", "Mid", "Low" };
        };
    }
}
