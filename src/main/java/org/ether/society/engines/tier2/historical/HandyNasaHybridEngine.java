/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * NASA HANDY Hybrid Model Variant B7.2 (NASA HANDY + Ether H3 Grid Integration).
 * Injects wealth inequality Gini penalization and ecological carrying capacity depletion
 * directly into Ether H3 cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HandyNasaHybridEngine extends AbstractSimulationEngine {
    public HandyNasaHybridEngine() {
        super("HandyNasaHybridEngine", "HandyNasaHybridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(HandyNasaHybridEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0) return;

        double biomass = buffer.getBiomassNatural()[i];
        double density = pop / 100.0;
        double maxBiomass = 100.0;
        double regeneration = 0.02 * biomass * Math.max(0.0, 1.0 - biomass / maxBiomass);
        double depletion = 0.0002 * Math.min(density, 500.0) * (biomass / maxBiomass);
        acc.dBiomassNatural += (float) ((regeneration - depletion) * dtYears);
    }
}
