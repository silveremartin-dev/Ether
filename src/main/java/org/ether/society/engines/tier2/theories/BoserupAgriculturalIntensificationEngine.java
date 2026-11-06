/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.engines.tier1.SoilNutrientNPKEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ester Boserup Agricultural Intensification &amp; Induced Innovation Engine.
 *
 * <p>Formalizes Boserup (1965, <i>The Conditions of Agricultural Growth</i>): demographic density
 * forces a transition from long fallow to short fallow, multicropping, and land-saving agricultural
 * capital deepening (irrigation canalization, terracing, heavy plows), despite diminishing marginal
 * returns to direct labor.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BoserupAgriculturalIntensificationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(BoserupAgriculturalIntensificationEngine.class);

    @Override
    public String getName() {
        return "BoserupAgriculturalIntensificationEngine";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Boserup Agricultural Intensification Dynamics (1965)]
               • Demographic Pressure:   ρ = N / Area_arable
               • Cultivation Shift:      dB_agr/dt = μ · σ(ρ; ρ_0, s) · (K_carrying - B_agr)
               • Fallow Shortening:      Forest fallow -> Bush -> Short -> Annual multicrop
               Ref: E. Boserup (1965) "The Conditions of Agricultural Growth", Allen & Unwin
               """;
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processBoserupIntensification(cells, deltaYears);
    }

    public static void processBoserupIntensification(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double cellArea = Math.max(10.0, cell.getCellAreaKm2());
            double areaScale = Math.max(0.05, cellArea / 1770.49);
            double density = pop / cellArea;

            // Arable suitability and continuous cultivation activation
            double arableBiomeFactor = SoilNutrientNPKEngine.calculateContinuousArableSuitability(cell);
            double cultivationActivation = SmoothMath.sigmoid(density, 2.0, 0.5);

            double agriBiomass = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                    ? cell.getBiomassAgriculture()
                    : Math.min(2000.0 * areaScale * arableBiomeFactor, Math.max(50.0 * areaScale * arableBiomeFactor, pop * 2.5));

            // Continuous Boserupian intensification factor xi in [0, 1] (Hill function with n=1.4, K=20 hab/km2)
            double densityNorm = Math.pow(Math.max(0.0, density), 1.4);
            double xi = densityNorm / (Math.pow(20.0, 1.4) + densityNorm);

            // Boserup (1965): Agricultural intensification multiplies land productivity via terracing and irrigation
            double boserupYieldMultiplier = 1.0 + 0.25 * xi * cultivationActivation;
            double techLandCeiling = pop * 600.0 * (1.0 + xi);
            double effectiveCeiling = Math.max(agriBiomass * boserupYieldMultiplier, techLandCeiling);

            double newAgriBiomass = Math.min(effectiveCeiling, agriBiomass * boserupYieldMultiplier);
            cell.setBiomassAgriculture(newAgriBiomass);

            // Induced capital deepening: demographic density compels construction of durable hydraulic/land capital
            double capitalDeepeningRate = pop * 2.0 * xi * deltaYears;
            cell.setResourceCapital(cell.getResourceCapitalOr(0.0) + capitalDeepeningRate);

            // Boserup Axiom: Marginal returns to labor decrease under intensive cropping
            double work = cell.getResourceWorkOr(50.0);
            double workAbsorptionRate = SmoothMath.smoothClamp(0.025 * xi * deltaYears, 0.0, 0.90, 20.0);
            cell.setResourceWork(Math.max(10.0, work * (1.0 - workAbsorptionRate)));
        }
    }
}
