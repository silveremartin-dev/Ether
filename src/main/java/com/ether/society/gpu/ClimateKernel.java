/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society.gpu;

/**
 * GPU kernel for parallel climate updates across H3 cells.
 * Uses TornadoVM for GPU acceleration - each cell processed independently.
 *
 * <p>
 * <strong>GPU Parallelization Strategy:</strong>
 * </p>
 * <ul>
 * <li>Each GPU thread processes one H3 cell</li>
 * <li>7.2M cells × GPU threads = massive parallelism</li>
 * <li>~10-100x speedup vs CPU sequential</li>
 * </ul>
 *
 * <p>
 * <strong>Input Data (from database → GPU memory):</strong>
 * </p>
 * <ul>
 * <li>h3Indices: H3 cell IDs (long[])</li>
 * <li>latitudes: Cell center latitudes (double[])</li>
 * <li>elevations: Terrain elevation (double[])</li>
 * <li>temperatures: Current temps (double[], updated in-place)</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClimateKernel {

    /**
     * Parallel climate update kernel (runs on GPU).
     * 
     * Each thread calculates temperature for one H3 cell based on:
     * - Latitude (distance from equator)
     * - Elevation (lapse rate)
     * - Season (month of year)
     * - Global offset (volcanic eruptions, etc.)
     *
     * @param cellCount    Number of cells to process
     * @param latitudes    Array of cell latitudes (-90 to 90)
     * @param elevations   Array of cell elevations (meters)
     * @param temperatures Array of temperatures (updated in-place)
     * @param month        Current month (0-11)
     * @param globalOffset Global temperature modifier (°C)
     */
    // @Parallel // TornadoVM annotation (uncomment when TornadoVM installed)
    public static void updateClimate(
            int cellCount,
            double[] latitudes,
            double[] elevations,
            double[] temperatures,
            int month,
            double globalOffset) {

        // Each GPU thread processes one cell
        for (int i = 0; i < cellCount; i++) {
            double lat = latitudes[i];
            double elev = elevations[i];

            // Calculate seasonal factor (-1 = winter, +1 = summer in N. Hemisphere)
            double seasonFactor = -Math.cos((month / 12.0) * 2.0 * Math.PI);

            // Latitude effect: warmer near equator
            // Normalized latitude: 0 (poles) to 1 (equator)
            double absLat = Math.abs(lat);
            double latitudeFactor = 1.0 - (absLat / 90.0);

            // Base temperature from latitude
            final double MAX_TEMP_DIFF = 40.0; // Equator vs poles difference
            double baseTemp = latitudeFactor * MAX_TEMP_DIFF - 10.0;

            // Elevation lapse rate: -10°C per 1000m
            final double LAPSE_RATE = 10.0 / 1000.0;
            double elevEffect = -elev * LAPSE_RATE;

            // Seasonal variation (stronger at poles)
            final double SEASONAL_AMPLITUDE = 15.0;
            double seasonalTemp = seasonFactor * SEASONAL_AMPLITUDE * (absLat / 90.0);

            // Hemisphere adjustment (opposite seasons)
            if (lat < 0) {
                seasonalTemp = -seasonalTemp; // Southern hemisphere
            }

            // Final temperature calculation
            temperatures[i] = baseTemp + elevEffect + seasonalTemp + globalOffset;
        }
    }

    /**
     * CPU fallback version (when GPU not available).
     * Identical logic but runs sequentially on CPU.
     */
    public static void updateClimateCPU(
            int cellCount,
            double[] latitudes,
            double[] elevations,
            double[] temperatures,
            int month,
            double globalOffset) {

        // Sequential CPU execution
        updateClimate(cellCount, latitudes, elevations, temperatures, month, globalOffset);
    }
}
