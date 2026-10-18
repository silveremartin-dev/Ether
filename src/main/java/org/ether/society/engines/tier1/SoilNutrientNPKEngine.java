/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Soil N-P-K Stoichiometry & Geological Mineral Mining Engine.
 * Models:
 * 1. <b>Liebig's Law of the Minimum</b>: Agricultural yield = min(N, P, K).
 * 2. <b>Rock Phosphate (P) & Potash (K) Geological Mining</b>: Mineral fertilizer extraction from crustal deposits.
 * 3. <b>Haber-Bosch Nitrogen (N) Synthesis</b>: High-pressure ammonia synthesis from atmospheric N2 requiring thermal power (P_capita >= 5000W).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SoilNutrientNPKEngine {
    private static final Logger logger = LoggerFactory.getLogger(SoilNutrientNPKEngine.class);

    /* Haber-Bosch power requirement in Watts per capita */
    public static final double HABER_BOSCH_POWER_WATTS = 5000.0;

    public static void processSoilNutrients(List<H3Cell> cells) {
        processSoilNutrients(cells, 1.0 / 12.0);
    }

    /*
     * Executes N-P-K stoichiometry, mineral mining extraction, and Haber-Bosch synthesis tick.
     */
    public static void processSoilNutrients(List<H3Cell> cells, double dtYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        int size = cells.size();
        H3Cell sample = cells.get(0);
        int h3Res = (sample.getH3Index() != null && sample.getH3Index() != 0L)
                ? (int) ((sample.getH3Index() >>> 52) & 0xFL)
                : 3;
        double planarCellAreaKm2 = H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3);
        double areaScale = Math.max(0.05, planarCellAreaKm2 / 1770.49);
        double minMinedOreScaled = 10.0 * areaScale;
        double baselineN = 5.0 * areaScale;
        double industrialBonusN = 45.0 * areaScale;
        double soilNBase = 20.0 * areaScale;
        double soilPBase = 15.0 * areaScale;
        double soilKBase = 15.0 * areaScale;
        double nNorm = 50.0 * areaScale;
        double pNorm = 25.0 * areaScale;
        double kNorm = 25.0 * areaScale;
        double safeDt = Math.max(0.001, dtYears);
        double dtFactor = safeDt * 0.1;
        double relaxationRate = 1.0 - Math.exp(-3.0 * safeDt);
        double terraceMaintenanceDecay = Math.min(1.0, 0.004 * safeDt);

        java.util.concurrent.atomic.AtomicInteger haberBoschCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.function.Consumer<H3Cell> cellProcessor = cell -> {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            double baseBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
            if (baseBiomass <= 0.0 && pop <= 0) return;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double metalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 100.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

            // 1. Rock Phosphate (P) & Potash (K) Geological Mining Extraction
            double extractedMiningP = Math.min(metalOre * 0.05, minMinedOreScaled);
            double extractedMiningK = Math.min(metalOre * 0.05, minMinedOreScaled);

            // Enforce conservation of mass: deduct mined fertilizer ore from the geological mineral stock
            double totalMinedOre = (extractedMiningP + extractedMiningK) * dtFactor;
            cell.setResourceMetal(Math.max(0.0, metalOre - totalMinedOre));

            // 2. Haber-Bosch Industrial Nitrogen Fixation
            double synthesizedN = baselineN;
            if (tech >= 3.5 && powerPerCapita >= HABER_BOSCH_POWER_WATTS) {
                haberBoschCounter.incrementAndGet();
                synthesizedN += industrialBonusN;
            }

            // 3. Liebig's Law of the Minimum Yield Bound
            double effectiveSoilN = soilNBase + synthesizedN;
            double effectiveSoilP = soilPBase + extractedMiningP;
            double effectiveSoilK = soilKBase + extractedMiningK;

            double liebigFactor = Math.min(effectiveSoilN / nNorm, Math.min(effectiveSoilP / pNorm, effectiveSoilK / kNorm));

            org.ether.society.model.Biome biome = cell.getBiome() != null ? cell.getBiome() : org.ether.society.model.Biome.PLAINS;
            double arableBiomeFactor = switch (biome) {
                case MOUNTAINS -> 0.35;
                case HILLS, BEACH -> 0.55;
                case TUNDRA, DESERT, SNOW -> 0.15;
                default -> 1.0;
            };
            double techLandCeiling = (360.0 + 550.0 * Math.pow(Math.max(0.0, tech - 1.4), 1.5)) * areaScale * arableBiomeFactor;
            double maxCellBiomass = Math.min(3500.0 * areaScale * arableBiomeFactor, techLandCeiling);
            double laborCultivatedBiomass = pop * (0.65 + 0.25 * Math.min(tech, 5.0));
            double potentialBiomass = (tech >= 1.5)
                    ? Math.min(maxCellBiomass, Math.max(25.0 * areaScale * arableBiomeFactor, laborCultivatedBiomass))
                    : 0.0;
            if (baseBiomass <= 0.0) {
                baseBiomass = potentialBiomass;
            }
            double targetYield = potentialBiomass * liebigFactor;

            double updatedBiomass;
            if (tech < 1.5) {
                updatedBiomass = 0.0;
            } else if (baseBiomass <= targetYield) {
                updatedBiomass = baseBiomass + relaxationRate * (targetYield - baseBiomass);
            } else {
                updatedBiomass = baseBiomass - terraceMaintenanceDecay * (baseBiomass - targetYield);
            }
            cell.setBiomassAgriculture(Math.clamp(updatedBiomass, 0.0, maxCellBiomass * 1.25));
        };

        if (size >= 10000) {
            cells.parallelStream().forEach(cellProcessor);
        } else {
            for (int i = 0; i < size; i++) {
                cellProcessor.accept(cells.get(i));
            }
        }

        int haberBoschActive = haberBoschCounter.get();
        if (haberBoschActive > 0) {
            logger.debug("NPK Engine: Haber-Bosch industrial nitrogen synthesis active across {} power grid cells.", haberBoschActive);
        }
    }
}


