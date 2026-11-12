/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;

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
public class BoserupAgriculturalIntensificationEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(BoserupAgriculturalIntensificationEngine.class);

    public BoserupAgriculturalIntensificationEngine() {
        super("BoserupAgriculturalIntensificationEngine", "Ester Boserup Agricultural Intensification", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null) return;
        float pop = buffer.getBiomassHuman()[i];
        if (pop > 0.001f) {
            double density = pop / 100.0;
            double densityNorm = Math.pow(Math.max(0.0, density), 1.4);
            double xi = densityNorm / (Math.pow(20.0, 1.4) + densityNorm);
            double capitalDeepeningRate = pop * 2.0 * xi * dtYears;
            acc.dResourceCapital += (float) capitalDeepeningRate;
        }
    }

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
}
