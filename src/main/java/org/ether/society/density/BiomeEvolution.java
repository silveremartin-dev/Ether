/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Handles biome evolution over time based on human activity and climate.
 * 
 * Biome transitions:
 * - Overgrazing: PLAINS/FOREST → DESERT
 * - Deforestation: FOREST/JUNGLE → PLAINS
 * - Irrigation: DESERT → PLAINS (with tech + water)
 * - Climate change: TUNDRA ↔ FOREST, FOREST ↔ DESERT
 * - Reforestation: PLAINS → FOREST (if low population)
 */
public class BiomeEvolution {
    private static final Logger logger = LoggerFactory.getLogger(BiomeEvolution.class);

    // Thresholds

    private static final double DEFORESTATION_POP_DENSITY = 200; // Pop per cell triggering deforestation
    private static final double DESERTIFICATION_THRESHOLD = 0.3; // Water below this = desertification risk
    private static final double REFORESTATION_POP_MIN = 10; // Max pop for natural regrowth

    // Transition probabilities per tick
    private static final double DEFORESTATION_RATE = 0.002; // 0.2% per tick
    private static final double DESERTIFICATION_RATE = 0.001; // 0.1% per tick
    private static final double REFORESTATION_RATE = 0.0005; // 0.05% per tick
    private static final double IRRIGATION_RATE = 0.001; // 0.1% per tick with tech

    private final Random random = new Random();

    /**
     * Process biome evolution for all cells.
     * Should be called once per simulation tick (e.g., monthly).
     */
    public void tick(List<H3Cell> cells, int year) {
        int deforestations = 0;
        int desertifications = 0;
        int reforestations = 0;

        for (H3Cell cell : cells) {
            Biome current = cell.getBiome();
            if (current == null || current == Biome.OCEAN || current == Biome.DEEP_OCEAN) {
                continue;
            }

            Biome newBiome = processCell(cell, current);
            if (newBiome != current) {
                cell.setBiome(newBiome);

                if (newBiome == Biome.DESERT)
                    desertifications++;
                else if (newBiome == Biome.PLAINS && current == Biome.FOREST)
                    deforestations++;
                else if (newBiome == Biome.FOREST)
                    reforestations++;
            }
        }

        if (deforestations + desertifications + reforestations > 0) {
            logger.debug("Year {}: Biome changes - {} deforestations, {} desertifications, {} reforestations",
                    year, deforestations, desertifications, reforestations);
        }
    }

    private Biome processCell(H3Cell cell, Biome current) {
        int population = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 0;

        // Normalize water to 0-1 (max 1000)
        double waterRatio = Math.min(1.0, water / 1000.0);

        // Check for deforestation (high population in forests)
        if ((current == Biome.FOREST || current == Biome.JUNGLE) && population > DEFORESTATION_POP_DENSITY) {
            if (random.nextDouble() < DEFORESTATION_RATE * (population / DEFORESTATION_POP_DENSITY)) {
                // Remove wood resource
                cell.setWoodResource(wood * 0.5);
                return Biome.PLAINS;
            }
        }

        // Check for desertification (low water, high population stress)
        if ((current == Biome.PLAINS || current == Biome.HILLS) && waterRatio < DESERTIFICATION_THRESHOLD) {
            double stressFactor = population > 0 ? 1.0 + (population / 100.0) : 1.0;
            if (random.nextDouble() < DESERTIFICATION_RATE * stressFactor) {
                // Reduce resources
                cell.setFoodResource(food * 0.3);
                cell.setWaterResource(water * 0.5);
                return Biome.DESERT;
            }
        }

        // Check for reforestation (low population, good conditions)
        if (current == Biome.PLAINS && population <= REFORESTATION_POP_MIN && waterRatio > 0.5) {
            if (random.nextDouble() < REFORESTATION_RATE) {
                // Increase wood resource
                cell.setWoodResource(wood + 200);
                return Biome.FOREST;
            }
        }

        // Check for irrigation (desert with tech and water access)
        if (current == Biome.DESERT && population > 50 && waterRatio > 0.3) {
            // Assume tech level enables irrigation at pop > 50
            if (random.nextDouble() < IRRIGATION_RATE) {
                cell.setFoodResource(food + 100);
                return Biome.PLAINS;
            }
        }

        // Climate-driven changes (very slow, over millennia)
        // Warming: Tundra → Plains/Forest
        if (current == Biome.TUNDRA || current == Biome.SNOW) {
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 0;
            if (temp > 5 && random.nextDouble() < 0.0001) { // Very rare
                return Biome.PLAINS;
            }
        }

        // Cooling: Forest → Tundra (rare)
        if (current == Biome.FOREST) {
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15;
            if (temp < -5 && random.nextDouble() < 0.0001) {
                return Biome.TUNDRA;
            }
        }

        return current; // No change
    }

    /**
     * Get summary statistics for biome distribution.
     */
    public static BiomeStats getStats(List<H3Cell> cells) {
        BiomeStats stats = new BiomeStats();
        for (H3Cell cell : cells) {
            Biome b = cell.getBiome();
            if (b != null) {
                stats.increment(b);
            }
        }
        return stats;
    }

    /**
     * Statistics container for biome distribution.
     */
    public static class BiomeStats {
        private int[] counts = new int[Biome.values().length];

        public void increment(Biome biome) {
            counts[biome.ordinal()]++;
        }

        public int getCount(Biome biome) {
            return counts[biome.ordinal()];
        }

        public int getForestArea() {
            return counts[Biome.FOREST.ordinal()] + counts[Biome.JUNGLE.ordinal()];
        }

        public int getDesertArea() {
            return counts[Biome.DESERT.ordinal()];
        }

        public int getHabitableArea() {
            return counts[Biome.PLAINS.ordinal()] + counts[Biome.FOREST.ordinal()]
                    + counts[Biome.JUNGLE.ordinal()] + counts[Biome.HILLS.ordinal()]
                    + counts[Biome.BEACH.ordinal()];
        }

        @Override
        public String toString() {
            return String.format("Forest: %d, Desert: %d, Habitable: %d",
                    getForestArea(), getDesertArea(), getHabitableArea());
        }
    }
}
