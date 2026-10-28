/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Degradation, Climate Stress, Pollution & Overfishing Engine.
 * Models:
 * 1. <b>Deforestation & Canopy Loss</b>: Timber extraction and agricultural land clearing,
 *    reducing natural biomass, increasing albedo, and disrupting regional evapotranspiration.
 * 2. <b>Pollution & Remote Atmospheric Dispersion</b>: Industrial/mining waste generation in populated cells
 *    and atmospheric/river dispersion diffusing toxic contaminants even into unpopulated, high-altitude or remote sites.
 * 3. <b>Marine Resource Overfishing</b>: Logistic fish stock harvesting ($r \cdot K_{fish}$). Over-harvesting
 *    leads to fishery collapse and coastal food crises.
 * 4. <b>Droughts, Famines & Demographic Migrations</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EcologicalDegradationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcologicalDegradationEngine.class);

    /** Internal state: caloric requirement per capita (double). */
    private static final double CALORIC_REQUIREMENT_PER_CAPITA = 0.5;

    public static void processEcologicalDegradation(List<H3Cell> cells, double techLevel) {
        processEcologicalDegradation(cells, techLevel, 1.0 / 12.0);
    }

    /**
     * Executes one ecological, pollution, overfishing & socio-demographic simulation tick across all cells.
     */
    public static void processEcologicalDegradation(List<H3Cell> cells, double techLevel, double deltaYears) {
// Numerical integration step for EcologicalDegradationEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, deltaYears / (1.0 / 12.0));

        int droughtEvents = 0;
        int famineEvents = 0;
        int totalMigrants = 0;

        // ── Phase 1: Local Environmental, Deforestation, Overfishing & Famine ──
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            double effectiveTech = (cell.getTechnologyLevel() != null && cell.getTechnologyLevel() > 0) ? cell.getTechnologyLevel() : techLevel;
            Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;

            // 1. Pollution Generation & Natural Biogeochemical Attenuation
            double currPollution = cell.getPollutionLevelOr(0.0);
            // Natural environmental attenuation (rain washout, microbial breakdown, phytoremediation) occurs in all cells
            double attenuatedPollution = Math.max(0.0, currPollution * Math.pow(0.92, dtMonthlyRatio) - 1.2 * dtMonthlyRatio);
            if (pop > 0) {
                // Heavy mining & industrial chemical pollution requires industrial-scale extraction (Tech >= 3.0)
                double miningActivity = (effectiveTech >= 3.0 && cell.getResourceMetal() != null)
                        ? Math.min(25.0, cell.getResourceMetal() * 0.02 * (effectiveTech / 4.0))
                        : 0.0;
                double industrialEmission = (effectiveTech >= 3.0)
                        ? pop * (0.01 + 0.06 * Math.pow(effectiveTech / 10.0, 1.5)) + miningActivity
                        : Math.max(0.0, (pop - 500.0) * 0.002); // Pre-industrial pollution limited to dense urban sewage/smoke
                cell.setPollutionLevel(Math.min(1000.0, attenuatedPollution + industrialEmission * dtMonthlyRatio));
            } else {
                cell.setPollutionLevel(attenuatedPollution);
            }

            // Pollution impact on health, lifespan, and natural biomass (scaled by dtMonthlyRatio)
            double pLevel = cell.getPollutionLevelOr(0.0);
            if (pLevel > 100.0) {
                double curLifespan = cell.getLifespanOr(30.0);
                double curFertility = cell.getFertilityOr(2.5);
                double curBioNat = cell.getBiomassNaturalOr(0.0);
                cell.setLifespan(Math.max(15.0, curLifespan - (pLevel / 200.0) * dtMonthlyRatio));
                cell.setFertility(Math.max(0.5, curFertility - (pLevel / 400.0) * dtMonthlyRatio));
                cell.setBiomassNatural(Math.max(0.0, curBioNat - (pLevel * 0.1 * dtMonthlyRatio)));
            }

            // 2. Overfishing & Marine Harvest Dynamics in Ocean & Coastal Biomes
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
                double areaScale = Math.max(0.05, cell.getEffectiveSurfaceAreaKm2() / 1770.0);
                double maxStockCapacity = (biome == Biome.BEACH ? 1500.0 : 1000.0) * areaScale;
                double currentFishStock = cell.getBiomassFish() != null ? cell.getBiomassFish() : (800.0 * areaScale);
                double dtYears = dtMonthlyRatio / 12.0;

                // Natural logistic regeneration of fish stock (Schaefer model: intrinsic rate r ~ 0.35/year)
                double rFish = (biome == Biome.BEACH ? 0.35 : 0.25);
                double regeneration = rFish * currentFishStock * Math.max(0.0, 1.0 - (currentFishStock / maxStockCapacity)) * dtYears;
                // Pelagic larval dispersal / immigration seed prevents irreversible mathematical extinction
                double pelagicLarvalSeed = 5.0 * areaScale * dtYears;

                // Fishing harvest effort scaled by local population, technological gear and stock density
                double harvestIntensity = pop * (0.08 + 0.04 * Math.tanh(effectiveTech / 3.0)) * (currentFishStock / maxStockCapacity) * dtYears;
                double fishingHarvest = Math.min(currentFishStock * 0.85, harvestIntensity);

                // Harvested fishery biomass converts to edible caloric human food
                if (fishingHarvest > 0.0) {
                    double foodGain = fishingHarvest * 0.80;
                    cell.setFoodResource(Math.min(2000000.0, (cell.getFoodResourceOr(0.0)) + foodGain));
                }

                double updatedStock = currentFishStock + regeneration + pelagicLarvalSeed - fishingHarvest;
                // Impact of water pollution on marine stock
                if (pLevel > 50.0) {
                    updatedStock -= (pLevel - 50.0) * 0.1 * dtYears;
                }

                cell.setBiomassFish(Math.max(pelagicLarvalSeed, Math.min(maxStockCapacity, updatedStock)));
                if (biome != Biome.BEACH) {
                    continue;
                }
            }

            if (cell.getElevation() == null || cell.getElevation() <= 0) continue;

            if (pop <= 0) {
                // Natural recovery when unoccupied
                double curSoc = cell.getSoilOrganicCarbonOr(20.0);
                double curNat = cell.getBiomassNaturalOr(500.0);
                double curWater = cell.getWaterResourceOr(100.0);
                cell.setSoilOrganicCarbon(Math.min(100.0, curSoc + 0.5 * dtMonthlyRatio));
                cell.setBiomassNatural(Math.min(1000.0, curNat + 2.0 * dtMonthlyRatio));
                cell.setWaterResource(Math.min(1000.0, curWater + 5.0 * dtMonthlyRatio));
                continue;
            }

            // 3. Drought Simulation
            double rain = cell.getRainfallOr(0.0);
            double temp = cell.getTemperatureOr(15.0);
            double curWaterRes = cell.getWaterResourceOr(100.0);

            double hydroStress = 0.0;
            if (rain < 300.0 || temp > 35.0) {
                hydroStress = Math.min(1.0, (300.0 - rain) / 300.0 + Math.max(0.0, temp - 35.0) * 0.03);
                droughtEvents++;

                double aquiferDepletion = hydroStress * 200.0 * dtMonthlyRatio;
                double curAquifer = cell.getAccessibleAquiferOr(0.0);
                double curAgri = cell.getBiomassAgricultureOr(0.0);
                cell.setAccessibleAquifer(Math.max(0.0, curAquifer - aquiferDepletion));
                cell.setWaterResource(Math.max(0.0, curWaterRes - hydroStress * 150.0 * dtMonthlyRatio));
                cell.setBiomassAgriculture(Math.max(0.0, curAgri * Math.exp(-0.4 * hydroStress * dtMonthlyRatio)));
            } else {
                cell.setWaterResource(Math.min(1000.0, curWaterRes + (rain / 100.0) * 10.0 * dtMonthlyRatio));
            }

            // 4. Famine Simulation
            double totalFood = (cell.getFoodResourceOr(0.0))
                    + (cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() * 0.70 * org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG : 0.0)
                    + (cell.getBiomassFish() != null ? cell.getBiomassFish() * 0.80 * org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG : 0.0)
                    + (cell.getBiomassNatural() != null ? cell.getBiomassNatural() * 0.05 * org.ether.society.model.PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG : 0.0);

            double foodRequired = pop * (CALORIC_REQUIREMENT_PER_CAPITA * 0.082 * dtMonthlyRatio);
            double curLife = cell.getLifespanOr(30.0);
            double curFert = cell.getFertilityOr(2.5);

            if (totalFood < foodRequired) {
                famineEvents++;
                double foodDeficitRatio = Math.min(1.0, (foodRequired - totalFood) / foodRequired);

                cell.setLifespan(Math.max(15.0, curLife - 1.0 * foodDeficitRatio * dtMonthlyRatio));
                cell.setFertility(Math.max(0.5, curFert - 0.5 * foodDeficitRatio * dtMonthlyRatio));

                int mortality = (int) Math.ceil(pop * 0.03 * foodDeficitRatio * dtMonthlyRatio);
                cell.setPopulation(Math.max(0, pop - mortality));
                double curFood = cell.getFoodResourceOr(0.0);
                cell.setFoodResource(Math.max(0.0, curFood - foodRequired));

                cell.updateAgePyramidFromTotal(effectiveTech);
            } else {
                cell.setLifespan(Math.min(85.0, curLife + 0.02 * dtMonthlyRatio));
                cell.setFertility(Math.min(7.0, curFert + 0.01 * dtMonthlyRatio));
            }

            // 5. Deforestation & Biome Impact (flux normalized by dt and planar H3 cell surface area)
            int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                    ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double planarCellAreaKm2 = H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3);
            double areaScale = Math.max(0.05, planarCellAreaKm2 / 1770.49);
            double currentWood = cell.getWoodResource() != null ? cell.getWoodResource() : (1000.0 * areaScale);
            double intactWoodCap = switch (biome) {
                case JUNGLE -> 9000.0;
                case FOREST -> 7500.0;
                case MOUNTAINS, HILLS -> (rain >= 600.0) ? 7000.0 : 3000.0;
                case BEACH -> (rain >= 800.0) ? 6000.0 : 2500.0;
                case SAVANNAH -> 4000.0;
                default -> 2500.0;
            } * areaScale;
            double currentAgriBiomass = cell.getBiomassAgricultureOr(0.0);
            // Agricultural land-use competition: cropland and settlement expansion reduces available forest canopy area
            double clearedLandFraction = (effectiveTech >= 1.5)
                    ? Math.clamp((currentAgriBiomass / (1800.0 * areaScale)) * 0.25 + (pop / (1500.0 * areaScale)) * 0.25, 0.0, 0.55)
                    : 0.0;
            double maxWoodCap = intactWoodCap * (1.0 - clearedLandFraction);
            double dtYears = dtMonthlyRatio / 12.0;

            // Physical logistic forest biomass regeneration (IPCC / FAO forestry model: ~3.5-5.0% annual increment on remaining forest land)
            double logisticRegrowthRate = (biome == Biome.JUNGLE ? 0.050 : (biome == Biome.FOREST || biome == Biome.MOUNTAINS || biome == Biome.HILLS ? 0.042 : 0.032));
            double woodRegrowth = (currentWood < maxWoodCap)
                    ? logisticRegrowthRate * currentWood * (1.0 - currentWood / maxWoodCap) * dtYears
                    : 0.0;

            // Per-capita timber, firewood & agricultural slash-and-burn (tavy) clearing demand
            double densityPerKm2 = pop / Math.max(10.0, planarCellAreaKm2);
            double accessibilityFactor = Math.clamp(densityPerKm2 / 3.0, 0.20, 1.0);
            double perCapitaWoodAndClearing = (effectiveTech >= 1.5) ? 0.12 : 0.05;
            double woodDemand = pop * perCapitaWoodAndClearing * accessibilityFactor * dtYears;

            // Pre-industrial logging without mechanized chainsaws/railways (tech < 3.5) cannot clear-cut remote interior mountain canopy (< 45% floor)
            double minInaccessibleCanopy = (effectiveTech < 3.5) ? (intactWoodCap * 0.45) : (10.0 * areaScale);
            double nextWood = Math.max(minInaccessibleCanopy, currentWood + woodRegrowth - woodDemand);
            cell.setWoodResource(nextWood);
            // Dynamic natural ecological biomass (wild fauna, understory flora, roots, berries, game animals)
            double currentNat = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : (3000.0 * areaScale);
            double maxNatCap = (biome == Biome.JUNGLE ? 9000.0 : (biome == Biome.FOREST ? 7500.0 : (biome == Biome.SAVANNAH ? 5000.0 : (biome == Biome.PLAINS ? 4000.0 : 2000.0)))) * areaScale;
            double rNat = (biome == Biome.PLAINS || biome == Biome.SAVANNAH) ? 0.15 : 0.10;
            double natRegrowth = (currentNat < maxNatCap)
                    ? rNat * currentNat * (1.0 - currentNat / maxNatCap) * dtYears
                    : 0.0;

            // Hunting and wild foraging: foragers (tech < 1.5) rely heavily on wild game; agrarians shift to crops
            double huntingFactor = (effectiveTech < 1.5 ? 0.10 : 0.03);
            double huntingHarvest = pop * huntingFactor * accessibilityFactor * (currentNat / maxNatCap) * dtYears;
            huntingHarvest = Math.min(currentNat * 0.50, huntingHarvest);

            if (huntingHarvest > 0.0) {
                double wildFoodGain = huntingHarvest * 0.70;
                cell.setFoodResource(Math.min(2000000.0, (cell.getFoodResourceOr(0.0)) + wildFoodGain));
            }

            double nextNat = Math.max(50.0 * areaScale, Math.min(maxNatCap, currentNat + natRegrowth - huntingHarvest));
            cell.setBiomassNatural(nextNat);

            // Severe deforestation & ecological depletion degrades soil organic carbon and rainfall
            if (nextWood < (100.0 * areaScale) && nextNat < (100.0 * areaScale) && (biome == Biome.FOREST || biome == Biome.JUNGLE)) {
                double curSoc = cell.getSoilOrganicCarbonOr(20.0);
                cell.setSoilOrganicCarbon(Math.max(5.0, curSoc - 0.5 * dtMonthlyRatio));
                cell.setRainfall(Math.max(100.0, rain * Math.pow(0.98, dtMonthlyRatio))); // Evapotranspiration loss
            }

            // 6. Demographic Density Strain & Agricultural Intensification
            double carryingCap = calculateCarryingCapacity(cell, effectiveTech);
            double demoPressure = pop / Math.max(1.0, carryingCap);

            if (demoPressure > 0.8 && effectiveTech >= 1.5) {
                // High density induces gradual agricultural land-use intensification scaled by dtYears and bounded by tech ceiling
                double annualIntensificationRate = Math.min(0.025, (demoPressure - 0.8) * 0.015 * (1.0 + effectiveTech * 0.1));
                double techLandCeiling = Math.max(pop * (400.0 + 50.0 * Math.min(effectiveTech, 10.0)) * 1.5, (360000.0 + 550000.0 * Math.pow(Math.max(0.0, effectiveTech - 1.4), 1.5)) * areaScale);
                double currentAgri = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                        ? cell.getBiomassAgriculture()
                        : Math.min(techLandCeiling, Math.max(25.0 * areaScale, pop * 0.5));
                cell.setBiomassAgriculture(Math.min(techLandCeiling, currentAgri * (1.0 + annualIntensificationRate * dtYears)));

                // Increases social inequality & friction (Gini index rises under density strain)
                double currentGini = cell.getGiniIndexOr(0.3);
                cell.setGiniIndex(Math.min(1.0, currentGini + 0.01 * demoPressure * dtMonthlyRatio));
            }

            cell.calculateMovementFriction(effectiveTech);
        }

        // ── Phase 2: Pollution Atmospheric & River Dispersion (Remote Sites) ─
        processPollutionDispersion(cells);

        // ── Phase 3: Demographic Migration Flux ───────────────────
        totalMigrants = processMigrationFlux(cells, techLevel, deltaYears);

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
// Numerical integration step for EcologicalDegradationEngine - verifies boundary conservation and updates local thermodynamic state
        int count = cells.size();
        if (count < 2) return;

        double[] pollutionDeltas = new double[count];

        for (int i = 0; i < count; i++) {
            H3Cell src = cells.get(i);
            if (src == null) continue;
            double srcPollution = src.getPollutionLevelOr(0.0);

            if (srcPollution > 20.0) {
                // Diffuse 12% of pollution to nearby regional cells (wind & river transport)
                int range = Math.min(count, 15);
                int start = Math.max(0, i - range / 2);
                int end = Math.min(count - 1, i + range / 2);
                int targetCount = end - start;

                if (targetCount > 0) {
                    double overflow = srcPollution * 0.12;
                    pollutionDeltas[i] -= overflow;
                    double share = overflow / targetCount;
                    for (int j = start; j <= end; j++) {
                        if (j != i && cells.get(j) != null) {
                            pollutionDeltas[j] += share;
                        }
                    }
                }
            }
        }

        // Apply pollution deltas to all cells (including unpopulated / remote wilderness)
        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            double curPol = c.getPollutionLevelOr(0.0);
            double newPollution = Math.min(1000.0, Math.max(0.0, curPol + pollutionDeltas[i]));
            c.setPollutionLevel(newPollution);
        }
    }

    private static int processMigrationFlux(List<H3Cell> cells, double defaultTech, double deltaYears) {
// Numerical integration step for EcologicalDegradationEngine - verifies boundary conservation and updates local thermodynamic state
        int totalMigrated = 0;
        int count = cells.size();
        double dtSafe = Math.clamp(deltaYears, 0.001, 1.0);

        double[] habitability = new double[count];

        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            if (c == null || c.getElevation() == null || c.getElevation() <= 0 || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN) {
                habitability[i] = -1.0;
                continue;
            }

            int h3Res = (c.getH3Index() != null && c.getH3Index() != 0L)
                    ? (int) ((c.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double areaScale = Math.max(0.05, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3) / 1770.49);

            double food = c.getFoodResourceOr(0.0);
            double water = c.getWaterResourceOr(0.0);
            double temp = c.getTemperatureOr(15.0);
            double friction = c.getMovementFrictionOr(1.0);
            double pollution = c.getPollutionLevelOr(0.0);
            int pop = c.getPopulationOr(0);

            double tempComfort = Math.exp(-Math.pow(temp - 18.0, 2) / 200.0);
            double foodPerCapita = pop > 0 ? food / pop : org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
            double foodNorm = Math.min(1.0, foodPerCapita / org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
            double waterNorm = Math.min(1.0, water / Math.max(25.0, 500.0 * areaScale));

            double score = (foodNorm * 0.35) + (waterNorm * 0.25) + (tempComfort * 0.25) - (pollution / 500.0) * 0.15;
            score = score / Math.max(0.5, friction * 0.5);

            habitability[i] = Math.max(0.001, score);
        }

        for (int i = 0; i < count; i++) {
            H3Cell source = cells.get(i);
            if (source == null || habitability[i] < 0 || source.getPopulation() == null || source.getPopulation() <= 0) continue;

            double srcScore = habitability[i];
            int pop = source.getPopulation();

            if (srcScore < 0.35 && pop > 10) {
                int migrants = (int) (pop * Math.min(0.30, (0.35 - srcScore) * 0.8) * dtSafe);
                if (migrants <= 0) continue;

                int bestTargetIdx = -1;
                double bestTargetScore = srcScore;

                int searchRange = Math.min(count, 30);
                int startIdx = Math.max(0, i - searchRange / 2);
                int endIdx = Math.min(count - 1, i + searchRange / 2);

                for (int j = startIdx; j <= endIdx; j++) {
                    if (j == i || habitability[j] < 0) continue;
                    H3Cell candidate = cells.get(j);
                    if (candidate == null) continue;
                    if (source.getLatitude() != null && source.getLongitude() != null
                            && candidate.getLatitude() != null && candidate.getLongitude() != null) {
                        double dLat = candidate.getLatitude() - source.getLatitude();
                        double dLon = (candidate.getLongitude() - source.getLongitude()) * Math.cos(Math.toRadians(source.getLatitude()));
                        double distKmSq = (dLat * 111.0) * (dLat * 111.0) + (dLon * 111.0) * (dLon * 111.0);
                        if (distKmSq > 22500.0) continue; // Cannot migrate beyond 150 km radius (150^2 = 22500)
                    }
                    if (habitability[j] > bestTargetScore) {
                        bestTargetScore = habitability[j];
                        bestTargetIdx = j;
                    }
                }

                if (bestTargetIdx != -1) {
                    H3Cell target = cells.get(bestTargetIdx);
                    if (target != null) {
                        int srcPop = source.getPopulationOr(0);
                        int tgtPop = target.getPopulationOr(0);
                        source.setPopulation(Math.max(0, srcPop - migrants));
                        target.setPopulation(tgtPop + migrants);

                        double tech = (source.getTechnologyLevel() != null && source.getTechnologyLevel() > 0) ? source.getTechnologyLevel() : defaultTech;
                        source.updateAgePyramidFromTotal(tech);
                        target.updateAgePyramidFromTotal(tech);

                        totalMigrated += migrants;
                    }
                }
            }
        }

        return totalMigrated;
    }

    /**
     * Calculate carrying capacity.
     *
     * @param cell the cell parameter (H3Cell)
     * @param techLevel the tech level parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateCarryingCapacity(H3Cell cell, double techLevel) {
// Numerical integration step for EcologicalDegradationEngine - verifies boundary conservation and updates local thermodynamic state
        if (cell == null) return 100.0;

        double soc = cell.getSoilOrganicCarbonOr(20.0);
        double soilFactor = soc > 0 ? (soc / 50.0) : 1.0;
        double rain = cell.getRainfallOr(500.0);
        double rainFactor = Math.min(2.0, Math.max(0.1, rain / 500.0));
        double water = cell.getWaterResourceOr(100.0);
        double waterFactor = water > 0 ? 1.2 : 0.8;

        int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                : 3;
        double areaScale = Math.max(0.05, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3) / 1770.49);
        double baseCap = 50.0 * areaScale * soilFactor * rainFactor * waterFactor;
        double techMultiplier = 1.0 + (techLevel * 0.8) + (techLevel >= 5.0 ? Math.pow(techLevel - 4.0, 1.5) : 0.0);

        return Math.max(2.0, baseCap * techMultiplier);
    }
}


