/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meadows / Forrester World3 Hybrid Engine Variant B21.2.
 * Couples local cell extraction/pollution directly into resource capital and life expectancy.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class World3HybridEngine extends AbstractSimulationEngine {
    public World3HybridEngine() {
        super("World3HybridEngine", "World3HybridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(World3HybridEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (buffer.getBiomassHuman()[i] <= 0.0f) return;

        double metal = buffer.getMetalResource()[i];
        float capital = buffer.getResourceCapital()[i];
        double pollution = buffer.getPollutionLevel()[i];

        double fcaor = 0.05 + 0.85 * Math.pow(Math.max(0.0, 1.0 - (metal / 50.0)), 2.0);
        double netCapitalOutput = Math.max(0.0, capital * (1.0 - fcaor * dtYears));
        acc.dResourceCapital += (float) (netCapitalOutput - capital);

        if (pollution > 20.0) {
            float lifespan = buffer.getLifespan()[i];
            double pollPenalty = Math.min(0.40, (pollution - 20.0) * 0.005 * dtYears);
            double newLifespan = Math.max(20.0, lifespan * (1.0 - pollPenalty));
            acc.dLifespan += (float) (newLifespan - lifespan);
        }
    }
}
