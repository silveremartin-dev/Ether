/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.paint.Color;

/**
 * Perceptually uniform scientific colormaps for high-fidelity GIS heatmaps.
 * Supports Google TURBO, Matplotlib VIRIDIS, MAGMA, SPECTRAL, and TERRAIN palettes.
 */
public enum ScientificColorMap {
    RAINBOW("Arc-en-ciel Classique (Rainbow / Jet)"),
    TURBO("Google Turbo (Uniform Rainbow)"),
    VIRIDIS("Viridis (Sequential Blue-Yellow)"),
    MAGMA("Magma (High-Contrast Heatmap)"),
    SPECTRAL("Spectral (Diverging Red-Blue)"),
    TERRAIN("Relief Topographique (Terrain)");

    private final String displayName;

    ScientificColorMap(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Map a normalized scalar value [0.0, 1.0] to a Color using the selected palette.
     */
    public Color getColor(double norm) {
        double v = Math.clamp(norm, 0.0, 1.0);
        return switch (this) {
            case RAINBOW -> sampleRainbow(v);
            case TURBO -> sampleTurbo(v);
            case VIRIDIS -> sampleViridis(v);
            case MAGMA -> sampleMagma(v);
            case SPECTRAL -> sampleSpectral(v);
            case TERRAIN -> sampleTerrain(v);
        };
    }

    // --- 1. Google Turbo Colormap (Perceptually Uniform Rainbow Replacement) ---
    private static Color sampleTurbo(double x) {
        // Polynomial approximation of Google Turbo Colormap
        double r = 0.1357 + x * ( 4.5155 + x * (-24.5516 + x * ( 42.1361 + x * (-27.1443 + x * 5.4402))));
        double g = 0.0914 + x * ( 2.0791 + x * (  6.2415 + x * (-20.8931 + x * ( 20.3082 + x * -6.8202))));
        double b = 0.1067 + x * (12.5841 + x * (-61.9056 + x * (108.7770 + x * (-80.7963 + x * 21.9213))));
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    // --- 2. Matplotlib Viridis Colormap (Deep Blue -> Teal -> Green -> Yellow) ---
    private static Color sampleViridis(double x) {
        Color[] stops = {
            Color.rgb(68, 1, 84),
            Color.rgb(59, 82, 139),
            Color.rgb(33, 145, 140),
            Color.rgb(94, 201, 98),
            Color.rgb(253, 231, 37)
        };
        return interpolateStops(stops, x);
    }

    // --- 3. Magma Colormap (Black -> Dark Purple -> Crimson -> Orange -> Pale Yellow) ---
    private static Color sampleMagma(double x) {
        Color[] stops = {
            Color.rgb(0, 0, 4),
            Color.rgb(81, 18, 124),
            Color.rgb(182, 54, 121),
            Color.rgb(251, 136, 97),
            Color.rgb(252, 253, 191)
        };
        return interpolateStops(stops, x);
    }

    // --- 4. Spectral Colormap (Crimson Red -> Gold -> Cyan -> Deep Royal Blue) ---
    private static Color sampleSpectral(double x) {
        Color[] stops = {
            Color.rgb(158, 1, 66),
            Color.rgb(213, 62, 79),
            Color.rgb(253, 174, 97),
            Color.rgb(254, 224, 139),
            Color.rgb(230, 245, 152),
            Color.rgb(102, 194, 165),
            Color.rgb(50, 136, 189),
            Color.rgb(94, 79, 162)
        };
        return interpolateStops(stops, x);
    }

    // --- 5. Topographical Terrain Colormap ---
    private static Color sampleTerrain(double x) {
        Color[] stops = {
            Color.rgb(10, 35, 80),   // Deep ocean
            Color.rgb(50, 130, 200), // Coastal water
            Color.rgb(240, 230, 140),// Sand / Beach
            Color.rgb(34, 139, 34),  // Lowland forest
            Color.rgb(160, 120, 60), // Mountain rock
            Color.rgb(255, 255, 255) // Glacier / Snow
        };
        return interpolateStops(stops, x);
    }

    // --- 6. Classic Rainbow / Jet Colormap (Deep Blue -> Cyan -> Green -> Yellow -> Orange -> Red -> Dark Red) ---
    private static Color sampleRainbow(double x) {
        Color[] stops = {
            Color.rgb(0, 0, 140),    // Dark Blue
            Color.rgb(0, 70, 255),   // Blue
            Color.rgb(0, 220, 255),  // Cyan
            Color.rgb(0, 220, 50),   // Green
            Color.rgb(240, 230, 0),  // Yellow
            Color.rgb(255, 120, 0),  // Orange
            Color.rgb(230, 20, 0),   // Red
            Color.rgb(130, 0, 0)     // Dark Red
        };
        return interpolateStops(stops, x);
    }

    private static Color interpolateStops(Color[] stops, double x) {
        if (x <= 0.0) return stops[0];
        if (x >= 1.0) return stops[stops.length - 1];

        double scaled = x * (stops.length - 1);
        int idx = (int) scaled;
        double frac = scaled - idx;

        Color c1 = stops[idx];
        Color c2 = stops[Math.min(stops.length - 1, idx + 1)];

        double r = c1.getRed() + frac * (c2.getRed() - c1.getRed());
        double g = c1.getGreen() + frac * (c2.getGreen() - c1.getGreen());
        double b = c1.getBlue() + frac * (c2.getBlue() - c1.getBlue());
        double a = c1.getOpacity() + frac * (c2.getOpacity() - c1.getOpacity());

        return Color.color(
            Math.clamp(r, 0.0, 1.0),
            Math.clamp(g, 0.0, 1.0),
            Math.clamp(b, 0.0, 1.0),
            Math.clamp(a, 0.0, 1.0)
        );
    }
}
