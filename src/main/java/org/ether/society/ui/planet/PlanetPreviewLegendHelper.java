/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;

/**
 * Cartographic legend builder for planetary heightmap, temperature, precipitation, and seasonality.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PlanetPreviewLegendHelper {

    private PlanetPreviewLegendHelper() {
    }

    public static void updateLegend(HBox legendBar, PlanetPreset preset, int selectedIdx) {
        if (legendBar == null || preset == null) return;
        legendBar.getChildren().clear();

        if (selectedIdx == 0) { // Heightmap
            double minAlt = preset.minAltitudeMeters();
            double maxAlt = preset.maxAltitudeMeters();
            Label minLabel = new Label(String.format("Min: %,.0f m", minAlt));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(minLabel);

            if (preset.waterLevel() > -0.4) {
                addLegendItem(legendBar, "OCEAN", Color.rgb(15, 23, 42), I18n.getOrDefault("planet.legend.depressions", "Abysses / Océans"));
            } else {
                addLegendItem(legendBar, "DEPRESSIONS", PlanetColorPaletteHelper.getHypsometricColor(0.0), I18n.getOrDefault("planet.legend.depressions", "Dépressions / Abysses"));
            }
            addLegendItem(legendBar, "LOWLANDS", PlanetColorPaletteHelper.getHypsometricColor(0.10), I18n.getOrDefault("planet.legend.lowlands", "Basses Terres"));
            addLegendItem(legendBar, "PLAINS", PlanetColorPaletteHelper.getHypsometricColor(0.35), I18n.getOrDefault("planet.legend.plains", "Plaines & Bassins"));
            addLegendItem(legendBar, "PLATEAUS", PlanetColorPaletteHelper.getHypsometricColor(0.55), I18n.getOrDefault("planet.legend.highlands", "Plateaux & Collines"));
            addLegendItem(legendBar, "HIGHLANDS", PlanetColorPaletteHelper.getHypsometricColor(0.75), I18n.getOrDefault("planet.legend.mountains", "Hauts Reliefs"));
            addLegendItem(legendBar, "PEAKS", PlanetColorPaletteHelper.getHypsometricColor(1.00), I18n.getOrDefault("planet.legend.peaks", "Sommets & Neiges"));

            Label maxLabel = new Label(String.format("Max: %,.0f m", maxAlt));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(maxLabel);
        } else if (selectedIdx == 1) { // Temperature
            double minT = preset.averageTempC() - preset.temperatureGradient() - 20.0;
            double maxT = preset.averageTempC() + preset.temperatureGradient();
            Label minLabel = new Label(String.format("Min: %.1f °C", minT));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(minLabel);

            addLegendItem(legendBar, "POLAR", Color.rgb(40, 0, 120), I18n.getOrDefault("planet.legend.polar", "Polar / Gel"));
            addLegendItem(legendBar, "MILD", Color.rgb(0, 200, 180), I18n.getOrDefault("planet.legend.mild", "Mild"));
            addLegendItem(legendBar, "WARM", Color.rgb(240, 200, 0), I18n.getOrDefault("planet.legend.warm", "Chaud"));
            addLegendItem(legendBar, "HOT", Color.rgb(220, 0, 40), I18n.getOrDefault("planet.legend.hot", "Canicule"));

            Label maxLabel = new Label(String.format("Max: %.1f °C", maxT));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444;");
            legendBar.getChildren().add(maxLabel);
        } else if (selectedIdx == 2) { // Precipitation
            Label minLabel = new Label(I18n.getOrDefault("planet.legend.min_arid", "Min: 0 mm/an (Aride)"));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #eab308;");
            legendBar.getChildren().add(minLabel);

            addLegendItem(legendBar, "ARID", Color.rgb(210, 170, 110), I18n.getOrDefault("planet.legend.arid", "Aride"));
            addLegendItem(legendBar, "MODERATE", Color.rgb(60, 180, 80), I18n.getOrDefault("planet.legend.moderate", "Moderate"));
            addLegendItem(legendBar, "HUMID", Color.rgb(20, 160, 180), I18n.getOrDefault("planet.legend.humid", "Humide"));
            addLegendItem(legendBar, "HEAVY", Color.rgb(10, 60, 200), I18n.getOrDefault("planet.legend.deluge", "Deluge"));

            Label maxLabel = new Label(I18n.getOrDefault("planet.legend.max_humid", "Max: 3 000 mm/an (Humide)"));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6;");
            legendBar.getChildren().add(maxLabel);
        } else { // Seasonality
            double maxAmp = Math.min(60.0, (preset.axialTiltDegrees() / 23.5) * 35.0);
            Label minLabel = new Label(I18n.getOrDefault("planet.legend.min_stable", "Min: 0.0 °C (Stable)"));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(minLabel);

            addLegendItem(legendBar, "STABLE", Color.rgb(30, 80, 140), I18n.getOrDefault("planet.legend.low_variance", "Faible Variance"));
            addLegendItem(legendBar, "SEASONAL", Color.rgb(80, 180, 100), I18n.getOrDefault("planet.legend.seasonal", "Seasonal"));
            addLegendItem(legendBar, "HIGH", Color.rgb(240, 140, 20), I18n.getOrDefault("planet.legend.high_variance", "High"));
            addLegendItem(legendBar, "EXTREME", Color.rgb(220, 30, 80), I18n.getOrDefault("planet.legend.extreme_amp", "Extreme Amplitude"));

            Label maxLabel = new Label(String.format("%s: %.1f °C (%s)", I18n.getOrDefault("planet.short.max", "Max"), maxAmp, I18n.getOrDefault("planet.legend.amplitude", "Amplitude")));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a855f7;");
            legendBar.getChildren().add(maxLabel);
        }
    }

    private static void addLegendItem(HBox legendBar, String id, Color col, String text) {
        Pane colorSwatch = new Pane();
        colorSwatch.setPrefSize(14, 14);
        colorSwatch.setStyle(String.format("-fx-background-color: rgba(%d,%d,%d,1); -fx-border-color: #ffffff; -fx-border-width: 1; -fx-background-radius: 3;",
                (int)(col.getRed()*255), (int)(col.getGreen()*255), (int)(col.getBlue()*255)));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("control-label");
        HBox itemBox = new HBox(4, colorSwatch, lbl);
        itemBox.setAlignment(Pos.CENTER);
        legendBar.getChildren().add(itemBox);
    }
}
