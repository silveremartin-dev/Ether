/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Language, Cultural Isolation & Linguistic Diffusion Engine.
 * Dynamic simulation models:
 * 1. <b>Isolat Culturel / Insularité (Cultural Isolation like Japan/Sakoku)</b>: Island & geographically isolated
 *    territories develop high internal cultural cohesion, preserving unique linguistic groups and resisting
 *    foreign assimilation and uncontrolled immigration.
 * 2. <b>Linguistic Divergence & Dialect Speciation</b>: High terrain friction accelerates local dialect formation.
 * 3. <b>Lingua Franca Diffusion</b>: Trade corridors and imperial capitals diffuse unified languages.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class LanguageLinguisticEngine extends AbstractSimulationEngine {
    public LanguageLinguisticEngine() {
        super("LanguageLinguisticEngine", "LanguageLinguisticEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(LanguageLinguisticEngine.class);
    private static long baseSeed = 42L;

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        baseSeed = 42L;
    }

    /**
     * Sets the scenario-derived random seed for deterministic language assimilation and diffusion.
     *
     * @param seed the scenario seed (long)
     */
    public static void setBaseSeed(long seed) {
        baseSeed = seed;
    }

    /**
     * Gets the scenario-derived random seed for deterministic language assimilation and diffusion.
     *
     * @return the base seed (long)
     */
    public static long getBaseSeed() {
        return baseSeed;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (buffer.getBiomassHuman()[i] <= 0.0f) return;
        double elevation = buffer.getElevation()[i];
        boolean isIsolated = elevation > 1200.0;
        if (!isIsolated) {
            acc.dInstitutionalComplexity += (float) (0.01 * dtYears);
        }
    }
}


