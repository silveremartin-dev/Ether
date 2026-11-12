/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
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
 * Nordhaus DICE Hybrid Model Variant B8.2 (Nordhaus DICE + Ether H3 Grid Integration).
 * Injects Nordhaus quadratic climate damage function D(T_cell) directly into Ether H3 cell capital.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class NordhausDiceHybridEngine extends AbstractSimulationEngine {
    public NordhausDiceHybridEngine() {
        super("NordhausDiceHybridEngine", "NordhausDiceHybridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(NordhausDiceHybridEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double temp = buffer.getTemperature()[i];
        double tempAnomaly = Math.max(0.0, temp - 15.0);
        double damageFraction = Math.min(0.50, 0.00236 * tempAnomaly * tempAnomaly);
        float capital = buffer.getResourceCapital()[i];
        if (capital > 0.0f && damageFraction > 0.0) {
            acc.dResourceCapital += (float) (Math.max(0.0, capital * Math.exp(-damageFraction * dtYears)) - capital);
        }
    }
}


