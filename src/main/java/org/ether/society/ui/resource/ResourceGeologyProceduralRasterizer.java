/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.generation.SimplexNoise;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procedural synthesis engine and rasterizer for planetary geological resource tensors.
 * <p>
 * Simulates ore and hydrocarbon distribution using multi-octave 3D Simplex noise coupled with
 * empirical tectonic/cratonic hotspot kernels:
 * \[
 *   \mathcal{G}_i(\lambda, \phi) = \mathcal{K}_i(\lambda, \phi) \cdot \prod_{j=1}^4 f_j(P_{i,j})
 * \]
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ResourceGeologyProceduralRasterizer {

    private static final Map<Long, SimplexNoise> noiseCache = new ConcurrentHashMap<>();
    private static final ProceduralGenerator generator = new ProceduralGenerator();

    private ResourceGeologyProceduralRasterizer() {
        // Utility class
    }

    private static SimplexNoise getOrCreateNoise(long seed) {
        return noiseCache.computeIfAbsent(seed, SimplexNoise::new);
    }

    /**
     * Evaluates procedural sample value for a specific geological tensor layer at (\( \lambda, \phi \)).
     *
     * @param layerIdx tensor layer index (0 to 9)
     * @param lon longitude in degrees [-180, +180]
     * @param lat latitude in degrees [-90, +90]
     * @param planet planetary preset parameters
     * @param abundanceSliders abundance slider map
     * @param thresholdSliders threshold slider map
     * @param param3Sliders parameter 3 slider map
     * @param param4Sliders parameter 4 slider map
     * @param tensorSeeds tensor seed textfield map
     * @return normalized physical intensity [0.0, 1.0]
     */
    public static double sampleProceduralGeologyTensor(
            int layerIdx, double lon, double lat, PlanetPreset planet,
            Map<Integer, Slider> abundanceSliders,
            Map<Integer, Slider> thresholdSliders,
            Map<Integer, Slider> param3Sliders,
            Map<Integer, Slider> param4Sliders,
            Map<Integer, TextField> tensorSeeds) {

        double p1 = abundanceSliders.containsKey(layerIdx) ? abundanceSliders.get(layerIdx).getValue() : ResourceGeologyDepositCatalog.GEOLOGY_SLIDER_SPECS[layerIdx][0].defaultValue;
        double p2 = thresholdSliders.containsKey(layerIdx) ? thresholdSliders.get(layerIdx).getValue() : ResourceGeologyDepositCatalog.GEOLOGY_SLIDER_SPECS[layerIdx][1].defaultValue;
        double p3 = param3Sliders.containsKey(layerIdx) ? param3Sliders.get(layerIdx).getValue() : ResourceGeologyDepositCatalog.GEOLOGY_SLIDER_SPECS[layerIdx][2].defaultValue;
        double p4 = param4Sliders.containsKey(layerIdx) ? param4Sliders.get(layerIdx).getValue() : ResourceGeologyDepositCatalog.GEOLOGY_SLIDER_SPECS[layerIdx][3].defaultValue;

        double[][] baseHotspots = ResourceGeologyDepositCatalog.getHotspotsForLayer(layerIdx);
        double baseVal = ResourceGeologyDepositCatalog.sampleHotspotVal(lon, lat, baseHotspots);

        long seed = 12345L + (layerIdx + 1) * 9876543L;
        if (tensorSeeds.containsKey(layerIdx) && tensorSeeds.get(layerIdx) != null) {
            try {
                seed = Long.parseLong(tensorSeeds.get(layerIdx).getText().trim());
            } catch (NumberFormatException ignored) {}
        }

        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);
        double x = Math.cos(latR) * Math.cos(lonR);
        double y = Math.cos(latR) * Math.sin(lonR);
        double z = Math.sin(latR);

        SimplexNoise noise1 = getOrCreateNoise(seed);
        SimplexNoise noise2 = getOrCreateNoise(seed + 99999L);
        double scale1 = 2.5 + (layerIdx % 5) * 1.2;
        double scale2 = 5.5 + (layerIdx % 4) * 1.8;
        double n1 = (noise1.noise(x * scale1, y * scale1, z * scale1) + 1.0) * 0.5;
        double n2 = (noise2.noise(x * scale2, y * scale2, z * scale2) + 1.0) * 0.5;

        double combined;

        switch (layerIdx) {
            case 0 -> { // COAL
                double biomassMask = (n1 > (1.0 - p2)) ? 1.0 : 0.0;
                double rankMult = 0.5 + p3 * 0.5;
                double depthMult = Math.clamp(p4 / 5.0, 0.2, 2.0);
                combined = (baseVal * 0.5 + n1 * 0.3 + n2 * 0.2) * biomassMask * rankMult * depthMult * p1;
            }
            case 1 -> { // CRUDE_OIL
                double tocMask = (n1 > (1.0 - p2 * 0.8)) ? 1.0 : 0.2;
                double trapBonus = 0.5 + p3 * 0.8 * n2;
                double tempFactor = 1.0 - Math.abs(p4 - 120.0) / 100.0;
                tempFactor = Math.clamp(tempFactor, 0.1, 1.0);
                combined = (baseVal * 0.5 + n1 * 0.5) * tocMask * trapBonus * tempFactor * p1;
            }
            case 2 -> { // NATURAL_GAS
                double shaleBoost = 0.4 + p2 * 0.6;
                double depthPres = Math.clamp(p3 / 4.0, 0.3, 1.8);
                double sealMask = 0.3 + p4 * 0.7;
                combined = (baseVal * 0.4 + n1 * 0.4 + n2 * 0.2) * shaleBoost * depthPres * sealMask * p1;
            }
            case 3 -> { // URANIUM
                double ppmNorm = p1 / 250.0;
                double hydroBoost = 0.5 + p2 * 1.0 * n2;
                double thUFactor = 1.0 + (p3 - 3.5) * 0.1;
                double unconformityMask = 0.6 + p4 * 0.8 * (n1 > 0.6 ? 1.5 : 0.5);
                combined = (baseVal * 0.4 + n1 * 0.6) * ppmNorm * hydroBoost * thUFactor * unconformityMask;
            }
            case 4 -> { // HELIUM_3
                double ppbNorm = p1 / 15.0;
                double ilmeniteBoost = 0.4 + p2 * 0.8;
                double solarIon = Math.clamp(p3, 0.0, 5.0);
                double churnLoss = Math.clamp(1.0 - (p4 - 3.0) * 0.05, 0.5, 1.2);
                double atmShield = planet.atmospherePressureAtm() > 0.1 ? 0.001 : 1.0;
                combined = (baseVal * 0.5 + n1 * 0.5) * ppbNorm * ilmeniteBoost * solarIon * churnLoss * atmShield;
            }
            case 5 -> { // IRON_COPPER
                double bifLayer = (n1 > 0.4) ? p3 * 0.8 : 0.2;
                double magmaticPorphyry = p2 * 1.2 * n2 * (p4 / 0.8);
                combined = (baseVal * 0.4 + bifLayer * 0.3 + magmaticPorphyry * 0.3) * p1;
            }
            case 6 -> { // PRECIOUS_METALS (Au, Ag, Pt)
                double placer = p2 * 1.2 * (n1 > 0.4 ? 1.2 : 0.2);
                double epithermal = p3 * 1.4 * (n2 > 0.5 ? 1.5 : 0.3);
                double porphyryAu = (p4 / 2.0) * 0.6;
                combined = (baseVal * 0.45 + placer * 0.25 + epithermal * 0.2 + porphyryAu * 0.1) * p1;
            }
            case 7 -> { // CRITICAL_REE (Terres Rares, Bastnasite, Li)
                double carbonatite = p2 * 1.5 * (n1 > 0.6 ? 1.6 : 0.2);
                double salarLi = p3 * 1.3 * (n2 > 0.5 ? 1.2 : 0.1);
                double spodumene = (p4 / 1.5) * 0.5;
                combined = (baseVal * 0.4 + carbonatite * 0.25 + salarLi * 0.2 + spodumene * 0.15) * p1;
            }
            case 8 -> { // MANTLE_HEAT
                double heatNorm = p1 / 65.0;
                double plumeAnomalies = 1.0 + p2 * 1.5 * n1;
                double cratonInsulation = Math.clamp(1.0 - (p3 - 150.0) / 300.0, 0.3, 1.5);
                combined = (baseVal * 0.5 + n1 * 0.5) * heatNorm * plumeAnomalies * cratonInsulation;
            }
            default -> { // AQUIFERS
                double volumeNorm = p1;
                double permFactor = 0.5 + p2 * 0.8 * n1;
                double porosityBoost = p3 / 18.0;
                double permafrostShield = Math.clamp(1.0 - p4 / 1000.0, 0.1, 1.0);
                combined = (baseVal * 0.4 + n2 * 0.6) * volumeNorm * permFactor * porosityBoost * permafrostShield;
            }
        }
        return Math.clamp(combined, 0.0, 1.0);
    }

    /**
     * Resolves the high-contrast display color for a specific geological tensor value.
     *
     * @param layerIdx tensor layer index
     * @param val physical value in [0.0, 1.0]
     * @return blended JavaFX Color
     */
    public static Color getGeologyResourceColor(int layerIdx, double val) {
        val = Math.clamp(val, 0.0, 1.0);
        return switch (layerIdx) {
            case 0 -> lerpColorFx(Color.rgb(146, 64, 14), Color.rgb(254, 240, 138), val);
            case 1 -> lerpColorFx(Color.rgb(153, 27, 27), Color.rgb(254, 202, 202), val);
            case 2 -> lerpColorFx(Color.rgb(14, 116, 144), Color.rgb(207, 250, 254), val);
            case 3 -> lerpColorFx(Color.rgb(20, 83, 45), Color.rgb(74, 222, 128), val);
            case 4 -> lerpColorFx(Color.rgb(76, 29, 149), Color.rgb(192, 132, 252), val);
            case 5 -> lerpColorFx(Color.rgb(139, 69, 19), Color.rgb(249, 115, 22), val);
            case 6 -> lerpColorFx(Color.rgb(161, 98, 7), Color.rgb(250, 204, 21), val);
            case 7 -> lerpColorFx(Color.rgb(13, 148, 136), Color.rgb(45, 212, 191), val);
            case 8 -> lerpColorFx(Color.rgb(185, 28, 28), Color.rgb(253, 224, 71), val);
            default -> lerpColorFx(Color.rgb(30, 58, 138), Color.rgb(96, 165, 250), val);
        };
    }

    /**
     * Generates a high-resolution raster image of the specified procedural geological tensor.
     */
    public static Image generateProceduralGeologyRasterImage(
            int layerIdx, int width, int height, PlanetPreset planet,
            Map<Integer, Slider> abundanceSliders,
            Map<Integer, Slider> thresholdSliders,
            Map<Integer, Slider> param3Sliders,
            Map<Integer, Slider> param4Sliders,
            Map<Integer, TextField> tensorSeeds) {

        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                boolean isLand = wLvl <= -0.4 || pt.elevation() >= wLvl;
                Color baseBackground = isLand ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);
                double val = sampleProceduralGeologyTensor(layerIdx, lon, lat, planet, abundanceSliders, thresholdSliders, param3Sliders, param4Sliders, tensorSeeds);
                val = Math.clamp(val, 0.0, 1.0);
                Color pxColor;
                if (val > 0.02) {
                    Color resourceColor = getGeologyResourceColor(layerIdx, val);
                    pxColor = blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
                } else {
                    pxColor = baseBackground;
                }
                pw.setColor(x, y, pxColor);
            }
        }
        return img;
    }

    public static Color lerpColorFx(Color c1, Color c2, double t) {
        double r = Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0.0, 1.0);
        double g = Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0.0, 1.0);
        double b = Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0.0, 1.0);
        return Color.color(r, g, b);
    }

    public static Color blendColors(Color c1, Color c2, double weightC2) {
        double w2 = Math.clamp(weightC2, 0.0, 1.0);
        double w1 = 1.0 - w2;
        return Color.color(
                Math.clamp(c1.getRed() * w1 + c2.getRed() * w2, 0.0, 1.0),
                Math.clamp(c1.getGreen() * w1 + c2.getGreen() * w2, 0.0, 1.0),
                Math.clamp(c1.getBlue() * w1 + c2.getBlue() * w2, 0.0, 1.0)
        );
    }

    public static Image bufferedImageToFXImage(BufferedImage bImg) {
        if (bImg == null) return null;
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        WritableImage fxImg = new WritableImage(w, h);
        PixelWriter pw = fxImg.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, bImg.getRGB(x, y));
            }
        }
        return fxImg;
    }
}
