/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.engines.tier1.SoilNutrientNPKEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Boserupian Agricultural Intensification & Demographic Pressure Engine (Ester Boserup, 1965).
 *
 * <p>Models endogenous technological shifts driven by demographic carrying capacity pressure:</p>
 * <pre>
 *   Stage: Foraging → Forest-Fallow → Bush-Fallow → Short-Fallow → Multi-Cropping Irrigation
 *   Labor_req ∝ Yield^1.40
 *   Trigger: Density = Population / Habitable_Area ≥ Threshold_k
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BoserupAgriculturalIntensificationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(BoserupAgriculturalIntensificationEngine.class);

    @Override
    public String getName() {
        return "Boserup Agricultural Intensification";
    }

    @Override
    public String getDescription() {
        return "Models population pressure forcing technological transitions to higher-yield, higher-labor intensive agricultural systems.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Boserup Agricultural Intensification Model (1965)]
               • Density Trigger:     D_i = N_i / Area_km2
               • Labor Demand:        L_req = L_0 · (Yield_target / Yield_0)^1.40
               • Technology Stages:   1: Foraging (D < 2), 2: Long Fallow (D < 10), 3: Annual Crop (D < 50), 4: Multi-Crop Irrigation (D >= 50)
               • Boserup Axiom:       Population pressure is the mother of agricultural innovation (anti-Malthusian).
               Units: D [hab/km²], L_req [heures-homme/hectare], Yield [kg/ha]
               Ref: E. Boserup (1965) "The Conditions of Agricultural Growth"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Agrarian Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            // Calculate genuine geographic density in hab/km² based on planar H3 resolution area
            int h3Res = (cell.getH3Index() != null && cell.getH3Index() != 0L)
                    ? (int) ((cell.getH3Index() >>> 52) & 0xFL)
                    : 3;
            double cellArea = Math.max(10.0, H3Cell.getAvgHexagonAreaKm2((h3Res >= 0 && h3Res <= 15) ? h3Res : 3));
            double areaScale = Math.max(0.05, cellArea / 1770.49);
            double density = pop / cellArea;
            double tech = cell.getTechnologyLevelOr(1.0);
            if (tech < 1.5 && density < 2.0) {
                // Stage 1 Foraging (D < 2 hab/km², tech < 1.5): no crop cultivation
                continue;
            }

            // Continuous arability suitability replaces discrete switch(biome)
            double arableBiomeFactor = SoilNutrientNPKEngine.calculateContinuousArableSuitability(cell);
            double agriBiomass = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                    ? cell.getBiomassAgriculture()
                    : Math.min(2000.0 * areaScale * arableBiomeFactor, Math.max(50.0 * areaScale * arableBiomeFactor, pop * 2.5));

            double techLandCeiling = Math.max(pop * (400.0 + 50.0 * Math.min(tech, 10.0)) * 1.5, (360000.0 + 550000.0 * Math.pow(Math.max(0.0, tech - 1.4), 1.5)) * areaScale * arableBiomeFactor);

            // Continuous Boserupian intensification factor xi in [0, 1] (Hill function with n=1.4, K=20 hab/km2)
            // Ester Boserup (1965): Population pressure forces transition from extensive fallow to intensive multi-cropping
            double densityNorm = Math.pow(Math.max(0.0, density), 1.4);
            double xi = densityNorm / (Math.pow(20.0, 1.4) + densityNorm);

            // Continuous growth rate: from 1.5%/yr (extensive fallow) up to 4.5%/yr (multi-crop terrace irrigation)
            double growthRate = 0.015 + 0.030 * xi;
            double ceilingMultiplier = 1.0 + 0.35 * xi;
            double effectiveCeiling = techLandCeiling * ceilingMultiplier;

            double newAgriBiomass = Math.min(effectiveCeiling, agriBiomass + (agriBiomass * growthRate * deltaYears));
            cell.setBiomassAgriculture(newAgriBiomass);

            // Boserupian endogenous innovation rate: higher carrying capacity pressure spurs technical adaptations
            double techIncrement = (0.0010 + 0.0040 * xi) * deltaYears;
            double techCeiling = (density < 2.0) ? 3.5 : 10.0;
            cell.setTechnologyLevel(Math.min(techCeiling, tech + techIncrement));

            // Boserup Axiom: Marginal returns to labor decrease under intensive cropping (L_req ∝ Yield^1.4)
            // Available mechanical/labor work is absorbed as a continuous rate scaled by deltaYears
            double work = cell.getResourceWorkOr(50.0);
            double workAbsorptionRate = Math.clamp(0.025 * xi * deltaYears, 0.0, 0.90);
            cell.setResourceWork(Math.max(10.0, work * (1.0 - workAbsorptionRate)));
        }
    }
}
