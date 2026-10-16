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

        int haberBoschActive = 0;
        double relaxationRate = 1.0 - Math.exp(-3.0 * Math.max(0.001, dtYears));

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            double baseBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
            if (baseBiomass <= 0.0 && pop <= 0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double metalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 100.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                    ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double planarCellAreaKm2 = H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3);
            double areaScale = Math.max(0.05, planarCellAreaKm2 / 1770.49);

            // 1. Rock Phosphate (P) & Potash (K) Geological Mining Extraction
            double extractedMiningP = Math.min(metalOre * 0.05, 10.0 * areaScale);
            double extractedMiningK = Math.min(metalOre * 0.05, 10.0 * areaScale);

            // 2. Haber-Bosch Industrial Nitrogen Fixation (Requires industrial/modern era tech >= 3.5 and high power)
            double synthesizedN = 5.0 * areaScale; // Baseline biological leguminous fixation
            if (tech >= 3.5 && powerPerCapita >= HABER_BOSCH_POWER_WATTS) {
                haberBoschActive++;
                synthesizedN += 45.0 * areaScale; // Massive industrial ammonia yield
            }

            // 3. Liebig's Law of the Minimum Yield Bound
            double effectiveSoilN = (20.0 * areaScale) + synthesizedN;
            double effectiveSoilP = (15.0 * areaScale) + extractedMiningP;
            double effectiveSoilK = (15.0 * areaScale) + extractedMiningK;

            double liebigFactor = Math.min(effectiveSoilN / (50.0 * areaScale), Math.min(effectiveSoilP / (25.0 * areaScale), effectiveSoilK / (25.0 * areaScale)));

            // Potential agricultural biomass cultivated by population on this cell
            // Hunter-gatherers and nomadic foragers (tech < 1.5) do not cultivate crops.
            // Pre-industrial swidden/fallow and early wet-rice systems (tech 1.5-2.5) scale with labor and technology,
            // constrained by topographic arable land fraction (steep mountains/hills vs flat alluvial plains).
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

            // Asymptotic relaxation towards Liebig-limited target yield when below baseline;
            // when Boserupian hydraulic/terrace intensification has raised baseBiomass above extensive fallow targetYield,
            // apply physical terrace maintenance depreciation (0.4%/yr) bounded by maxCellBiomass * 1.25.
            double updatedBiomass;
            if (tech < 1.5) {
                updatedBiomass = 0.0;
            } else if (baseBiomass <= targetYield) {
                updatedBiomass = baseBiomass + relaxationRate * (targetYield - baseBiomass);
            } else {
                double terraceMaintenanceDecay = Math.min(1.0, 0.004 * Math.max(0.001, dtYears));
                updatedBiomass = baseBiomass - terraceMaintenanceDecay * (baseBiomass - targetYield);
            }
            cell.setBiomassAgriculture(Math.clamp(updatedBiomass, 0.0, maxCellBiomass * 1.25));
        }

        if (haberBoschActive > 0) {
            logger.info("NPK Engine: Haber-Bosch industrial nitrogen synthesis active across {} power grid cells.", haberBoschActive);
        }
    }
}


