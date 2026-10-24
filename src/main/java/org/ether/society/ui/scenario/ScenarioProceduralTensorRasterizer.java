/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Procedural raster generator for multi-field cultural tensors.
 * <p>
 * Simulates spatial cultural distributions based on parameterized Langevin-SDE formulas:
 * \[
 *   d\mathbf{C}_i = \alpha \nabla^2 \mathbf{C}_i \, dt + \sigma \, d\mathbf{W}_t
 * \]
 * coupling geographical coordinates, elevation barriers, and stochastic seeds.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ScenarioProceduralTensorRasterizer {

    private ScenarioProceduralTensorRasterizer() {
        // Utility class
    }

    /**
     * Generates an equirectangular procedural preview raster for a cultural tensor.
     *
     * @param tensorIdx tensor dimension index
     * @param w target image width
     * @param h target image height
     * @param p1 parameter 1 value
     * @param p2 parameter 2 value
     * @param p3 parameter 3 value
     * @param seed random seed
     * @return generated JavaFX Image
     */
    public static Image generateProceduralTensorRasterImage(int tensorIdx, int w, int h, double p1, double p2, double p3, long seed) {
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x / (double) w) * 360.0;
                Color c = evaluateProceduralTensorColor(tensorIdx, lat, lon, 500.0, p1, p2, p3, seed);
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    /**
     * Evaluates the color of a procedural tensor at specific spherical coordinates.
     *
     * @param tIndex tensor index
     * @param lat latitude in degrees [-90..90]
     * @param lon longitude in degrees [-180..180]
     * @param elevation terrain elevation in meters
     * @param p1 dimension-specific parameter 1
     * @param p2 dimension-specific parameter 2
     * @param p3 dimension-specific parameter 3
     * @param seed stochastic seed
     * @return evaluated Color
     */
    public static Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        double noiseX = (lon + 180.0) / 360.0;
        double noiseY = (lat + 90.0) / 180.0;
        double pseudoNoise = Math.sin(noiseX * 12.0 + seed % 100) * Math.cos(noiseY * 12.0 + (seed / 100) % 100);
        double pseudoNoise2 = Math.sin(noiseX * 24.0 + (seed / 10) % 100) * Math.cos(noiseY * 24.0 + seed % 50);
        double combinedNoise = Math.clamp(0.5 + 0.35 * pseudoNoise + 0.15 * pseudoNoise2, 0.0, 1.0);

        return switch (tIndex % 9) {
            case 0 -> {
                double reliefFactor = Math.clamp(elevation / 3000.0, 0.0, 1.0) * p3;
                double val = Math.clamp((noiseY * 0.7 + noiseX * 0.3 + combinedNoise * 0.3 * p1 + reliefFactor * 0.2) * (p2 / 4.0), 0.0, 1.0);
                yield Color.hsb(val * 300.0, 0.75, 0.90);
            }
            case 1 -> {
                double val = Math.clamp(Math.abs(Math.sin((lat + p1 * 0.01) * 0.1) * Math.cos((lon + p2 * 10.0) * 0.1) + combinedNoise * p3 * 0.3), 0.0, 1.0);
                yield Color.hsb(180.0 + val * 120.0, 0.80, 0.85);
            }
            case 2 -> {
                double altNorm = Math.clamp(elevation / 3000.0, 0.0, 1.0);
                double val = Math.clamp(altNorm * 0.5 + combinedNoise * 0.5 * p1, 0.0, 1.0);
                yield Color.hsb(40.0 + val * 200.0, 0.85, 0.95);
            }
            case 3 -> {
                double val = Math.clamp(combinedNoise * p2, 0.0, 1.0);
                yield Color.hsb((val * 360.0 + seed % 360) % 360.0, 0.75, 0.85);
            }
            case 4 -> {
                double val = Math.clamp(combinedNoise * (p2 / 5.0) + p1 * 0.05, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 5 -> {
                double val = Math.clamp(combinedNoise * p3 + (p1 / 30.0), 0.0, 1.0);
                yield Color.hsb(35.0 + val * 30.0, 0.85, 0.95);
            }
            case 6 -> {
                double val = Math.clamp(combinedNoise * p2 + (p1 / 8.0) * 0.5, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 7 -> {
                double val = Math.clamp(combinedNoise * p1 + p2 * 5.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 8 -> {
                double val = Math.clamp(combinedNoise * p1 + p3 * 2.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            default -> {
                double val = Math.clamp(combinedNoise, 0.0, 1.0);
                yield Color.gray(val);
            }
        };
    }
}
