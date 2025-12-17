/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles resource depletion and regeneration over time.
 * 
 * Resources:
 * - Renewable: Wood, Food, Fish, Water (regenerate based on biome)
 * - Non-Renewable: Minerals (deplete permanently when extracted)
 */
public class ResourceEvolution {
    private static final Logger logger = LoggerFactory.getLogger(ResourceEvolution.class);

    // Regeneration rates (per tick)
    private static final double WOOD_REGEN_RATE = 0.01; // 1% per tick
    private static final double FOOD_REGEN_RATE = 0.05; // 5% per tick (seasonal)
    private static final double FISH_REGEN_RATE = 0.03; // 3% per tick
    private static final double WATER_REGEN_RATE = 0.1; // 10% per tick (rainfall)

    // Depletion thresholds
    private static final double WOOD_DEPLETION_THRESHOLD = 50; // Below this, regrowth is slow
    private static final double FISH_OVERFISHING_THRESHOLD = 0.5; // Extraction rate vs stock

    /**
     * Process resource evolution for all cells.
     * Should be called monthly.
     */
    public void tick(List<H3Cell> cells, int month) {
        for (H3Cell cell : cells) {
            regenerateRenewables(cell, month);
            checkOverextraction(cell);
        }
    }

    /**
     * Regenerate renewable resources based on biome and conditions.
     */
    private void regenerateRenewables(H3Cell cell, int month) {
        Biome biome = cell.getBiome();
        if (biome == null)
            return;

        // Wood regeneration (forests only)
        if (biome == Biome.FOREST || biome == Biome.JUNGLE) {
            double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 0;
            double maxWood = biome == Biome.JUNGLE ? 1500 : 1000;

            // Slower regrowth if heavily depleted
            double regenRate = wood < WOOD_DEPLETION_THRESHOLD
                    ? WOOD_REGEN_RATE * 0.5
                    : WOOD_REGEN_RATE;

            cell.setWoodResource(Math.min(maxWood, wood + (maxWood * regenRate)));
        }

        // Fish regeneration (coastal/ocean)
        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
            double fish = cell.getBiomassFish() != null ? cell.getBiomassFish() : 0;
            double maxFish = biome == Biome.DEEP_OCEAN ? 800 : 500;

            // Seasonal spawning (spring boost)
            double seasonBoost = (month >= 2 && month <= 5) ? 1.5 : 1.0;

            cell.setBiomassFish(Math.min(maxFish, fish + (maxFish * FISH_REGEN_RATE * seasonBoost)));
        }

        // Water regeneration (based on rainfall)
        double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 0;
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;
        double maxWater = 1000;

        // Rainfall adds to water, evaporation removes some
        double rainContrib = rainfall * 0.001; // mm to resource units
        double evaporation = water * 0.02; // 2% evaporates

        // Desert has low max water
        if (biome == Biome.DESERT) {
            maxWater = 100;
            evaporation *= 3; // Triple evaporation in desert
        }

        cell.setWaterResource(Math.max(0, Math.min(maxWater, water + rainContrib - evaporation)));

        // Natural food replenishment (wild sources)
        if (biome != Biome.OCEAN && biome != Biome.DEEP_OCEAN) {
            double natural = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0;
            double maxNatural = getMaxNaturalBiomass(biome);

            // Seasonal growth (higher in spring/summer)
            double seasonFactor = getSeasonalFactor(cell.getLatitude(), month);

            cell.setBiomassNatural(Math.min(maxNatural, natural + (maxNatural * FOOD_REGEN_RATE * seasonFactor)));
        }
    }

    /**
     * Check for overextraction and apply penalties.
     */
    private void checkOverextraction(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0)
            return;

        // Fish overextraction
        double fish = cell.getBiomassFish() != null ? cell.getBiomassFish() : 0;
        double fishConsumption = pop * 0.1; // Each person consumes 0.1 fish units/tick

        if (fish > 0 && fishConsumption / fish > FISH_OVERFISHING_THRESHOLD) {
            // Overfishing: stock collapses faster
            cell.setBiomassFish(Math.max(0, fish - fishConsumption * 1.5));
        } else {
            cell.setBiomassFish(Math.max(0, fish - fishConsumption));
        }

        // Wood overconsumption (for fire/building)
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 0;
        double woodConsumption = pop * 0.05; // Each person uses 0.05 wood units/tick

        cell.setWoodResource(Math.max(0, wood - woodConsumption));

        // Water consumption
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;
        double waterConsumption = pop * 0.02;

        cell.setWaterResource(Math.max(0, water - waterConsumption));
    }

    private double getMaxNaturalBiomass(Biome biome) {
        return switch (biome) {
            case JUNGLE -> 1000;
            case FOREST -> 800;
            case PLAINS -> 500;
            case HILLS -> 400;
            case BEACH -> 200;
            case MOUNTAINS -> 150;
            case TUNDRA -> 100;
            case DESERT -> 50;
            case SNOW -> 30;
            default -> 0;
        };
    }

    private double getSeasonalFactor(double latitude, int month) {
        boolean isNorthern = latitude >= 0;
        int growthPeak = isNorthern ? 5 : 11; // June for north, December for south

        int monthsFromPeak = Math.abs(month - growthPeak);
        if (monthsFromPeak > 6)
            monthsFromPeak = 12 - monthsFromPeak;

        // 0.5 in winter, 1.0 at peak
        return 0.5 + 0.5 * (1 - monthsFromPeak / 6.0);
    }

    /**
     * Extract non-renewable resources (mining).
     * Returns amount actually extracted.
     */
    public double extractMineral(H3Cell cell, double requestedAmount) {
        // Minerals are tracked separately - for now use a simple marker
        // In future: cell.getMineralDeposit(type)

        // Placeholder logic
        double available = 100; // Would come from cell data
        double extracted = Math.min(available, requestedAmount);

        // Minerals don't regenerate
        // cell.setMineralDeposit(available - extracted);

        return extracted;
    }
}
