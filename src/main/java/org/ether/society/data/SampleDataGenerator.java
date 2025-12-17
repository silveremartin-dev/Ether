/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
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
package org.ether.society.data;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates sample data for a specific region (for MVP demo).
 * Generates realistic elevation and biome data for testing without requiring
 * real SRTM/MODIS data download.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
public class SampleDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SampleDataGenerator.class);
    private final H3Service h3Service;
    private final Random random;

    public SampleDataGenerator(long seed) {
        this.h3Service = new H3Service();
        this.random = new Random(seed);
    }

    /**
     * Generates sample H3 cells for a region (e.g., Europe).
     *
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLng Minimum longitude
     * @param maxLng Maximum longitude
     * @return List of H3 cells with realistic data
     */
    public List<H3Cell> generateRegion(double minLat, double maxLat, double minLng, double maxLng) {
        logger.info("Generating sample region: lat[{}, {}], lng[{}, {}]",
                minLat, maxLat, minLng, maxLng);

        List<H3Cell> cells = new ArrayList<>();
        java.util.Set<Long> seenH3 = new java.util.HashSet<>();

        double step = 0.1; // ~10km sampling

        for (double lat = minLat; lat <= maxLat; lat += step) {
            for (double lng = minLng; lng <= maxLng; lng += step) {
                long h3Index = h3Service.latLngToH3(lat, lng);

                if (!seenH3.contains(h3Index)) {
                    seenH3.add(h3Index);

                    LatLng center = h3Service.h3ToLatLng(h3Index);
                    H3Cell cell = new H3Cell(h3Index, center.lat, center.lng);

                    // Generate realistic data
                    generateCellData(cell, center.lat, center.lng);

                    cells.add(cell);
                }
            }
        }

        logger.info("Generated {} H3 cells for region", cells.size());
        return cells;
    }

    /**
     * Generates realistic elevation, temperature, biome for a cell.
     */
    private void generateCellData(H3Cell cell, double lat, double lng) {
        // Elevation: use distance from "mountain center" to simulate terrain
        double mountainCenterLat = (cell.getLatitude() + 45.0) / 2.0;
        double mountainCenterLng = (cell.getLongitude() + 10.0) / 2.0;

        double distToMountain = Math.sqrt(
                Math.pow(lat - mountainCenterLat, 2) +
                        Math.pow(lng - mountainCenterLng, 2));

        // Elevation: higher near "mountains", lower far away
        double baseElev = Math.max(0, 3000 - distToMountain * 300);
        double noise = (random.nextDouble() - 0.5) * 200;
        double elevation = Math.max(-100, baseElev + noise);

        cell.setElevation(elevation);

        // Temperature: based on latitude and elevation
        double absLat = Math.abs(lat);
        double baseTemp = (1.0 - absLat / 90.0) * 35.0;
        double tempFromElev = -elevation * 0.006; // Lapse rate
        cell.setTemperature(baseTemp + tempFromElev);

        // Rainfall: random with latitude influence
        double baseRain = 500 + random.nextDouble() * 1000;
        cell.setRainfall(baseRain);

        // Biome: determined by elevation and temperature
        Biome biome = determineBiome(elevation, cell.getTemperature(), cell.getRainfall());
        cell.setBiome(biome);

        // Resources: based on biome
        assignResources(cell, biome);
    }

    private Biome determineBiome(double elevation, double temp, double rainfall) {
        if (elevation < 0)
            return Biome.OCEAN;
        if (elevation < 10)
            return Biome.BEACH;
        if (elevation > 2500) {
            return temp < -5 ? Biome.SNOW : Biome.MOUNTAINS;
        }
        if (temp < 0 && rainfall < 300)
            return Biome.TUNDRA;
        if (temp > 25 && rainfall > 1500)
            return Biome.JUNGLE;
        if (temp > 20 && rainfall < 250)
            return Biome.DESERT;
        if (elevation > 500 && rainfall > 600)
            return Biome.HILLS;
        if (rainfall > 800)
            return Biome.FOREST;
        return Biome.PLAINS;
    }

    private void assignResources(H3Cell cell, Biome biome) {
        switch (biome) {
            case OCEAN -> {
                cell.setFoodResource(random.nextDouble() * 300); // Fish
                cell.setWaterResource(1000.0);
                cell.setWoodResource(0.0);
            }
            case JUNGLE, FOREST -> {
                cell.setFoodResource(random.nextDouble() * 600);
                cell.setWaterResource(random.nextDouble() * 800);
                cell.setWoodResource(random.nextDouble() * 900);
            }
            case PLAINS -> {
                cell.setFoodResource(random.nextDouble() * 700);
                cell.setWaterResource(random.nextDouble() * 400);
                cell.setWoodResource(random.nextDouble() * 200);
            }
            case DESERT -> {
                cell.setFoodResource(random.nextDouble() * 100);
                cell.setWaterResource(random.nextDouble() * 50);
                cell.setWoodResource(0.0);
            }
            default -> {
                cell.setFoodResource(random.nextDouble() * 300);
                cell.setWaterResource(random.nextDouble() * 300);
                cell.setWoodResource(random.nextDouble() * 300);
            }
        }
    }

    /**
     * Generate sample data for Europe region (for MVP).
     */
    public static List<H3Cell> generateEuropeSample() {
        SampleDataGenerator generator = new SampleDataGenerator(42);
        // Europe: roughly 35 deg N to 70 deg N, -10 deg W to 40 deg E
        return generator.generateRegion(35, 70, -10, 40);
    }

    public static void main(String[] args) {
        logger.info("=== Sample Data Generator ===");
        List<H3Cell> cells = generateEuropeSample();
        logger.info("Generated {} cells for Europe", cells.size());
        logger.info("Sample cell: lat={}, lng={}, elev={}, biome={}",
                cells.get(0).getLatitude(),
                cells.get(0).getLongitude(),
                cells.get(0).getElevation(),
                cells.get(0).getBiome());
    }
}
