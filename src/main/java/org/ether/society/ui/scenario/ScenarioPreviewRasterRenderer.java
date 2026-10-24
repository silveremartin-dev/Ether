/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;

import java.util.Map;

/**
 * Procedural raster rasterization and hillshade elevation blending pipeline for scenario preview.
 *
 * <p>Renders instant 2D density maps, custom tensor channels, hypsometric relief layers,
 * and Lambertian hillshade lighting.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioPreviewRasterRenderer {

    private ScenarioPreviewRasterRenderer() {
        // Utility class
    }

    /**
     * Alpha-blends two JavaFX Colors.
     */
    public static Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        if (overlay == null) return base;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    /**
     * Samples color from an Image at given latitude and longitude coordinates.
     */
    public static Color sampleImageColorAtLatLon(Image img, double lat, double lon) {
        if (img == null || img.getWidth() <= 1 || img.getHeight() <= 1) return null;
        PixelReader reader = img.getPixelReader();
        if (reader == null) return null;

        int px = (int) Math.clamp(((lon + 180.0) / 360.0) * img.getWidth(), 0, img.getWidth() - 1);
        int py = (int) Math.clamp(((90.0 - lat) / 180.0) * img.getHeight(), 0, img.getHeight() - 1);
        return reader.getColor(px, py);
    }

    /**
     * Renders instant 2D density or tensor raster preview with hypsometric relief and hillshade.
     */
    public static void drawInstant2DDensityPreview(
            GraphicsContext gc, double w, double h, double scale, double offX, double offY,
            ScenarioPreviewPaneBuilder.HostContext context,
            int idx, String mode, boolean isReliefOverlay,
            Map<String, WritableImage> proceduralImageCache) {

        int pwWidth = 320;
        int pwHeight = 160;

        PlanetPreset planet = context.getActivePlanetPreset() != null ? context.getActivePlanetPreset() : PlanetPreset.EARTH_LIKE;
        Image bgImage = null;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            bgImage = ImageMapLoader.base64PngToImage(planet.customElevBase64());
        } else if (planet.elevationUseImport()) {
            bgImage = ImageMapLoader.loadMapImage(planet.elevationMapSource(), planet.getAssociatedEpochYear(), "elevation.png");
        }
        if (bgImage == null && "earth".equalsIgnoreCase(planet.elevationMapSource())) {
            bgImage = context.getElevationImageForPreset(planet);
        }
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        int dims = context.getCultureVectorDimensions();
        Image activeCustomImage = null;
        boolean isImportActive = false;
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            isImportActive = context.isTensorImportActive(tIndex);
            activeCustomImage = context.getCustomTensorImage(tIndex);
        } else if (idx == 0) {
            isImportActive = context.isDensityImportActive();
            activeCustomImage = context.getCustomDensityImage();
        }

        PixelReader customReader = activeCustomImage != null ? activeCustomImage.getPixelReader() : null;
        boolean isFootprintMode = idx == 6 || mode.contains("empreinte");

        String cacheKey = "sc_mode=" + idx + "_w=" + pwWidth + "_h=" + pwHeight + "_relief=" + isReliefOverlay
                + "_import=" + isImportActive
                + "_pName=" + (planet != null ? planet.name() : "")
                + "_startYear=" + context.getStartYear()
                + "_pop=" + context.getInitialHumanCount()
                + "_pattern=" + context.getDensityPattern()
                + "_demoSeed=" + context.getDemoSeed()
                + "_cultSeed=" + context.getCultSeed()
                + "_hasCustomDensity=" + (context.getCustomDensityImage() != null);

        WritableImage img = proceduralImageCache.get(cacheKey);
        if (img == null) {
            img = new WritableImage(pwWidth, pwHeight);
            PixelWriter writer = img.getPixelWriter();
            ProceduralGenerator generator = new ProceduralGenerator();

            double minAlt = planet != null ? planet.minAltitudeMeters() : -11000.0;
            double maxAlt = planet != null ? planet.maxAltitudeMeters() : 8848.0;
            double wLevel = planet != null ? planet.waterLevel() : 0.478;
            boolean hasOcean = wLevel > -0.4;

            double cutThreshold;
            if (hasOcean) {
                cutThreshold = Math.clamp(wLevel, 0.01, 0.99);
            } else {
                double altRange = Math.max(100.0, maxAlt - minAlt);
                cutThreshold = Math.clamp((-minAlt) / altRange, 0.05, 0.95);
            }

            for (int py = 0; py < pwHeight; py++) {
                for (int px = 0; px < pwWidth; px++) {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;

                    double elevVal;
                    boolean isLand;
                    boolean isCoast;
                    double hillshade;

                    if (bgReader != null && bgImage != null) {
                        double bgW = bgImage.getWidth();
                        double bgH = bgImage.getHeight();
                        int bx = (int) Math.clamp(((px / (double) pwWidth) * bgW), 0, bgW - 1);
                        int by = (int) Math.clamp(((py / (double) pwHeight) * bgH), 0, bgH - 1);
                        elevVal = bgReader.getColor(bx, by).getRed();
                        boolean isAboveCut = elevVal >= cutThreshold;
                        isLand = hasOcean ? isAboveCut : true;

                        int bxE = (bx + 1) % (int) bgW;
                        int bxW = (bx - 1 + (int) bgW) % (int) bgW;
                        int byN = Math.max(0, by - 1);
                        int byS = Math.min((int) bgH - 1, by + 1);

                        double eE = bgReader.getColor(bxE, by).getRed();
                        double eW = bgReader.getColor(bxW, by).getRed();
                        double eN = bgReader.getColor(bx, byN).getRed();
                        double eS = bgReader.getColor(bx, byS).getRed();

                        isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                                || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));

                        double dLng = (eE - eW) * 16.0;
                        double dLat = (eN - eS) * 16.0;
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    } else {
                        var pt = generator.getPlanetPoint(lat, lon, planet);
                        elevVal = pt.elevation();

                        double dDeg = 0.5;
                        var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                        var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                        var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                        var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                        if (hasOcean) {
                            isLand = elevVal >= wLevel;
                            isCoast = (isLand != (ptE.elevation() >= wLevel)) || (isLand != (ptW.elevation() >= wLevel))
                                    || (isLand != (ptN.elevation() >= wLevel)) || (isLand != (ptS.elevation() >= wLevel));
                        } else {
                            isLand = true;
                            isCoast = Math.abs(elevVal) < 350.0;
                        }

                        double dLng = (ptE.elevation() - ptW.elevation()) * 4.0;
                        double dLat = (ptN.elevation() - ptS.elevation()) * 4.0;
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    }

                    Color baseReliefColor;
                    if (!isLand) {
                        double depth = Math.clamp((cutThreshold - elevVal) / Math.max(0.01, cutThreshold), 0.0, 1.0);
                        baseReliefColor = blendColors(Color.rgb(15, 23, 42), Color.rgb(30, 58, 138), 1.0 - depth);
                    } else {
                        double landNorm = Math.clamp((elevVal - cutThreshold) / Math.max(0.01, 1.0 - cutThreshold), 0.0, 1.0);
                        if (landNorm < 0.20) {
                            double t = landNorm / 0.20;
                            baseReliefColor = Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
                        } else if (landNorm < 0.40) {
                            double t = (landNorm - 0.20) / 0.20;
                            baseReliefColor = Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
                        } else if (landNorm < 0.65) {
                            double t = (landNorm - 0.40) / 0.25;
                            baseReliefColor = Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
                        } else if (landNorm < 0.85) {
                            double t = (landNorm - 0.65) / 0.20;
                            baseReliefColor = Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
                        } else {
                            double t = (landNorm - 0.85) / 0.15;
                            baseReliefColor = Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
                        }
                    }

                    Color pxColor;
                    if (idx >= 1 && idx <= dims) {
                        int tIndex = idx - 1;
                        if (isImportActive) {
                            if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                                int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                                int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                                pxColor = customReader.getColor(imgX, imgY);
                            } else {
                                pxColor = Color.BLACK;
                            }
                        } else {
                            double p1 = context.getTensorParam1(tIndex);
                            double p2 = context.getTensorParam2(tIndex);
                            double p3 = context.getTensorParam3(tIndex);
                            long seed = context.getTensorSeed(tIndex);
                            pxColor = context.evaluateProceduralTensorColor(tIndex, lat, lon, elevVal, p1, p2, p3, seed);
                        }
                    } else if (isImportActive) {
                        if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                            int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                            int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                            pxColor = customReader.getColor(imgX, imgY);
                            if (!isLand) {
                                pxColor = baseReliefColor;
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLand) {
                        pxColor = baseReliefColor;
                    } else {
                        long startYear = context.getStartYear();
                        boolean isAmericas = lon < -25.0;
                        boolean isSahul = (lat < 10.0 && lon > 95.0) || (lat < -10.0 && lon > 110.0);

                        boolean isHumanSettled = true;
                        if (startYear <= -50000 && (isAmericas || isSahul)) {
                            isHumanSettled = false;
                        } else if (startYear <= -25000 && isAmericas) {
                            isHumanSettled = false;
                        }

                        if (!isHumanSettled) {
                            pxColor = blendColors(baseReliefColor, Color.rgb(30, 95, 165), 0.35);
                        } else {
                            boolean isEastAfrica = (lat >= -15 && lat <= 15) && (lon >= 25 && lon <= 45);
                            boolean isFertileCrescent = (lat >= 20 && lat <= 38) && (lon >= 25 && lon <= 90);
                            boolean isChinaIndus = (lat >= 10 && lat <= 42) && (lon >= 65 && lon <= 125);

                            if (isEastAfrica || isFertileCrescent || isChinaIndus) {
                                Color heatCol = isFootprintMode ? Color.rgb(239, 68, 68) : Color.rgb(249, 115, 22);
                                pxColor = blendColors(baseReliefColor, heatCol, 0.70);
                            } else {
                                pxColor = blendColors(baseReliefColor, Color.rgb(16, 185, 129), 0.50);
                            }
                        }
                    }

                    if (isReliefOverlay && pxColor != Color.BLACK) {
                        if (isCoast) {
                            pxColor = hasOcean ? Color.rgb(240, 249, 255) : Color.rgb(251, 191, 36);
                        } else {
                            double mult = 0.68 + 0.45 * hillshade;
                            pxColor = Color.color(
                                Math.clamp(pxColor.getRed() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getGreen() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getBlue() * mult, 0.0, 1.0)
                            );
                        }
                    }

                    writer.setColor(px, py, pxColor != null ? pxColor : Color.BLACK);
                }
            }
            proceduralImageCache.put(cacheKey, img);
        }

        double drawW = 360.0 * scale;
        double drawH = 180.0 * scale;
        gc.drawImage(img, offX, offY, drawW, drawH);

        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.fillText("⚡ Aperçu 2D dynamique instantané (" + (mode.isEmpty() ? "Relief/Densité" : mode.toUpperCase()) + ")...", 20, h - 15);
    }
}
