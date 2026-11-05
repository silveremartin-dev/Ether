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
 * 4. <b>Continuous FAO GAEZ Arability Suitability</b>: Closed-form physical function of topographic slope, growing season temperature, and SOC.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SoilNutrientNPKEngine {
    private static final Logger logger = LoggerFactory.getLogger(SoilNutrientNPKEngine.class);

    /** Haber-Bosch power requirement in Watts per capita */
    public static final double HABER_BOSCH_POWER_WATTS = 5000.0;

    /**
     * Calculates continuous agro-ecological arability suitability $S_{\text{arable}} \in [0.05, 1.0]$
     * based on the FAO GAEZ framework (Fischer et al. 2002; Ramankutty et al. 2002).
     *
     * <p>Modeled as the continuous analytical product of relief, thermal growing-season limit, moisture, and SOC:
     * $$S_{\text{arable}} = S_{\text{relief}} \cdot S_{\text{thermal}} \cdot S_{\text{moisture}} \cdot S_{\text{edaphic}}$$
     *
     * @param cell H3 cell
     * @return arable suitability factor in $[0.05, 1.0]$
     */
    public static double calculateContinuousArableSuitability(H3Cell cell) {
        if (cell == null) return 0.50;
        double elev = cell.getElevationOr(0.0);
        if (elev < 0.0) return 0.0; // Open water

        double tempC = cell.getTemperatureOr(15.0);
        double rainMm = cell.getRainfallOr(600.0);
        double soc = cell.getSoilOrganicCarbonOr(40.0);

        // 1. Relief suitability (declining exponentially on steep mountain topography)
        double sRelief = 1.0 / (1.0 + Math.exp(0.0018 * (elev - 1400.0)));

        // 2. Thermal suitability (growing season GDD response above base temperature T_base = 5°C)
        double sThermal = 1.0 / (1.0 + Math.exp(-0.35 * (tempC - 7.5)));

        // 3. Moisture suitability (effective rain vs evapotranspiration)
        double sMoisture = (rainMm / (rainMm + 250.0)) * (1.0 - (0.40 / (1.0 + Math.exp(-0.003 * (rainMm - 2800.0)))));

        // 4. Edaphic suitability (soil organic carbon fertility)
        double sEdaphic = Math.clamp(soc / 35.0, 0.20, 1.0);

        return Math.clamp(sRelief * sThermal * sMoisture * sEdaphic, 0.05, 1.0);
    }

    public static void processSoilNutrients(List<H3Cell> cells) {
        processSoilNutrients(cells, 1.0 / 12.0);
    }

    /**
     * Executes N-P-K stoichiometry, mineral mining extraction, and Haber-Bosch synthesis tick.
     */
    public static void processSoilNutrients(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;

        int size = cells.size();
        H3Cell sample = cells.get(0);
        int h3Res = (sample != null && sample.getH3Index() != null && sample.getH3Index() != 0L)
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
            if (cell == null) return;
            int pop = cell.getPopulationOr(0);
            double baseBiomass = cell.getBiomassAgricultureOr(0.0);
            if (baseBiomass <= 0.0 || pop <= 0) return;

            double capital = cell.getResourceCapitalOr(0.0);
            double capPerCap = capital / Math.max(1.0, pop);
            double cultivationFactor = Math.tanh(capPerCap / 5.0);
            if (cultivationFactor <= 0.001) return;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
double metalOre = cell.getResourceMetalOr(100.0);

// 1. Rock Phosphate (P) & Potash (K) Geological Mining Extraction
double extractedMiningP = Math.min(metalOre * 0.05, minMinedOreScaled);
double extractedMiningK = Math.min(metalOre * 0.05, minMinedOreScaled);

// Enforce conservation of mass: deduct mined fertilizer ore from the geological mineral stock
double totalMinedOre = (extractedMiningP + extractedMiningK) * dtFactor;
cell.setResourceMetal(Math.max(0.0, metalOre - totalMinedOre));

// 2. Biological & Haber-Bosch Industrial Nitrogen Fixation (Smil 2001, Enriching the Earth)
// Pre-industrial biological fixation (rhizobia legumes / crop rotations) smoothly transitions to
// catalytic high-pressure Haber-Bosch synthesis (N2 + 3H2 <=> 2NH3 at 20 MPa, 450 C, Fritz Haber 1909)
// governed by physical electrical/mechanical power capacity.
double haberBoschShare = 1.0 / (1.0 + Math.exp(-0.01 * (powerPerCapita - HABER_BOSCH_POWER_WATTS)));
double bioFixationN = Math.max(0.0, nNorm - soilNBase) * (1.0 - haberBoschShare);
            if (haberBoschShare > 0.5) {
                haberBoschCounter.incrementAndGet();
            }
            double synthesizedN = baselineN + bioFixationN + (industrialBonusN * haberBoschShare);

            // 3. Liebig's Law of the Minimum Yield Bound
            double effectiveSoilN = soilNBase + synthesizedN;
            double effectiveSoilP = Math.min(pNorm * 1.5, soilPBase + extractedMiningP);
            double effectiveSoilK = Math.min(kNorm * 1.5, soilKBase + extractedMiningK);

            double liebigFactor = Math.min(effectiveSoilN / nNorm, Math.min(effectiveSoilP / pNorm, effectiveSoilK / kNorm));

            // Continuous arability suitability replaces discrete switch(biome)
            double arableBiomeFactor = calculateContinuousArableSuitability(cell);
            capPerCap = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
            double laborCultivatedBiomass = pop * (580.0 + 150.0 * Math.min(4.0, Math.log1p(capPerCap / 50.0)));
double landCeiling = (planarCellAreaKm2 < 5.0)
        ? (25.0 * areaScale * arableBiomeFactor)
        : Math.max(laborCultivatedBiomass * 1.5, (360000.0 + 550000.0 * Math.min(5.0, 1.0 + capPerCap / 100.0)) * areaScale * arableBiomeFactor);
double maxCellBiomass = landCeiling;
double potentialBiomass = (planarCellAreaKm2 < 5.0)
        ? (25.0 * areaScale * arableBiomeFactor)
        : Math.max(25.0 * areaScale * arableBiomeFactor, laborCultivatedBiomass);
double targetYield = potentialBiomass * liebigFactor;

double updatedBiomass;
if (baseBiomass <= 0.0) {
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
