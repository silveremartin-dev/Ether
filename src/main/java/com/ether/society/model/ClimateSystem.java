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
package com.ether.society.model;

import com.ether.society.config.Configuration;

/**
 * Manages seasonal climate updates for the world.
 * Calculates temperature variations based on latitude, elevation, and time of
 * year.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class ClimateSystem {
    private final Configuration.ClimateConfig config;

    public ClimateSystem(Configuration.ClimateConfig config) {
        this.config = config;
    }

    /**
     * Updates climate for all cells based on the current month.
     *
     * @param world World to update
     * @param month Current month (0-11)
     */
    public void update(World world, int month) {
        int width = world.getWidth();
        int height = world.getHeight();

        // Calculate global seasonal factor (-1 = winter, 1 = summer in Northern
        // Hemisphere)
        double globalSeasonFactor = -Math.cos((month / 12.0) * 2.0 * Math.PI);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = world.getCell(x, y);
                if (cell == null)
                    continue;

                // Latitude factor (0 = North Pole, 1 = South Pole, 0.5 = Equator)
                double latitude = (double) y / height;

                // Determine hemisphere seasonal factor
                double localSeasonFactor;
                if (latitude < 0.5) {
                    localSeasonFactor = globalSeasonFactor; // Northern Hemisphere
                } else {
                    localSeasonFactor = -globalSeasonFactor; // Southern Hemisphere (reversed)
                }

                // Latitude variance (stronger at poles, weaker at equator)
                double latitudeVariance = Math.abs(latitude - 0.5) * 2.0;

                // Seasonal temperature change
                double seasonalTempChange = localSeasonFactor * config.seasonalVariation() * latitudeVariance;

                // Calculate base temperature from latitude and elevation
                double latitudeFactor = 1.0 - Math.abs(y - height / 2.0) / (height / 2.0);
                double baseTemp = (latitudeFactor * config.latitudeEffect()) - 10.0;
                baseTemp -= cell.getElevation() * config.elevationLapseRate();

                // Apply seasonal change and global offset
                double currentTemp = baseTemp + seasonalTempChange + world.getGlobalTemperatureOffset();
                cell.setTemperature(currentTemp);
            }
        }
    }
}
