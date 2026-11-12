/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/**
 * Stateless cartographic raster naming normalizer and linear ARGB cross-epoch image interpolator
 * supporting both headless AWT {@link BufferedImage} and JavaFX {@link Image} textures.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see TemporalTensorRepository
 */
public final class TemporalRasterInterpolator {

    private TemporalRasterInterpolator() {
        // Stateless utility class
    }

    /**
     * Resolves the canonical directory name for a planet (e.g. {@code "terre"} -> {@code "earth"}),
     * stripping path-traversal characters to enforce filesystem containment.
     *
     * @param planet raw planet identifier
     * @return sanitized canonical planet directory name
     */
    public static String normalizePlanet(String planet) {
        if (planet == null || planet.isBlank()) return "earth";
        String p = planet.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (p.isBlank()) return "earth";

        return switch (p) {
            case "terre", "earth", "terra", "gaia" -> "earth";
            case "lune", "moon", "luna", "selene" -> "moon";
            case "mars", "ares" -> "mars";
            case "venus", "hesperos", "aphrodite" -> "venus";
            case "mercure", "mercury", "hermes" -> "mercury";
            default -> p;
        };
    }

    /**
     * Standardizes an arbitrary layer filename or alias into its short canonical tag
     * (e.g. {@code "earth_1000_density.png"} -> {@code "density"}).
     *
     * @param layerName raw layer path, filename, or alias
     * @return canonical layer tag
     */
    public static String canonicalLayerTag(String layerName) {
        if (layerName == null || layerName.isBlank()) return "elevation";
        String base = layerName.trim().toLowerCase(Locale.ROOT);
        if (base.startsWith("/")) base = base.substring(1);
        if (base.contains("/")) base = base.substring(base.lastIndexOf('/') + 1);
        if (base.endsWith(".png")) base = base.substring(0, base.length() - 4);
        for (String prefix : List.of("earth_", "mars_", "moon_", "venus_", "mercury_", "terre_", "lune_", "mercure_")) {
            if (base.startsWith(prefix)) {
                base = base.substring(prefix.length());
                break;
            }
        }
        if (base.matches("^-?\\d+_.+")) {
            base = base.substring(base.indexOf('_') + 1);
        }
        return switch (base) {
            case "geothermal", "mantleheat", "mantle_heat" -> "geothermal";
            case "trade", "tradenetwork", "trade_network" -> "tradenetwork";
            case "ironcopper", "iron_copper" -> "iron_copper";
            case "preciousmetals", "precious_metals", "preciousree", "precious_ree" -> "precious_metals";
            case "rareearths", "rare_earths" -> "rare_earths";
            case "aquifer", "aquifers" -> "aquifers";
            case "he3", "helium3", "helium_3" -> "helium3";
            case "temp", "temperature" -> "temperature";
            case "rain", "rainfall", "precipitation" -> "precipitation";
            case "elev", "elevation", "dem" -> "elevation";
            case "biome", "biomes" -> "biomes";
            case "pop", "density", "population" -> "density";
            case "sovereignty", "borders", "empire", "polity" -> "sovereignty";
            case "language", "isogloss", "dialects" -> "isogloss";
            case "kinship", "clans", "family" -> "kinship";
            case "religion", "rituals", "sacred" -> "rituals";
            case "tech", "technology" -> "technology";
            case "inst", "institutional", "complexity" -> "institutional";
            case "eco", "ecological", "footprint" -> "ecological";
            case "pathogen", "disease", "immunity" -> "pathogen";
            default -> base;
        };
    }

    /**
     * Performs linear cross-fade blending between two headless {@link BufferedImage} scalar raster maps
     * using scanline-buffered 32-bit ARGB integer arithmetic.
     *
     * @param img0 earlier baseline raster
     * @param img1 later target raster
     * @param t    barycentric interpolation factor in {@code [0.0, 1.0]}
     * @return interpolated {@link BufferedImage}
     */
    public static BufferedImage interpolateBufferedImages(BufferedImage img0, BufferedImage img1, double t) {
        t = Math.clamp(t, 0.0, 1.0);
        int w = Math.min(img0.getWidth(), img1.getWidth());
        int h = Math.min(img0.getHeight(), img1.getHeight());

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] line0 = new int[w];
        int[] line1 = new int[w];
        int[] lineOut = new int[w];
        double invT = 1.0 - t;

        for (int y = 0; y < h; y++) {
            img0.getRGB(0, y, w, 1, line0, 0, w);
            img1.getRGB(0, y, w, 1, line1, 0, w);
            for (int x = 0; x < w; x++) {
                int c0 = line0[x];
                int c1 = line1[x];
                int a = Math.clamp((int) Math.round((c0 >>> 24) * invT + (c1 >>> 24) * t), 0, 255);
                int r = Math.clamp((int) Math.round(((c0 >> 16) & 0xFF) * invT + ((c1 >> 16) & 0xFF) * t), 0, 255);
                int g = Math.clamp((int) Math.round(((c0 >> 8) & 0xFF) * invT + ((c1 >> 8) & 0xFF) * t), 0, 255);
                int b = Math.clamp((int) Math.round((c0 & 0xFF) * invT + (c1 & 0xFF) * t), 0, 255);
                lineOut[x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            result.setRGB(0, y, w, 1, lineOut, 0, w);
        }
        return result;
    }

    /**
     * Performs linear cross-fade blending between two JavaFX {@link Image} raster maps.
     *
     * @param img0 earlier baseline raster
     * @param img1 later target raster
     * @param t    barycentric interpolation factor in {@code [0.0, 1.0]}
     * @return interpolated JavaFX {@link WritableImage}
     */
    public static Image interpolateImages(Image img0, Image img1, double t) {
        t = Math.clamp(t, 0.0, 1.0);
        int w = (int) Math.min(img0.getWidth(), img1.getWidth());
        int h = (int) Math.min(img0.getHeight(), img1.getHeight());

        WritableImage result = new WritableImage(w, h);
        PixelReader r0 = img0.getPixelReader();
        PixelReader r1 = img1.getPixelReader();
        PixelWriter pw = result.getPixelWriter();
        double invT = 1.0 - t;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c0 = r0.getArgb(x, y);
                int c1 = r1.getArgb(x, y);
                int a = Math.clamp((int) Math.round((c0 >>> 24) * invT + (c1 >>> 24) * t), 0, 255);
                int r = Math.clamp((int) Math.round(((c0 >> 16) & 0xFF) * invT + ((c1 >> 16) & 0xFF) * t), 0, 255);
                int g = Math.clamp((int) Math.round(((c0 >> 8) & 0xFF) * invT + ((c1 >> 8) & 0xFF) * t), 0, 255);
                int b = Math.clamp((int) Math.round((c0 & 0xFF) * invT + (c1 & 0xFF) * t), 0, 255);
                pw.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }
}
