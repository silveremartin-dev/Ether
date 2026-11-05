/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.ui.resource.ResourcePreviewCanvasBuilder.HostContext;

/**
 * Stateless equirectangular 2D cartographic pixel renderer and relief hillshader
 * extracted from {@link ResourcePreviewCanvasBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ResourcePreviewCanvasBuilder
 */
public final class ResourcePreviewPixelRenderer {

    private ResourcePreviewPixelRenderer() {
        // Stateless cartographic pixel renderer
    }

    /**
     * Renders the thematic resource preview buffer across 15 geological, climatic, and ecological modes.
     *
     * @param host       host resource panel context
     * @param mode       thematic layer mode index (0..14)
     * @param isRelief   whether topographic hillshading and coastline delineation are enabled
     * @param zoomFactor interactive zoom scale
     * @param panX       horizontal pan offset in buffer pixels
     * @param panY       vertical pan offset in buffer pixels
     * @return rendered {@code 320 x 160} {@link WritableImage}
     */
    public static WritableImage renderPreviewBuffer(HostContext host,
                                                    int mode,
                                                    boolean isRelief,
                                                    double zoomFactor,
                                                    double panX,
                                                    double panY) {
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        planet = planet.withSeismicAndVolcanic(host.getSeismicActivityValue(), host.getVolcanicActivityValue());

        int w = 320;
        int h = 160;
        WritableImage buffer = new WritableImage(w, h);
        PixelWriter pw = buffer.getPixelWriter();

        Image customBiomeImage = host.getCustomBiomeImage();
        Image customHydroImage = host.getCustomHydroImage();
        PixelReader customBiomeReader = customBiomeImage != null ? customBiomeImage.getPixelReader() : null;
        PixelReader customHydroReader = customHydroImage != null ? customHydroImage.getPixelReader() : null;
        double wBio = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBio = customBiomeImage != null ? customBiomeImage.getHeight() : 0;
        double wHydro = customHydroImage != null ? customHydroImage.getWidth() : 0;
        double hHydro = customHydroImage != null ? customHydroImage.getHeight() : 0;

        Image elevImg = resolveElevationImage(planet);
        PixelReader elevReader = (elevImg != null && elevImg.getWidth() > 0) ? elevImg.getPixelReader() : null;
        double elevW = elevImg != null ? elevImg.getWidth() : 0;
        double elevH = elevImg != null ? elevImg.getHeight() : 0;

        ProceduralGenerator generator = host.getProceduralGenerator();
        ImageMapLoader mapLoader = host.getMapLoader();
        ResourceGeologySectionBuilder geologyBuilder = host.getGeologySectionBuilder();

        double minAlt = planet.minAltitudeMeters();
        double maxAlt = planet.maxAltitudeMeters();
        double wLvl = planet.waterLevel();
        boolean hasOcean = wLvl > -0.4;
        double cutThreshold = hasOcean
                ? Math.clamp(wLvl, 0.01, 0.99)
                : Math.clamp((-minAlt) / Math.max(100.0, maxAlt - minAlt), 0.05, 0.95);

        for (int py = 0; py < h; py++) {
            double yBase = (py - h / 2.0 - panY) / zoomFactor + h / 2.0;
            if (yBase < 0 || yBase >= h) {
                for (int px = 0; px < w; px++) pw.setColor(px, py, Color.rgb(15, 23, 42));
                continue;
            }
            double lat = 90.0 - (yBase / (double) h) * 180.0;

            for (int px = 0; px < w; px++) {
                double xBase = (px - w / 2.0 - panX) / zoomFactor + w / 2.0;
                if (xBase < 0 || xBase >= w) {
                    pw.setColor(px, py, Color.rgb(15, 23, 42));
                    continue;
                }
                double lon = -180.0 + (xBase / (double) w) * 360.0;

                boolean isLandHere;
                boolean isCoast;
                double hillshade;
                double declivityVal;

                if (elevReader != null && elevW > 0 && elevH > 0) {
                    int ex = (int) Math.clamp((xBase / (double) w) * elevW, 0, elevW - 1);
                    int ey = (int) Math.clamp((yBase / (double) h) * elevH, 0, elevH - 1);
                    double eVal = elevReader.getColor(ex, ey).getRed();
                    boolean isAboveCut = eVal >= cutThreshold;
                    isLandHere = !hasOcean || isAboveCut;

                    int exE = (ex + 1) % (int) elevW;
                    int exW = (ex - 1 + (int) elevW) % (int) elevW;
                    int eyN = Math.max(0, ey - 1);
                    int eyS = Math.min((int) elevH - 1, ey + 1);

                    double eE = elevReader.getColor(exE, ey).getRed();
                    double eW = elevReader.getColor(exW, ey).getRed();
                    double eN = elevReader.getColor(ex, eyN).getRed();
                    double eS = elevReader.getColor(ex, eyS).getRed();

                    isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                            || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));
                    double dLng = (eE - eW) * 16.0;
                    double dLat = (eN - eS) * 16.0;
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    declivityVal = Math.sqrt(dLng * dLng + dLat * dLat);
                } else {
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    double elev = pt.elevation();
                    declivityVal = pt.declivity();
                    double dDeg = 0.5;
                    var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                    var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                    var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                    var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                    if (hasOcean) {
                        isLandHere = elev >= wLvl;
                        isCoast = (isLandHere != (ptE.elevation() >= wLvl))
                                || (isLandHere != (ptW.elevation() >= wLvl))
                                || (isLandHere != (ptN.elevation() >= wLvl))
                                || (isLandHere != (ptS.elevation() >= wLvl));
                    } else {
                        isLandHere = true;
                        boolean isAboveDatum = elev >= 0.0;
                        isCoast = (isAboveDatum != (ptE.elevation() >= 0.0))
                                || (isAboveDatum != (ptW.elevation() >= 0.0))
                                || (isAboveDatum != (ptN.elevation() >= 0.0))
                                || (isAboveDatum != (ptS.elevation() >= 0.0));
                    }
                    double dLng = (ptE.elevation() - ptW.elevation()) / 1200.0;
                    double dLat = (ptN.elevation() - ptS.elevation()) / 1200.0;
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                }

                Color pxColor = evaluateModeColor(
                        mode, host, planet, generator, mapLoader, geologyBuilder,
                        customBiomeReader, wBio, hBio, customHydroReader, wHydro, hHydro,
                        xBase, yBase, w, h, lat, lon, isLandHere, declivityVal);

                if (isRelief) {
                    if (isCoast) {
                        pxColor = hasOcean ? Color.rgb(240, 249, 255) : Color.rgb(251, 191, 36);
                    } else {
                        double mult = 0.68 + 0.45 * hillshade;
                        pxColor = Color.color(
                                Math.clamp(pxColor.getRed() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getGreen() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getBlue() * mult, 0.0, 1.0));
                    }
                }
                pw.setColor(px, py, pxColor);
            }
        }
        return buffer;
    }

    private static Image resolveElevationImage(PlanetPreset planet) {
        if (planet == null) return null;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            return ImageMapLoader.base64PngToImage(planet.customElevBase64());
        }
        String pName = planet.elevationMapSource() != null && !planet.elevationMapSource().equalsIgnoreCase("none")
                ? planet.elevationMapSource().toLowerCase()
                : (planet.name() != null ? planet.name().toLowerCase() : "earth");
        if (pName.contains("earth") || pName.contains("terre")) return ImageMapLoader.loadMapImage("earth_elevation.png");
        if (pName.contains("mars")) return ImageMapLoader.loadMapImage("mars_elevation.png");
        if (pName.contains("moon") || pName.contains("lune")) return ImageMapLoader.loadMapImage("moon_elevation.png");
        if (pName.contains("venus")) return ImageMapLoader.loadMapImage("venus_elevation.png");
        if (pName.contains("mercury") || pName.contains("mercure")) return ImageMapLoader.loadMapImage("mercury_elevation.png");
        return null;
    }

    private static Color evaluateModeColor(int mode, HostContext host, PlanetPreset planet,
                                           ProceduralGenerator generator, ImageMapLoader mapLoader,
                                           ResourceGeologySectionBuilder geologyBuilder,
                                           PixelReader customBiomeReader, double wBio, double hBio,
                                           PixelReader customHydroReader, double wHydro, double hHydro,
                                           double xBase, double yBase, int w, int h,
                                           double lat, double lon, boolean isLandHere, double declivityVal) {
        if (mode == 0) {
            if (host.isRadioImportBiomeSelected()) {
                if (customBiomeReader != null && wBio > 0 && hBio > 0) {
                    int bx = (int) Math.min((xBase / (double) w) * wBio, wBio - 1);
                    int by = (int) Math.min((yBase / (double) h) * hBio, hBio - 1);
                    return mapLoader.getBiomeTargetColor(mapLoader.matchBiomeColor(customBiomeReader.getColor(bx, by)));
                }
                return Color.BLACK;
            }
            if (!isLandHere) return Color.rgb(0, 50, 200);
            return mapLoader.getBiomeTargetColor(generator.getPlanetPoint(lat, lon, planet).biome());
        }
        if (mode == 1) {
            if (host.isRadioImportHydroSelected()) {
                if (customHydroReader != null && wHydro > 0 && hHydro > 0) {
                    int hx = (int) Math.min((xBase / (double) w) * wHydro, wHydro - 1);
                    int hy = (int) Math.min((yBase / (double) h) * hHydro, hHydro - 1);
                    Color c = customHydroReader.getColor(hx, hy);
                    double waterIntensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                    if (waterIntensity > 0.6) return Color.rgb(2, 132, 199);
                    if (waterIntensity > 0.3) return Color.rgb(56, 189, 248);
                    if (!isLandHere) return Color.rgb(15, 23, 42);
                    return terrainSlopeColor(declivityVal);
                }
                return Color.BLACK;
            }
            if (!isLandHere) return Color.rgb(15, 23, 42);
            double riverFlow = generator.getPlanetPoint(lat, lon, planet).riverFlow();
            if (riverFlow > 0.42) return Color.rgb(2, 132, 199);
            if (riverFlow > 0.28) return Color.rgb(56, 189, 248);
            if (riverFlow > 0.16) return Color.rgb(20, 184, 166);
            return terrainSlopeColor(declivityVal);
        }
        if (mode >= 2 && mode <= 11) {
            int layerIdx = mode - 2;
            boolean useImport = geologyBuilder != null && geologyBuilder.isGeologyImportActive(layerIdx);
            Image gImg = geologyBuilder != null ? geologyBuilder.getGeologyLayerImage(layerIdx) : null;
            Color baseBackground = isLandHere ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);
            if (useImport) {
                if (gImg != null && gImg.getPixelReader() != null && gImg.getWidth() > 0 && gImg.getHeight() > 0) {
                    int gx = (int) Math.clamp((xBase / (double) w) * gImg.getWidth(), 0, gImg.getWidth() - 1);
                    int gy = (int) Math.clamp((yBase / (double) h) * gImg.getHeight(), 0, gImg.getHeight() - 1);
                    Color c = gImg.getPixelReader().getColor(gx, gy);
                    double alpha = c.getOpacity();
                    double brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                    if (alpha > 0.05 && brightness > 0.02) {
                        return blendColors(baseBackground, Color.color(c.getRed(), c.getGreen(), c.getBlue()), alpha);
                    }
                    return baseBackground;
                }
                return Color.BLACK;
            }
            double val = geologyBuilder != null
                    ? Math.clamp(geologyBuilder.sampleProceduralGeologyTensor(layerIdx, lon, lat, planet), 0.0, 1.0)
                    : 0.0;
            if (val > 0.02) {
                Color resourceColor = geologyBuilder != null ? geologyBuilder.getGeologyResourceColor(layerIdx, val) : Color.WHITE;
                return blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
            }
            return baseBackground;
        }
        if (mode == 12) {
            double tempC = generator.getPlanetPoint(lat, lon, planet).temperature();
            double tNorm = Math.clamp((tempC + 25.0) / 70.0, 0.0, 1.0);
            int r, g, b;
            if (tNorm < 0.25) {
                double n = tNorm / 0.25;
                r = (int) (15 + n * 40); g = (int) (100 + n * 80); b = (int) (220 + n * 35);
            } else if (tNorm < 0.5) {
                double n = (tNorm - 0.25) / 0.25;
                r = (int) (55 + n * 50); g = (int) (180 + n * 30); b = (int) (255 - n * 180);
            } else if (tNorm < 0.75) {
                double n = (tNorm - 0.5) / 0.25;
                r = (int) (105 + n * 140); g = (int) (210 - n * 60); b = (int) (75 - n * 60);
            } else {
                double n = (tNorm - 0.75) / 0.25;
                r = (int) (245 + n * 10); g = (int) (150 - n * 130); b = (int) (15 + n * 20);
            }
            Color tColor = Color.rgb(r, g, b);
            return !isLandHere ? blendColors(tColor, Color.rgb(15, 23, 42), 0.35) : tColor;
        }
        if (mode == 13) {
            double heatVal = geologyBuilder != null ? geologyBuilder.sampleProceduralGeologyTensor(8, lon, lat, planet) : 0.0;
            double sLevel = planet.seismicActivityLevel() / 10.0;
            double vLevel = planet.volcanicActivityLevel() / 8.0;
            double risk = Math.clamp(heatVal * 0.5 + declivityVal * 2.0 * sLevel + vLevel * 0.3, 0.0, 1.0);
            if (!isLandHere) {
                return Color.rgb((int) (15 + risk * 230), (int) (23 + risk * 40), (int) (42 + (1.0 - risk) * 50));
            }
            return Color.rgb((int) (40 + risk * 215), (int) (45 + risk * 90), (int) (55 + (1.0 - risk) * 20));
        }
        if (!isLandHere) return Color.rgb(15, 23, 42);
        var point = generator.getPlanetPoint(lat, lon, planet);
        double aridityIndex = point.rainfall() / Math.max(1.0, point.temperature() + 10.0);
        double dryness = Math.clamp(1.0 - (aridityIndex / 50.0), 0.0, 1.0);
        return Color.rgb(
                (int) Math.clamp(30 + dryness * 215, 0, 255),
                (int) Math.clamp(145 - dryness * 85, 0, 255),
                (int) Math.clamp(160 - dryness * 145, 0, 255));
    }

    private static Color terrainSlopeColor(double declivityVal) {
        return Color.rgb(
                (int) Math.min(255, 45 + declivityVal * 100),
                (int) Math.min(255, 60 + declivityVal * 80),
                (int) Math.min(255, 55 + declivityVal * 50));
    }

    /**
     * Linearly blends an overlay color onto a base color using {@code opacity}.
     *
     * @param base    base background color
     * @param overlay foreground color
     * @param opacity blend factor in [0.0, 1.0]
     * @return blended color
     */
    public static Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    /**
     * Computes relief shade color from elevation, sea level, and declivity slope.
     *
     * @param elevation  point elevation in meters
     * @param waterLevel planet sea level datum
     * @param declivity  surface slope gradient
     * @return relief shade color
     */
    public static Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        if (elevation < waterLevel) {
            double depthNorm = Math.clamp((waterLevel - elevation) / 2000.0, 0.0, 1.0);
            double val = 0.05 + 0.15 * (1.0 - depthNorm);
            return Color.color(val * 0.5, val * 0.7, val);
        }
        double hNorm = Math.clamp((elevation - waterLevel) / 3000.0, 0.0, 1.0);
        double shade = 0.2 + 0.6 * hNorm + 0.2 * Math.min(1.0, declivity * 2.0);
        return Color.color(shade, shade, shade);
    }
}
