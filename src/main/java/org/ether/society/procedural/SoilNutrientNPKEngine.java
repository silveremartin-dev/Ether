/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 1.0.0-beta.1
 */
public class SoilNutrientNPKEngine {
    private static final Logger logger = LoggerFactory.getLogger(SoilNutrientNPKEngine.class);

    /** Haber-Bosch power requirement in Watts per capita */
    public static final double HABER_BOSCH_POWER_WATTS = 5000.0;

    /**
     * Executes N-P-K stoichiometry, mineral mining extraction, and Haber-Bosch synthesis tick.
     */
    public static void processSoilNutrients(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int haberBoschActive = 0;

        for (H3Cell cell : cells) {
            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double metalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 100.0;

            // 1. Rock Phosphate (P) & Potash (K) Geological Mining Extraction
            double extractedMiningP = Math.min(metalOre * 0.05, 10.0);
            double extractedMiningK = Math.min(metalOre * 0.05, 10.0);

            // 2. Haber-Bosch Industrial Nitrogen Fixation (P_capita >= 5000 W)
            double synthesizedN = 5.0; // Baseline biological leguminous fixation
            if (powerPerCapita >= HABER_BOSCH_POWER_WATTS) {
                haberBoschActive++;
                synthesizedN += 45.0; // Massive industrial ammonia yield
            }

            // 3. Liebig's Law of the Minimum Yield Bound
            double effectiveSoilN = 20.0 + synthesizedN;
            double effectiveSoilP = 15.0 + extractedMiningP;
            double effectiveSoilK = 15.0 + extractedMiningK;

            double liebigFactor = Math.min(effectiveSoilN / 50.0, Math.min(effectiveSoilP / 25.0, effectiveSoilK / 25.0));

            // Apply Liebig yield limit to agricultural biomass
            double baseBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 100.0;
            cell.setBiomassAgriculture(Math.max(20.0, baseBiomass * liebigFactor));
        }

        if (haberBoschActive > 0) {
            logger.info("NPK Engine: Haber-Bosch industrial nitrogen synthesis active across {} power grid cells.", haberBoschActive);
        }
    }
}

