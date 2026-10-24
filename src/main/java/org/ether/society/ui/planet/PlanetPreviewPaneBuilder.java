/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;

/**
 * Autonomous UI Controller and Section Builder for Planetary Dynamic 2D Equirectangular Preview,
 * Interactive Zoom/Pan, Color Palettes, Cartographic Legends, and Cosine-Weighted Ocean Ratios.
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP).
 * Computes cosine-weighted surface area ratios $\int \cos(\phi) \, d\phi$ to provide true spherical
 * ocean vs land percentages decoupled from planar equirectangular distortion.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PlanetPreviewPaneBuilder {

    /** Callback interface defining interaction with the parent panel */
    public interface HostContext {
        PlanetPreset buildPresetFromUI();
        ProceduralGenerator getGenerator();
        Image getCustomElevationImage();
        boolean isElevationImportActive();
        Image getCustomClimateImage();
        boolean isTempImportActive();
        Image getCustomRainfallImage();
        boolean isPrecipImportActive();
        Image getCustomSeasonalityImage();
        boolean isSeasonImportActive();
        long getTempSeed(long baseSeed);
        long getPrecipSeed(long baseSeed);
        long getSeasonSeed(long baseSeed);
        void setBusyCursor(boolean busy);
    }

    private final HostContext context;

    // View Mode & Canvas Controls
    private Label previewTitle;
    private ComboBox<String> viewModeCombo;
    private Canvas previewCanvas;
    private HBox legendBar;
    private Label statsLabel;
    private Label astroLabel;

    // Interactive Zoom/Pan State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX;
    private double dragStartY;

    public PlanetPreviewPaneBuilder(HostContext context) {
        this.context = context;
    }

    public VBox buildPreviewPane() {
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        previewTitle = new Label(I18n.getOrDefault("planet.preview.title", "Dynamic 2D Equirectangular Planetary Preview"));
        previewTitle.getStyleClass().add("label-header");

        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
                I18n.getOrDefault("planet.view.heightmap", "MAP 1. Heightmap & Relief Map"),
                I18n.getOrDefault("planet.view.temperature", "MAP 2. Temperature Map (°C)"),
                I18n.getOrDefault("planet.view.precipitation", "MAP 3. Precipitation Map (mm/yr)"),
                I18n.getOrDefault("planet.view.seasonality", "MAP 4. Seasonality / Thermal Amplitude Map (°C)")
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(0));
        viewModeCombo.setMaxWidth(420);
        viewModeCombo.setOnAction(e -> {
            updateLegend();
            updatePreview();
        });

        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6;");
        previewCanvas = new Canvas(640, 360);
        Tooltip.install(previewCanvas, new Tooltip(I18n.getOrDefault("planet.tooltip.preview", "Dynamic 2D equirectangular preview")));

        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - previewCanvas.getWidth()) >= 4.0) {
                previewCanvas.setWidth(w);
                updatePreview();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - previewCanvas.getHeight()) >= 4.0) {
                previewCanvas.setHeight(h);
                updatePreview();
            }
        });

        previewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * factor));

            if (newZoom != oldZoom) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double w = previewCanvas.getWidth();
                double h = previewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
                double oldScale = baseScale * oldZoom;
                double newScale = baseScale * newZoom;

                if (oldScale > 0 && newScale > 0) {
                    double mouseLng = (mouseX - w / 2.0 - panX) / oldScale;
                    double mouseLat = (h / 2.0 + panY - mouseY) / oldScale;

                    zoomFactor = newZoom;
                    panX = mouseX - w / 2.0 - mouseLng * newScale;
                    panY = mouseY - h / 2.0 + mouseLat * newScale;
                } else {
                    zoomFactor = newZoom;
                }
                updatePreview();
            }
        });
        previewCanvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
        });
        previewCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            panX += dx;
            panY += dy;
            dragStartX = e.getX();
            dragStartY = e.getY();
            updatePreview();
        });
        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                updatePreview();
            }
        });

        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");

        statsLabel = new Label();
        statsLabel.getStyleClass().add("label-stats");

        astroLabel = new Label();
        astroLabel.getStyleClass().add("control-label");

        centerBox.getChildren().addAll(previewTitle, viewModeCombo, canvasContainer, legendBar, statsLabel, astroLabel);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        return centerBox;
    }

    public void updatePreview() {
        if (previewCanvas == null) return;
        context.setBusyCursor(true);
        try {
            updateLegend();

            PlanetPreset preset = context.buildPresetFromUI();
            GraphicsContext gc = previewCanvas.getGraphicsContext2D();

            int canvasW = (int) previewCanvas.getWidth();
            int canvasH = (int) previewCanvas.getHeight();
            if (gc == null || canvasW < 10 || canvasH < 10) return;

            int mapMode = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
            boolean isImportMode = context.isElevationImportActive();
            Image customElevImage = context.getCustomElevationImage();
            PixelReader elevReader = (isImportMode && customElevImage != null) ? customElevImage.getPixelReader() : null;

            long tSeed = context.getTempSeed(preset.seed());
            long pSeed = context.getPrecipSeed(preset.seed());
            long sSeed = context.getSeasonSeed(preset.seed());

            int w = 320;
            int h = 160;

            WritableImage buffer = new WritableImage(w, h);
            PixelWriter pw = buffer.getPixelWriter();

            double wElev = customElevImage != null ? customElevImage.getWidth() : 0;
            double hElev = customElevImage != null ? customElevImage.getHeight() : 0;

            double co2Forcing = 3.0 * (Math.log(Math.max(1.0, preset.co2Ppm()) / 280.0) / Math.log(2.0));
            double pressureBoost = (preset.atmospherePressureAtm() - 1.0) * 8.0;
            double minTemp = preset.averageTempC() + co2Forcing + pressureBoost - preset.temperatureGradient() - 20.0;
            double maxTemp = preset.averageTempC() + co2Forcing + pressureBoost + preset.temperatureGradient();

            int bgArgb = 0xFF0F172A;
            ProceduralGenerator generator = context.getGenerator();

            Image customClimateImage = context.getCustomClimateImage();
            Image customRainfallImage = context.getCustomRainfallImage();
            Image customSeasonalityImage = context.getCustomSeasonalityImage();

            for (int py = 0; py < h; py++) {
                double y_base = (py - h / 2.0 - panY) / zoomFactor + h / 2.0;
                if (y_base < 0 || y_base >= h) {
                    for (int px = 0; px < w; px++) pw.setArgb(px, py, bgArgb);
                    continue;
                }
                double lat = 90.0 - (y_base / (double) h) * 180.0;

                for (int px = 0; px < w; px++) {
                    double x_base = (px - w / 2.0 - panX) / zoomFactor + w / 2.0;
                    if (x_base < 0 || x_base >= w) {
                        pw.setArgb(px, py, bgArgb);
                        continue;
                    }
                    double lng = -180.0 + (x_base / (double) w) * 360.0;

                    Color pxColor;
                    boolean isOcean = false;

                    if (preset.waterLevel() > -0.4) {
                        if (isImportMode && customElevImage != null && elevReader != null) {
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int ex = (int) Math.min(u * wElev, wElev - 1);
                            int ey = (int) Math.min(v * hElev, hElev - 1);
                            Color c = elevReader.getColor(ex, ey);
                            double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            isOcean = (eNorm < preset.waterLevel());
                        } else {
                            var point = generator.getPlanetPoint(lat, lng, preset);
                            isOcean = (point.elevation() < preset.waterLevel());
                        }
                    }

                    if (mapMode == 0) { // Heightmap
                        if (isImportMode) {
                            if (customElevImage != null && elevReader != null && wElev > 0 && hElev > 0) {
                                double u = (lng + 180.0) / 360.0;
                                double v = (90.0 - lat) / 180.0;
                                int ex = (int) Math.min(u * wElev, wElev - 1);
                                int ey = (int) Math.min(v * hElev, hElev - 1);
                                Color c = elevReader.getColor(ex, ey);
                                double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                                if (isOcean) {
                                    pxColor = Color.rgb(15, 23, 42);
                                } else {
                                    double norm = (eNorm - preset.waterLevel()) / (1.0 - preset.waterLevel() + 0.001);
                                    pxColor = getHypsometricColor(norm);
                                }
                            } else {
                                pxColor = Color.BLACK;
                            }
                        } else {
                            if (isOcean) {
                                pxColor = Color.rgb(15, 23, 42);
                            } else {
                                var point = generator.getPlanetPoint(lat, lng, preset);
                                double norm = (point.elevation() - preset.waterLevel()) / (1.0 - preset.waterLevel() + 0.001);
                                pxColor = getHypsometricColor(norm);
                            }
                        }
                    } else if (mapMode == 1) { // Temperature
                        if (context.isTempImportActive() && customClimateImage != null && customClimateImage.getPixelReader() != null && customClimateImage.getWidth() > 0) {
                            PixelReader pr = customClimateImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int tx = (int) Math.min(u * customClimateImage.getWidth(), customClimateImage.getWidth() - 1);
                            int ty = (int) Math.min(v * customClimateImage.getHeight(), customClimateImage.getHeight() - 1);
                            int argb = pr.getArgb(tx, ty);
                            double tNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                            double tempC = minTemp + tNorm * (maxTemp - minTemp);
                            pxColor = getTemperatureColor(tempC, minTemp, maxTemp);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(10, 25, 60), 0.30);
                        } else {
                            double tempC = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).temperature();
                            pxColor = getTemperatureColor(tempC, minTemp, maxTemp);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(10, 25, 60), 0.30);
                        }
                    } else if (mapMode == 2) { // Precipitation
                        if (context.isPrecipImportActive() && customRainfallImage != null && customRainfallImage.getPixelReader() != null && customRainfallImage.getWidth() > 0) {
                            PixelReader pr = customRainfallImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int rx = (int) Math.min(u * customRainfallImage.getWidth(), customRainfallImage.getWidth() - 1);
                            int ry = (int) Math.min(v * customRainfallImage.getHeight(), customRainfallImage.getHeight() - 1);
                            int argb = pr.getArgb(rx, ry);
                            double precipNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                            pxColor = getPrecipitationColor(precipNorm);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(15, 23, 42), 0.35);
                        } else {
                            double precipNorm = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).rainfall();
                            pxColor = getPrecipitationColor(precipNorm);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(15, 23, 42), 0.35);
                        }
                    } else { // Seasonality
                        if (context.isSeasonImportActive() && customSeasonalityImage != null && customSeasonalityImage.getPixelReader() != null && customSeasonalityImage.getWidth() > 0) {
                            PixelReader pr = customSeasonalityImage.getPixelReader();
                            double u = (lng + 180.0) / 360.0;
                            double v = (90.0 - lat) / 180.0;
                            int sx = (int) Math.min(u * customSeasonalityImage.getWidth(), customSeasonalityImage.getWidth() - 1);
                            int sy = (int) Math.min(v * customSeasonalityImage.getHeight(), customSeasonalityImage.getHeight() - 1);
                            int argb = pr.getArgb(sx, sy);
                            double seasonNorm = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / (3.0 * 255.0);
                            pxColor = getSeasonalityColor(seasonNorm);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(10, 20, 50), 0.30);
                        } else {
                            double seasonNorm = generator.getPlanetPoint(lat, lng, preset, tSeed, pSeed, sSeed).seasonality();
                            pxColor = getSeasonalityColor(seasonNorm);
                            if (isOcean) pxColor = blendColors(pxColor, Color.rgb(10, 20, 50), 0.30);
                        }
                    }

                    pw.setColor(px, py, pxColor);
                }
            }

            gc.drawImage(buffer, 0, 0, canvasW, canvasH);

            double totalAreaWeight = 0.0;
            double oceanAreaWeight = 0.0;
            int sampleH = 160;
            int sampleW = 320;

            for (int gy = 0; gy < sampleH; gy++) {
                double gLat = 90.0 - (gy + 0.5) * (180.0 / sampleH);
                double weight = Math.cos(Math.toRadians(gLat));
                for (int gx = 0; gx < sampleW; gx++) {
                    double gLng = -180.0 + (gx + 0.5) * (360.0 / sampleW);
                    boolean isGlobOcean = false;
                    if (preset.waterLevel() > -0.4) {
                        if (isImportMode && customElevImage != null && elevReader != null && wElev > 0 && hElev > 0) {
                            double u = (gLng + 180.0) / 360.0;
                            double v = (90.0 - gLat) / 180.0;
                            int ex = (int) Math.min(u * wElev, wElev - 1);
                            int ey = (int) Math.min(v * hElev, hElev - 1);
                            Color c = elevReader.getColor(ex, ey);
                            double eNorm = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            isGlobOcean = (eNorm < preset.waterLevel());
                        } else {
                            var pt = generator.getPlanetPoint(gLat, gLng, preset);
                            isGlobOcean = (pt.elevation() < preset.waterLevel());
                        }
                    }
                    totalAreaWeight += weight;
                    if (isGlobOcean) {
                        oceanAreaWeight += weight;
                    }
                }
            }

            int oceanPct = (int) Math.round((oceanAreaWeight * 100.0) / Math.max(0.0001, totalAreaWeight));
            statsLabel.setText(I18n.get("planet.stats.ocean_land", oceanPct, 100 - oceanPct));
            updateAstroLabelText(preset);
        } finally {
            context.setBusyCursor(false);
        }
    }

    public void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        PlanetPreset preset = context.buildPresetFromUI();
        int selectedIdx = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;

        if (selectedIdx == 0) { // Heightmap
            double minAlt = preset.minAltitudeMeters();
            double maxAlt = preset.maxAltitudeMeters();
            Label minLabel = new Label(String.format("Min: %,.0f m", minAlt));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");
            legendBar.getChildren().add(minLabel);

            if (preset.waterLevel() > -0.4) {
                addLegendItem("OCEAN", Color.rgb(15, 23, 42), I18n.getOrDefault("planet.legend.depressions", "Abysses / Océans"));
            } else {
                addLegendItem("DEPRESSIONS", getHypsometricColor(0.0), I18n.getOrDefault("planet.legend.depressions", "Dépressions / Abysses"));
            }
            addLegendItem("LOWLANDS", getHypsometricColor(0.10), I18n.getOrDefault("planet.legend.lowlands", "Basses Terres"));
            addLegendItem("PLAINS", getHypsometricColor(0.35), I18n.getOrDefault("planet.legend.plains", "Plaines & Bassins"));
            addLegendItem("PLATEAUS", getHypsometricColor(0.55), I18n.getOrDefault("planet.legend.highlands", "Plateaux & Collines"));
            addLegendItem("HIGHLANDS", getHypsometricColor(0.75), I18n.getOrDefault("planet.legend.mountains", "Hauts Reliefs"));
            addLegendItem("PEAKS", getHypsometricColor(1.00), I18n.getOrDefault("planet.legend.peaks", "Sommets & Neiges"));

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

            addLegendItem("POLAR", Color.rgb(40, 0, 120), I18n.getOrDefault("planet.legend.polar", "Polar / Gel"));
            addLegendItem("MILD", Color.rgb(0, 200, 180), I18n.getOrDefault("planet.legend.mild", "Mild"));
            addLegendItem("WARM", Color.rgb(240, 200, 0), I18n.getOrDefault("planet.legend.warm", "Chaud"));
            addLegendItem("HOT", Color.rgb(220, 0, 40), I18n.getOrDefault("planet.legend.hot", "Canicule"));

            Label maxLabel = new Label(String.format("Max: %.1f °C", maxT));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444;");
            legendBar.getChildren().add(maxLabel);
        } else if (selectedIdx == 2) { // Precipitation
            Label minLabel = new Label(I18n.getOrDefault("planet.legend.min_arid", "Min: 0 mm/an (Aride)"));
            minLabel.getStyleClass().add("control-label");
            minLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #eab308;");
            legendBar.getChildren().add(minLabel);

            addLegendItem("ARID", Color.rgb(210, 170, 110), I18n.getOrDefault("planet.legend.arid", "Aride"));
            addLegendItem("MODERATE", Color.rgb(60, 180, 80), I18n.getOrDefault("planet.legend.moderate", "Moderate"));
            addLegendItem("HUMID", Color.rgb(20, 160, 180), I18n.getOrDefault("planet.legend.humid", "Humide"));
            addLegendItem("HEAVY", Color.rgb(10, 60, 200), I18n.getOrDefault("planet.legend.deluge", "Deluge"));

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

            addLegendItem("STABLE", Color.rgb(30, 80, 140), I18n.getOrDefault("planet.legend.low_variance", "Faible Variance"));
            addLegendItem("SEASONAL", Color.rgb(80, 180, 100), I18n.getOrDefault("planet.legend.seasonal", "Seasonal"));
            addLegendItem("HIGH", Color.rgb(240, 140, 20), I18n.getOrDefault("planet.legend.high_variance", "High"));
            addLegendItem("EXTREME", Color.rgb(220, 30, 80), I18n.getOrDefault("planet.legend.extreme_amp", "Extreme Amplitude"));

            Label maxLabel = new Label(String.format("%s: %.1f °C (%s)", I18n.getOrDefault("planet.short.max", "Max"), maxAmp, I18n.getOrDefault("planet.legend.amplitude", "Amplitude")));
            maxLabel.getStyleClass().add("control-label");
            maxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a855f7;");
            legendBar.getChildren().add(maxLabel);
        }
    }

    private void addLegendItem(String id, Color col, String text) {
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

    public void updateAstroLabelText(PlanetPreset preset) {
        if (astroLabel == null || preset == null) return;
        astroLabel.setText(String.format("%s: %.0f km | %s: %.1fh | %s: %.1f° | %s: %.0f d | %s: %.2f AU | %s: %.1f°C",
                preset.isSatellite() ? I18n.getOrDefault("planet.short.moon_radius", "🌙 Lune Rayon") : I18n.getOrDefault("planet.short.radius", "📏 Rayon"),
                preset.radiusKm(),
                I18n.getOrDefault("planet.short.day", "Jour"),
                preset.dayLengthHours(),
                I18n.getOrDefault("planet.short.tilt", "Tilt"),
                preset.axialTiltDegrees(),
                I18n.getOrDefault("planet.short.year", "Year"),
                preset.yearLengthDays(),
                I18n.getOrDefault("planet.short.dist", "Dist"),
                preset.distanceToSunAU(),
                I18n.getOrDefault("planet.short.temp", "Temp"),
                preset.averageTempC()));
    }

    public void updateTexts() {
        if (previewTitle != null) previewTitle.setText(I18n.get("planet.preview.title"));
        if (viewModeCombo != null) {
            int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
            viewModeCombo.getItems().setAll(
                    I18n.getOrDefault("planet.view.heightmap", "MAP 1. Heightmap & Relief Map"),
                    I18n.getOrDefault("planet.view.temperature", "MAP 2. Temperature Map (°C)"),
                    I18n.getOrDefault("planet.view.precipitation", "MAP 3. Precipitation Map (mm/yr)"),
                    I18n.getOrDefault("planet.view.seasonality", "MAP 4. Seasonality / Thermal Amplitude Map (°C)")
            );
            if (selected >= 0 && selected < viewModeCombo.getItems().size()) {
                viewModeCombo.getSelectionModel().select(selected);
            }
        }
        updateLegend();
    }

    public static Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.max(0.0, Math.min(1.0, r)), Math.max(0.0, Math.min(1.0, g)), Math.max(0.0, Math.min(1.0, b)));
    }

    public static Color getTemperatureColor(double tempC, double minT, double maxT) {
        double norm = Math.max(0.0, Math.min(1.0, (tempC - minT) / (maxT - minT + 0.001)));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(40 * (1 - t)), (int)(100 * t), (int)(120 + 100 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(100 * t), (int)(100 + 100 * t), (int)(220 * (1 - t)));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(100 + 140 * t), (int)(200 - 40 * t), 0);
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(240 - 20 * t), (int)(160 * (1 - t)), (int)(40 * t));
        }
    }

    public static Color getPrecipitationColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.25) {
            double t = norm / 0.25;
            return Color.rgb((int)(210 - 10 * t), (int)(170 + 40 * t), (int)(110 + 10 * t));
        } else if (norm < 0.5) {
            double t = (norm - 0.25) / 0.25;
            return Color.rgb((int)(200 - 140 * t), (int)(210 - 30 * t), (int)(120 - 40 * t));
        } else if (norm < 0.75) {
            double t = (norm - 0.5) / 0.25;
            return Color.rgb((int)(60 - 40 * t), (int)(180 - 20 * t), (int)(80 + 100 * t));
        } else {
            double t = (norm - 0.75) / 0.25;
            return Color.rgb((int)(20 - 10 * t), (int)(160 - 100 * t), (int)(180 + 20 * t));
        }
    }

    public static Color getSeasonalityColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.33) {
            double t = norm / 0.33;
            return Color.rgb((int)(30 + 50 * t), (int)(80 + 100 * t), (int)(140 - 40 * t));
        } else if (norm < 0.66) {
            double t = (norm - 0.33) / 0.33;
            return Color.rgb((int)(80 + 160 * t), (int)(180 - 40 * t), (int)(100 - 80 * t));
        } else {
            double t = (norm - 0.66) / 0.34;
            return Color.rgb((int)(240 - 20 * t), (int)(140 - 110 * t), (int)(20 + 60 * t));
        }
    }

    public static Color getHypsometricColor(double norm) {
        norm = Math.max(0.0, Math.min(1.0, norm));
        if (norm < 0.20) {
            double t = norm / 0.20;
            return Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
        } else if (norm < 0.40) {
            double t = (norm - 0.20) / 0.20;
            return Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
        } else if (norm < 0.65) {
            double t = (norm - 0.40) / 0.25;
            return Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
        } else if (norm < 0.85) {
            double t = (norm - 0.65) / 0.20;
            return Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
        } else {
            double t = (norm - 0.85) / 0.15;
            return Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
        }
    }

    public void selectViewMode(int modeIndex) {
        if (viewModeCombo != null && viewModeCombo.getSelectionModel().getSelectedIndex() != modeIndex) {
            if (modeIndex >= 0 && modeIndex < viewModeCombo.getItems().size()) {
                viewModeCombo.getSelectionModel().select(modeIndex);
            }
        }
    }

    public int getViewModeIndex() {
        return viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
    }

    public ComboBox<String> getViewModeCombo() { return viewModeCombo; }
    public Canvas getPreviewCanvas() { return previewCanvas; }
}
