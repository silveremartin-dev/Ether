/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Degradation, Climate Stress, Pollution &amp; Overfishing Engine (Tier 1 Core Physics).
 * <p>
 * Strictly governed by smooth ($C^\infty$) conservation and regeneration equations of physical
 * observables ($N, K, B_{\text{nat}}, B_{\text{agr}}, B_{\text{fish}}, W_{\text{wood}}$, soil carbon, water)
 * with zero {@code technologyLevel} parameters and zero Tier 2 static coupling ({@code AGENTS.md} §7.2, §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EcologicalDegradationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcologicalDegradationEngine.class);

    /** Standard human annual metabolic energy consumption ($3.362\,\text{GJ/capita/year}$). */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_GJ = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;

    /**
     * Executes one monthly ecological degradation tick across all cells.
     */
    public static void processEcologicalDegradation(List<H3Cell> cells) {
        processEcologicalDegradation(cells, 1.0 / 12.0);
    }

    public static void processEcologicalDegradation(List<H3Cell> cells, double legacyTech, double deltaYears) {
        processEcologicalDegradation(cells, deltaYears);
    }

    public static void processEcologicalDegradation(List<H3Cell> cells, Double legacyTech, double deltaYears) {
        processEcologicalDegradation(cells, deltaYears);
    }

    /**
     * Executes one ecological, pollution, overfishing &amp; socio-demographic simulation tick across all cells
     * over {@code deltaYears}, driven purely by conserved physical observables.
     *
     * @param cells      active H3 cells
     * @param deltaYears time step in Julian years
     */
    public static void processEcologicalDegradation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtSafeYears = Math.max(1e-4, deltaYears);
        double dtMonthlyRatio = Math.max(0.001, dtSafeYears / (1.0 / 12.0));

        int droughtEvents = 0;
        int famineEvents = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;

            // 1. Pollution Generation & Natural Biogeochemical Attenuation
            double currPollution = cell.getPollutionLevelOr(0.0);
            double attenuatedPollution = Math.max(0.0, currPollution * Math.pow(0.92, dtMonthlyRatio) - 1.2 * dtMonthlyRatio);
            if (pop > 0) {
                double capPerCapita = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
                double areaKm2 = cell.getEffectiveSurfaceAreaKm2() > 0 ? cell.getEffectiveSurfaceAreaKm2() : 1770.49;
                double popDensity = pop / areaKm2;
                double industrialGate = SmoothMath.sigmoid(capPerCapita, 1500.0, 350.0);
                double fossilCombustion = (cell.getResourceCoalOr(0.0) * 0.05
                        + cell.getResourceOilOr(0.0) * 0.08
                        + cell.getResourceGasOr(0.0) * 0.04) * industrialGate;
                double miningGate = SmoothMath.sigmoid(capPerCapita, 1100.0, 250.0);
                double rawMining = cell.getResourceMetalOr(0.0) * 0.02 * SmoothMath.softmin(3.0, capPerCapita / 500.0, 4.0);
                double miningActivity = miningGate * SmoothMath.softmin(25.0, rawMining, 0.5);
                double mechanizationFactor = 0.08 * industrialGate;
                double industrialEmission = popDensity * mechanizationFactor + miningActivity + SmoothMath.softmin(50.0, fossilCombustion, 0.2);
                cell.setPollutionLevel(Math.min(1000.0, attenuatedPollution + industrialEmission * dtMonthlyRatio));
            } else {
                cell.setPollutionLevel(attenuatedPollution);
            }

            double pLevel = cell.getPollutionLevelOr(0.0);
            if (pLevel > 0.0) {
                double pollutionToxicity = pLevel / (150.0 + pLevel);
                cell.setLifespan(Math.max(15.0, cell.getLifespanOr(30.0) - 1.2 * pollutionToxicity * dtMonthlyRatio));
                cell.setFertility(Math.max(0.5, cell.getFertilityOr(2.5) - 0.5 * pollutionToxicity * dtMonthlyRatio));
                cell.setBiomassNatural(Math.max(0.0, cell.getBiomassNaturalOr(0.0) - 5.0 * pollutionToxicity * dtMonthlyRatio));
            }

            int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                    ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double planarCellAreaKm2 = H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3);
            double areaScale = Math.max(0.05, planarCellAreaKm2 / 1770.49);

            // 2. Schaefer Logistic Fishery Dynamics in Marine & Coastal Cells
            double elev = cell.getElevationOr(0.0);
            boolean isMarineOrCoastal = (elev <= 0.0 || Boolean.TRUE.equals(cell.getIsCoastal())
                    || cell.getCoastalMarineResourceOr(0.0) > 0.0 || cell.getBiomassFishOr(0.0) > 0.0
                    || biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH);
            if (isMarineOrCoastal) {
                double coastalShelfFactor = SmoothMath.sigmoid(elev, -200.0, 80.0);
                double maxStockCapacity = (1000.0 + 500.0 * coastalShelfFactor) * areaScale;
                double currentFishStock = cell.getBiomassFish() != null ? cell.getBiomassFish() : (800.0 * areaScale);

                double rFish = 0.25 + 0.10 * coastalShelfFactor;
                double regeneration = rFish * currentFishStock * Math.max(0.0, 1.0 - (currentFishStock / maxStockCapacity)) * dtSafeYears;
                double pelagicLarvalSeed = 5.0 * areaScale * dtSafeYears;

                // Harvest intensity scaling across technological/industrial eras (Pauly 1998, Worm et al. 2006):
                // 1. Traditional/artisanal subsistence (k_pc < 60 kg/cap): ~0.06 t/cap/yr
                // 2. Steam trawling & ice preservation (k_pc ~ 600-1200 kg/cap): +0.35 t/cap/yr
                // 3. Modern diesel factory trawlers, sonar & pelagic nets (k_pc > 2500 kg/cap): +1.25 t/cap/yr
                // In modern eras, harvest capacity vastly exceeds natural MSY (r*K/4), driving severe stock depletion.
                double capPerCap = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
                double artisanalCatch = 0.06 + 0.04 * Math.tanh(capPerCap / 60.0);
                double steamTrawlerCatch = 0.35 * SmoothMath.sigmoid(capPerCap, 750.0, 200.0);
                double factoryFleetCatch = 1.25 * SmoothMath.sigmoid(capPerCap, 2800.0, 700.0);
                double perCapitaMarineCatch = artisanalCatch + steamTrawlerCatch + factoryFleetCatch;

                // Schaefer harvest kinetics: H = E * (B / B_max)^gamma
                double stockAbundanceRatio = Math.clamp(currentFishStock / maxStockCapacity, 0.0, 1.0);
                double catchPerEffort = Math.pow(stockAbundanceRatio, 0.75);
                double harvestIntensity = pop * perCapitaMarineCatch * catchPerEffort * dtSafeYears;

                // Modern overfishing can extract up to 90% of stock in high-capital zones
                double maxHarvestFraction = 0.50 + 0.40 * SmoothMath.sigmoid(capPerCap, 1200.0, 400.0);
                double fishingHarvest = Math.min(currentFishStock * maxHarvestFraction, harvestIntensity);

                if (fishingHarvest > 0.0) {
                    cell.setFoodResource(Math.min(2000000.0, cell.getFoodResourceOr(0.0) + fishingHarvest * 0.80));
                }

                double ecotoxicity = (pLevel / (100.0 + pLevel)) * 8.0 * dtSafeYears;
                double updatedStock = Math.max(0.0, currentFishStock + regeneration + pelagicLarvalSeed - fishingHarvest - ecotoxicity);
                cell.setBiomassFish(Math.clamp(updatedStock, pelagicLarvalSeed, maxStockCapacity));

                if (elev <= 0.0 && !Boolean.TRUE.equals(cell.getIsPolder())) {
                    continue;
                }
            }

            if (elev <= 0.0) continue;

            if (pop <= 0) {
                cell.setSoilOrganicCarbon(Math.min(100.0, cell.getSoilOrganicCarbonOr(20.0) + 0.5 * dtMonthlyRatio));
                cell.setBiomassNatural(Math.min(1000.0, cell.getBiomassNaturalOr(500.0) + 2.0 * dtMonthlyRatio));
                cell.setWaterResource(Math.min(1000.0, cell.getWaterResourceOr(100.0) + 5.0 * dtMonthlyRatio));
                continue;
            }

            // 3. Smooth C^infty Hydrological Drought Stress
            double rain = cell.getRainfallOr(0.0);
            double temp = cell.getTemperatureOr(15.0);
            double curWaterRes = cell.getWaterResourceOr(100.0);

            double totalAvailableWater = rain + SmoothMath.softmin(800.0, curWaterRes * 0.50, 0.02);
            double aridityDeficit = SmoothMath.softplus(300.0 - totalAvailableWater, 0.05) / 300.0;
            double heatStress = SmoothMath.softplus(temp - 45.0, 0.5) * 0.03;
            double hydroStress = SmoothMath.softmin(1.0, aridityDeficit + heatStress, 8.0);

            if (hydroStress > 0.05) {
                droughtEvents++;
            }
            double droughtGate = SmoothMath.sigmoid(hydroStress, 0.08, 0.03);
            double rechargeRate = (1.0 - droughtGate) * (rain / 100.0) * 10.0 * dtMonthlyRatio;
            double depletionRate = droughtGate * hydroStress * 150.0 * dtMonthlyRatio;
            cell.setAccessibleAquifer(Math.max(0.0, cell.getAccessibleAquiferOr(0.0) - droughtGate * hydroStress * 200.0 * dtMonthlyRatio));
            cell.setWaterResource(Math.clamp(curWaterRes + rechargeRate - depletionRate, 0.0, 1000.0));
            cell.setBiomassAgriculture(Math.max(0.0, cell.getBiomassAgricultureOr(0.0) * Math.exp(-0.4 * droughtGate * hydroStress * dtMonthlyRatio)));

            // 4. Smooth Nutritional Stress Telemetry (Incorporates all biophysical food fluxes: crops, wild flora, fauna/livestock, marine fish & shellfish)
            double agBio = cell.getBiomassAgricultureOr(0.0);
            double natBio = cell.getBiomassNaturalOr(0.0);
            double liveBio = cell.getBiomassLivestockOr(0.0);
            double fishBio = cell.getBiomassFishOr(0.0);
            double coastalBio = cell.getCoastalMarineResourceOr(0.0);

            double agShare = agBio / (agBio + natBio * 0.01 + 1.0);
            double wildForageFrac = 0.008 - 0.003 * SmoothMath.sigmoid(agShare, 0.20, 0.08);
            double animalHarvestFrac = 0.15; // Sustainable annual meat harvest from wild game / pastoral livestock

            double edibleAgriGJ = agBio * 0.70 * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG * 1e-3);
            double edibleMarineGJ = (fishBio + coastalBio * 0.20) * 0.80 * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG * 1e-3);
            double edibleFaunaGJ = liveBio * animalHarvestFrac * (PhysicalConstants.BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG * 1e-3);
            double edibleFloraGJ = natBio * wildForageFrac * PhysicalConstants.BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG;

            double totalFood = cell.getFoodResourceOr(0.0) + edibleAgriGJ + edibleMarineGJ + edibleFaunaGJ + edibleFloraGJ;

            double foodRequired = pop * HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtSafeYears;
            double curLife = cell.getLifespanOr(30.0);
            double curFert = cell.getFertilityOr(2.5);

            double deficitRatio = SmoothMath.softmin(1.0, SmoothMath.softplus(foodRequired - totalFood, 0.5) / Math.max(1e-4, foodRequired), 8.0);
            if (deficitRatio > 0.05) famineEvents++;
            double surplusGate = SmoothMath.sigmoid(totalFood - foodRequired, 0.0, Math.max(0.1, 0.1 * foodRequired));
            cell.setLifespan(Math.clamp(curLife + (0.02 * surplusGate - 0.5 * deficitRatio) * dtMonthlyRatio, 15.0, 85.0));
            cell.setFertility(Math.clamp(curFert + (0.01 * surplusGate - 0.25 * deficitRatio) * dtMonthlyRatio, 0.5, 7.0));

            // 5. Continuous Whittaker & Likens Woody Biomass & Deforestation (Zero Tier-2 Sakoku static coupling!)
            double currentWood = cell.getWoodResource() != null ? cell.getWoodResource() : (1000.0 * areaScale);
            double forestAffinity = SmoothMath.sigmoid(rain, 450.0, 166.67) * SmoothMath.sigmoid(temp, 2.0, 4.0);
            double woodResidenceYears = 35.0 * forestAffinity + 5.0 * (1.0 - forestAffinity) * (rain / (rain + 250.0));
            double nppP = 3000.0 * (1.0 - Math.exp(-0.000664 * Math.max(0.0, rain)));
            double nppT = 3000.0 / (1.0 + Math.exp(1.315 - 0.119 * temp));
            double cellNpp = SmoothMath.softmin(nppP, nppT, 0.02);
            double intactWoodCap = Math.clamp(cellNpp * 0.60 * woodResidenceYears * 0.25 * areaScale, 100.0 * areaScale, 9500.0 * areaScale);

            double agriClearanceGate = SmoothMath.sigmoid(agBio, 10.0, 4.0);
            double clearedLandFraction = agriClearanceGate * SmoothMath.softmin(0.55,
                    (agBio / (1800.0 * areaScale)) * 0.25 + (pop / (1500.0 * areaScale)) * 0.25, 10.0);
            double maxWoodCap = Math.max(currentWood, intactWoodCap * (1.0 - clearedLandFraction));

            double posTemp = SmoothMath.softplus(temp, 0.5);
            double logisticRegrowthRate = 0.030 + 0.020 * forestAffinity * (posTemp / (posTemp + 12.0));
            double woodRegrowth = logisticRegrowthRate * currentWood * Math.max(0.0, 1.0 - currentWood / maxWoodCap) * dtSafeYears;

            double densityPerKm2 = pop / Math.max(10.0, planarCellAreaKm2);
            double accessibilityFactor = SmoothMath.smoothClamp(densityPerKm2 / 3.0, 0.20, 1.0, 8.0);
            double perCapitaWoodAndClearing = 0.05 + 0.07 * agriClearanceGate;
            // Endogenous physical timber scarcity elasticity (von Thünen / Hotelling): scarce mountain timber raises extraction effort
            double remainingWoodRatio = Math.clamp(currentWood / Math.max(1.0, intactWoodCap), 0.0, 1.0);
            double scarcityConservation = 0.25 + 0.75 * SmoothMath.sigmoid(remainingWoodRatio, 0.35, 0.10);
            double woodDemand = pop * perCapitaWoodAndClearing * accessibilityFactor * scarcityConservation * dtSafeYears;

            // Smooth mechanized logging accessibility (chainsaws/railways require high physical capital per capita K/N)
            double capPerCap = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
            double mechanizedLogging = SmoothMath.sigmoid(capPerCap, 250.0, 75.0);
            double minInaccessibleCanopy = (1.0 - mechanizedLogging) * (intactWoodCap * 0.45) + mechanizedLogging * (10.0 * areaScale);
            double nextWood = Math.max(minInaccessibleCanopy, currentWood + woodRegrowth - woodDemand);
            cell.setWoodResource(nextWood);

            // Dynamic wild fauna & understory flora
            double currentNat = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : (3000.0 * areaScale);
            double baseNatCap = Math.clamp(cellNpp * 3.5 * areaScale, 500.0 * areaScale, 9000.0 * areaScale);
            double maxNatCap = Math.max(baseNatCap, pop * 48.0 * (1.0 - agriClearanceGate));
            double rNat = 0.08 + 0.07 * (1.0 - forestAffinity);
            double natRegrowth = rNat * currentNat * Math.max(0.0, 1.0 - currentNat / maxNatCap) * dtSafeYears;

            double huntingFactor = 0.10 - 0.07 * agriClearanceGate;
            double huntingHarvest = Math.min(currentNat * 0.50,
                    pop * huntingFactor * accessibilityFactor * (currentNat / maxNatCap) * dtSafeYears);
            if (huntingHarvest > 0.0) {
                cell.setFoodResource(Math.min(2000000.0, cell.getFoodResourceOr(0.0) + huntingHarvest * 0.70));
            }
            double nextNat = Math.clamp(currentNat + natRegrowth - huntingHarvest, 50.0 * areaScale, maxNatCap);
            cell.setBiomassNatural(nextNat);

            // Smooth canopy loss evapotranspiration & soil carbon feedback
            double severeDeforestation = (1.0 - SmoothMath.sigmoid(nextWood / areaScale, 100.0, 30.0))
                    * (1.0 - SmoothMath.sigmoid(nextNat / areaScale, 100.0, 30.0))
                    * SmoothMath.sigmoid(forestAffinity, 0.40, 0.10);
            if (severeDeforestation > 0.01) {
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbonOr(20.0) - 0.5 * severeDeforestation * dtMonthlyRatio));
                cell.setRainfall(Math.max(100.0, rain * Math.pow(1.0 - 0.02 * severeDeforestation, dtMonthlyRatio)));
            }

            // 6. Smooth Demographic Density Strain & Boserupian Land Intensification
            double carryingCap = calculateCarryingCapacity(cell);
            double demoPressure = pop / Math.max(1.0, carryingCap);
            double pressureExcess = SmoothMath.softplus(demoPressure - 0.80, 10.0);
            if (pressureExcess > 0.001 && agBio > 0.0) {
                double annualIntensificationRate = SmoothMath.softmin(0.025, pressureExcess * 0.02, 100.0);
                double landCeiling = Math.max(pop * 900.0,
                        (360000.0 + 550000.0 * SmoothMath.softmin(5.0, 1.0 + capPerCap / 100.0, 2.0)) * areaScale);
                cell.setBiomassAgriculture(Math.min(landCeiling, agBio * (1.0 + annualIntensificationRate * dtSafeYears)));
                cell.setGiniIndex(Math.min(1.0, cell.getGiniIndexOr(0.3) + 0.01 * demoPressure * SmoothMath.sigmoid(demoPressure, 0.8, 0.1) * dtMonthlyRatio));
            }

            cell.calculateMovementFriction();
        }

        processPollutionDispersion(cells);
        int totalMigrants = processMigrationFlux(cells, dtSafeYears);

        if (droughtEvents > 0 || famineEvents > 0 || totalMigrants > 0) {
            logger.debug("Simulation Tick: {} drought cells, {} famine cells, {} climate migrants relocated.",
                    droughtEvents, famineEvents, totalMigrants);
        }
    }

    private static void processPollutionDispersion(List<H3Cell> cells) {
        int count = cells.size();
        if (count < 2) return;
        double[] pollutionDeltas = new double[count];

        for (int i = 0; i < count; i++) {
            H3Cell src = cells.get(i);
            if (src == null) continue;
            double srcPollution = src.getPollutionLevelOr(0.0);
            double excess = SmoothMath.softplus(srcPollution - 20.0, 0.5);
            if (excess > 0.01) {
                double emittedFraction = excess * 0.05;
                pollutionDeltas[i] -= emittedFraction;
                int step = Math.max(1, count / 8);
                int targets = 0;
                for (int j = 1; j <= 4; j++) {
                    int neighborIdx = (i + j * step) % count;
                    if (neighborIdx != i) {
                        pollutionDeltas[neighborIdx] += emittedFraction / 4.0;
                        targets++;
                    }
                }
                if (targets == 0) {
                    pollutionDeltas[i] += emittedFraction;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            H3Cell c = cells.get(i);
            if (c != null && pollutionDeltas[i] != 0.0) {
                c.setPollutionLevel(Math.max(0.0, c.getPollutionLevelOr(0.0) + pollutionDeltas[i]));
            }
        }
    }

    private static int processMigrationFlux(List<H3Cell> cells, double dtYears) {
        int totalMigrated = 0;
        int count = cells.size();
        if (count < 2) return 0;

        for (int i = 0; i < count; i++) {
            H3Cell source = cells.get(i);
            if (source == null) continue;
            int srcPop = source.getPopulationOr(0);
            if (srcPop < 10) continue;

            double carryingCap = calculateCarryingCapacity(source);
            double excessPop = SmoothMath.softplus(srcPop - carryingCap * 1.15, 0.2);
            double curFood = source.getFoodResourceOr(0.0);
            double foodRequired = srcPop * HUMAN_ANNUAL_METABOLIC_ENERGY_GJ * dtYears;
            double starvationUrgency = SmoothMath.sigmoid(foodRequired * 0.50 - curFood, 0.0, Math.max(0.1, foodRequired * 0.1));

            if (excessPop > 0.5 || starvationUrgency > 0.2) {
                double migrationRate = 0.02 + 0.03 * starvationUrgency;
                double distressedPool = Math.max(excessPop, srcPop * 0.10 * starvationUrgency);
                int migrants = (int) Math.min(srcPop * 0.15, Math.floor(distressedPool * migrationRate * (dtYears * 12.0)));

                if (migrants > 0) {
                    H3Cell bestTarget = null;
                    double bestScore = -Double.MAX_VALUE;
                    int step = Math.max(1, count / 16);
                    for (int k = 1; k <= 8; k++) {
                        int targetIdx = (i + k * step) % count;
                        if (targetIdx == i) continue;
                        H3Cell candidate = cells.get(targetIdx);
                        if (candidate == null) continue;
                        Biome candBiome = candidate.getBiome();
                        if (candBiome == Biome.OCEAN || candBiome == Biome.DEEP_OCEAN || candBiome == Biome.GLACIER) continue;
                        if (candidate.getElevationOr(0.0) <= 0.0) continue;

                        double lat1 = source.getLatitudeOr(0.0);
                        double lon1 = source.getLongitudeOr(0.0);
                        double lat2 = candidate.getLatitudeOr(0.0);
                        double lon2 = candidate.getLongitudeOr(0.0);
                        double dLat = (lat2 - lat1) * 111.0;
                        double dLon = (lon2 - lon1) * 111.0 * Math.cos(Math.toRadians((lat1 + lat2) / 2.0));
                        if (dLat * dLat + dLon * dLon > 22500.0) continue;

                        double candCap = calculateCarryingCapacity(candidate);
                        int candPop = candidate.getPopulationOr(0);
                        double spareCap = candCap - candPop;
                        double score = spareCap + candidate.getFoodResourceOr(0.0) * 0.1 - candidate.getPollutionLevelOr(0.0) * 2.0;
                        if (score > bestScore && spareCap > migrants) {
                            bestScore = score;
                            bestTarget = candidate;
                        }
                    }

                    if (bestTarget != null) {
                        org.ether.society.database.H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(source, Math.max(0, srcPop - migrants));
                        org.ether.society.database.H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(bestTarget, bestTarget.getPopulationOr(0) + migrants);
                        totalMigrated += migrants;
                    }
                }
            }
        }
        return totalMigrated;
    }

    /**
     * Computes local biophysical human carrying capacity strictly from soil organic carbon,
     * meteoric rainfall, freshwater availability, cultivated biomass, and physical capital per capita ($C^\infty$).
     *
     * @param cell H3 cell
     * @return biophysical carrying capacity (individuals)
     */
    public static double calculateCarryingCapacity(H3Cell cell) {
        if (cell == null) return 100.0;

        double soc = cell.getSoilOrganicCarbonOr(20.0);
        double soilFactor = SmoothMath.softmax(0.2, soc / 50.0, 5.0);
        double rain = cell.getRainfallOr(500.0);
        double rainFactor = SmoothMath.smoothClamp(rain / 500.0, 0.1, 2.0, 5.0);
        double water = cell.getWaterResourceOr(100.0);
        double waterFactor = 0.8 + 0.4 * (water / (25.0 + Math.max(0.0, water)));

        int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                : 3;
        double areaScale = Math.max(0.05, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3) / 1770.49);
        double baseCap = 50.0 * areaScale * soilFactor * rainFactor * waterFactor;

        int pop = Math.max(1, cell.getPopulationOr(1));
        double capPerCap = cell.getResourceCapitalOr(0.0) / pop;
        double agBiomass = cell.getBiomassAgricultureOr(0.0);
        double agGate = SmoothMath.sigmoid(agBiomass, 10.0, 4.0);
        double agMultiplier = 1.0 + agGate * (0.5 + 2.0 * (1.0 - Math.exp(-agBiomass / (500.0 * areaScale))));

        // Marine & coastal fishery carrying capacity boost (shellfish, fish, marine algae)
        double marineStock = cell.getBiomassFishOr(0.0) + cell.getCoastalMarineResourceOr(0.0);
        double marineMultiplier = 1.0 + 1.2 * Math.tanh(marineStock / (800.0 * areaScale));

        // Terrestrial fauna / pastoral livestock carrying capacity boost (hunting & pastoralism)
        double liveStock = cell.getBiomassLivestockOr(0.0);
        double pastoralMultiplier = 1.0 + 0.8 * Math.tanh(liveStock / (600.0 * areaScale));

        double capitalMultiplier = 1.0 + 3.0 * (1.0 - Math.exp(-capPerCap / 250.0));

        return Math.max(2.0, baseCap * agMultiplier * marineMultiplier * pastoralMultiplier * capitalMultiplier);
    }
}
