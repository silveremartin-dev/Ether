/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Degradation, Climate Stress, Pollution & Overfishing Engine.
 * Models:
 * 1. <b>Déforestation (Deforestation & Canopy Loss)</b>: Timber extraction and agricultural land clearing,
 *    reducing natural biomass, increasing albedo, and disrupting regional evapotranspiration.
 * 2. <b>Pollution & Dispersion aux Sites Inaccessibles</b>: Industrial/mining waste generation in populated cells
 *    and atmospheric/river dispersion diffusing toxic contaminants even into unpopulated, high-altitude or remote sites.
 * 3. <b>Surpêche (Marine Resource Overfishing)</b>: Logistic fish stock harvesting ($r \cdot K_{fish}$). Over-harvesting
 *    leads to fishery collapse and coastal food crises.
 * 4. <b>Sécheresses, Famines & Migrations Démographiques</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EcologicalDegradationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcologicalDegradationEngine.class);

    private static final double CALORIC_REQUIREMENT_PER_CAPITA = 0.5;

    /**
     * Executes one ecological, pollution, overfishing & socio-demographic simulation tick across all cells.
     */
    public static void processEcologicalDegradation(List<H3Cell> cells, double techLevel) {
        if (cells == null || cells.isEmpty()) return;

        int droughtEvents = 0;
        int famineEvents = 0;
        int totalMigrants = 0;

        // ── Phase 1: Local Environmental, Deforestation, Overfishing & Famine ──
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            double effectiveTech = cell.getTechnologyLevel() > 0 ? cell.getTechnologyLevel() : techLevel;
            Biome biome = cell.getBiome();

            // 1. Pollution Generation (Populated / Industrial / Mining Cells)
            if (pop > 0) {
                double miningActivity = cell.getResourceMetal() != null ? cell.getResourceMetal() * 0.05 : 0.0;
                double industrialPollution = pop * (0.02 + 0.08 * Math.pow(effectiveTech / 10.0, 1.5)) + miningActivity;

                double currPollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
                cell.setPollutionLevel(Math.min(1000.0, currPollution + industrialPollution));
            } else {
                // Natural attenuation of pollution in clean environment
                double currPollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
                cell.setPollutionLevel(Math.max(0.0, currPollution * 0.95 - 1.0));
            }

            // Pollution impact on health, lifespan, and natural biomass
            double pLevel = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            if (pLevel > 100.0) {
                cell.setLifespan(Math.max(15.0, cell.getLifespan() - (pLevel / 200.0)));
                cell.setFertility(Math.max(0.5, cell.getFertility() - (pLevel / 400.0)));
                cell.setBiomassNatural(Math.max(0.0, cell.getBiomassNatural() - (pLevel * 0.1)));
            }

            // 2. Surpêche (Overfishing Dynamics in Ocean & Coastal Biomes)
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
                double currentFishStock = cell.getBiomassFish() != null ? cell.getBiomassFish() : 800.0;
                double maxStockCapacity = 1000.0;

                // Natural logistic regeneration of fish stock (r = 0.2)
                double regeneration = 0.20 * currentFishStock * (1.0 - (currentFishStock / maxStockCapacity));

                // Fishing harvest effort by nearby population & tech
                double fishingEffort = pop * (0.3 + 0.15 * effectiveTech);

                double updatedStock = currentFishStock + regeneration - fishingEffort;
                // Impact of water pollution on marine stock
                if (pLevel > 50.0) {
                    updatedStock -= pLevel * 0.2;
                }

                cell.setBiomassFish(Math.max(0.0, Math.min(maxStockCapacity, updatedStock)));
                continue;
            }

            if (cell.getElevation() <= 0) continue;

            if (pop <= 0) {
                // Natural recovery when unoccupied
                cell.setSoilOrganicCarbon(Math.min(100.0, cell.getSoilOrganicCarbon() + 0.5));
                cell.setBiomassNatural(Math.min(1000.0, cell.getBiomassNatural() + 2.0));
                cell.setWaterResource(Math.min(1000.0, cell.getWaterResource() + 5.0));
                continue;
            }

            // 3. Drought Simulation (Sécheresse)
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 0.0;
            double temp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            double hydroStress = 0.0;
            if (rain < 300.0 || temp > 35.0) {
                hydroStress = Math.min(1.0, (300.0 - rain) / 300.0 + Math.max(0.0, temp - 35.0) * 0.03);
                droughtEvents++;

                double aquiferDepletion = hydroStress * 200.0;
                cell.setAccessibleAquifer(Math.max(0.0, cell.getAccessibleAquifer() - aquiferDepletion));
                cell.setWaterResource(Math.max(0.0, cell.getWaterResource() - hydroStress * 150.0));
                cell.setBiomassAgriculture(Math.max(0.0, cell.getBiomassAgriculture() * (1.0 - 0.4 * hydroStress)));
            } else {
                cell.setWaterResource(Math.min(1000.0, cell.getWaterResource() + (rain / 100.0) * 10.0));
            }

            // 4. Famine Simulation (Famine)
            double totalFood = (cell.getFoodResource() != null ? cell.getFoodResource() : 0.0)
                    + (cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0)
                    + (cell.getBiomassFish() != null ? cell.getBiomassFish() : 0.0);

            double foodRequired = pop * CALORIC_REQUIREMENT_PER_CAPITA;

            if (totalFood < foodRequired) {
                famineEvents++;
                double foodDeficitRatio = Math.min(1.0, (foodRequired - totalFood) / foodRequired);

                cell.setLifespan(Math.max(15.0, cell.getLifespan() - 3.0 * foodDeficitRatio));
                cell.setFertility(Math.max(0.5, cell.getFertility() - 1.5 * foodDeficitRatio));

                int mortality = (int) (pop * 0.08 * foodDeficitRatio);
                cell.setPopulation(Math.max(0, pop - mortality));
                cell.setFoodResource(0.0);

                cell.updateAgePyramidFromTotal(effectiveTech);
            } else {
                cell.setFoodResource(Math.min(2000.0, totalFood - foodRequired * 0.5));
                cell.setLifespan(Math.min(85.0, cell.getLifespan() + 0.2));
                cell.setFertility(Math.min(7.0, cell.getFertility() + 0.05));
            }

            // 5. Déforestation & Impact sur le Biome
            double woodDemand = pop * 0.12;
            if (cell.getBiomassNatural() > woodDemand) {
                cell.setBiomassNatural(cell.getBiomassNatural() - woodDemand);
                cell.setWoodResource(Math.max(0.0, cell.getWoodResource() - woodDemand));
            } else {
                cell.setBiomassNatural(0.0);
                cell.setWoodResource(0.0);
            }

            // Déforestation sévère (biomasse naturelle < 100) dégrade les sols et les pluies
            if (cell.getBiomassNatural() < 100.0 && (biome == Biome.FOREST || biome == Biome.JUNGLE)) {
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - 0.5));
                cell.setRainfall(Math.max(100.0, cell.getRainfall() * 0.98)); // Evapotranspiration loss
            }

            // 6. Pression Démographique & Intensification Agricole (Demographic Density Strain)
            double carryingCap = calculateCarryingCapacity(cell, effectiveTech);
            double demoPressure = pop / Math.max(1.0, carryingCap);

            if (demoPressure > 0.8) {
                // High density forces agricultural intensification (terracing, double cropping, wet rice)
                double yieldIntensification = 1.0 + Math.min(0.5, (demoPressure - 0.8) * 0.4 * (1.0 + effectiveTech * 0.2));
                cell.setBiomassAgriculture(Math.min(3000.0, cell.getBiomassAgriculture() * yieldIntensification));

                // Increases social inequality & friction (Gini index rises under density strain)
                cell.setGiniIndex(Math.min(1.0, cell.getGiniIndex() + 0.01 * demoPressure));
            }

            cell.calculateMovementFriction(effectiveTech);
        }

        // ── Phase 2: Pollution Atmospheric & River Dispersion (Sites Inaccessibles) ─
        processPollutionDispersion(cells);

        // ── Phase 3: Demographic Migration Flux (Migrations) ───────────────────
        totalMigrants = processMigrationFlux(cells, techLevel);

        if (droughtEvents > 0 || famineEvents > 0 || totalMigrants > 0) {
            logger.debug("Simulation Tick: {} drought cells, {} famine cells, {} climate migrants relocated.",
                    droughtEvents, famineEvents, totalMigrants);
        }
    }

    /**
     * Atmospheric and aquatic dispersion of toxic pollution across neighboring cells,
     * diffusing contaminants even into unpopulated, high-altitude or remote wilderness sites.
     */
    private static void processPollutionDispersion(List<H3Cell> cells) {
        int count = cells.size();
        if (count < 2) return;

        double[] pollutionDeltas = new double[count];

        for (int i = 0; i < count; i++) {
            H3Cell src = cells.get(i);
            double srcPollution = src.getPollutionLevel() != null ? src.getPollutionLevel() : 0.0;

            if (srcPollution > 20.0) {
                // Diffuse 12% of pollution to nearby regional cells (wind & river transport)
                double overflow = srcPollution * 0.12;
                pollutionDeltas[i] -= overflow;

                int range = Math.min(count, 15);
                int start = Math.max(0, i - range / 2);
                int end = Math.min(count - 1, i + range / 2);
                int targetCount = Math.max(1, (end - start));

                double share = overflow / targetCount;
                for (int j = start; j <= end; j++) {
                    if (j != i) {
                        pollutionDeltas[j] += share;
                    }
                }
            }
        }

        // Apply pollution deltas to all cells (including unpopulated / remote wilderness)
        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            double newPollution = Math.min(1000.0, Math.max(0.0, c.getPollutionLevel() + pollutionDeltas[i]));
            c.setPollutionLevel(newPollution);
        }
    }

    private static int processMigrationFlux(List<H3Cell> cells, double defaultTech) {
        int totalMigrated = 0;
        int count = cells.size();

        double[] habitability = new double[count];

        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            if (c.getElevation() <= 0 || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN) {
                habitability[i] = -1.0;
                continue;
            }

            double food = c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            double water = c.getWaterResource() != null ? c.getWaterResource() : 0.0;
            double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;
            double friction = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            double pollution = c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0;
            int pop = c.getPopulation() != null ? c.getPopulation() : 0;

            double tempComfort = Math.exp(-Math.pow(temp - 18.0, 2) / 200.0);
            double foodPerCapita = pop > 0 ? food / pop : org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
            double foodNorm = Math.min(1.0, foodPerCapita / org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);

            double score = (foodNorm * 0.35) + (water / 500.0) * 0.25 + (tempComfort * 0.25) - (pollution / 500.0) * 0.15;
            score = score / Math.max(0.5, friction * 0.5);

            habitability[i] = Math.max(0.001, score);
        }

        for (int i = 0; i < count; i++) {
            H3Cell source = cells.get(i);
            if (habitability[i] < 0 || source.getPopulation() <= 0) continue;

            double srcScore = habitability[i];
            int pop = source.getPopulation();

            if (srcScore < 0.35 && pop > 10) {
                int migrants = (int) (pop * Math.min(0.30, (0.35 - srcScore) * 0.8));
                if (migrants <= 0) continue;

                int bestTargetIdx = -1;
                double bestTargetScore = srcScore;

                int searchRange = Math.min(count, 30);
                int startIdx = Math.max(0, i - searchRange / 2);
                int endIdx = Math.min(count - 1, i + searchRange / 2);

                for (int j = startIdx; j <= endIdx; j++) {
                    if (j == i || habitability[j] < 0) continue;
                    if (habitability[j] > bestTargetScore) {
                        bestTargetScore = habitability[j];
                        bestTargetIdx = j;
                    }
                }

                if (bestTargetIdx != -1) {
                    H3Cell target = cells.get(bestTargetIdx);

                    source.setPopulation(Math.max(0, source.getPopulation() - migrants));
                    target.setPopulation(target.getPopulation() + migrants);

                    double tech = source.getTechnologyLevel() > 0 ? source.getTechnologyLevel() : defaultTech;
                    source.updateAgePyramidFromTotal(tech);
                    target.updateAgePyramidFromTotal(tech);

                    totalMigrated += migrants;
                }
            }
        }

        return totalMigrated;
    }

    public static double calculateCarryingCapacity(H3Cell cell, double techLevel) {
        if (cell == null) return 100.0;

        double soilFactor = cell.getSoilOrganicCarbon() > 0 ? (cell.getSoilOrganicCarbon() / 50.0) : 1.0;
        double rainFactor = Math.min(2.0, Math.max(0.1, cell.getRainfall() / 500.0));
        double waterFactor = cell.getWaterResource() > 0 ? 1.2 : 0.8;

        double baseCap = 50.0 * soilFactor * rainFactor * waterFactor;
        double techMultiplier = 1.0 + (techLevel * 0.8) + (techLevel >= 5.0 ? Math.pow(techLevel - 4.0, 1.5) : 0.0);

        return Math.max(10.0, baseCap * techMultiplier);
    }
}

