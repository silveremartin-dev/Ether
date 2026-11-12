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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * World3 Coupled Dynamic System Engine (Meadows et al., MIT / Club of Rome 1972).
 *
 * <p>Formally implements the coupled sub-systems:
 * 1. Industrial Capital (IC) &amp; FCAOR (Fraction of Capital Allocated to Obtaining Resources)
 * 2. Non-Renewable Resources (NR) Depletion Rate (NRUR)
 * 3. Persistent Pollution Generation (PPG) &amp; Biospheric Assimilation (PPA)</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class World3CouplingEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(World3CouplingEngine.class);

    private static double initialGlobalResourceStock = 1000.0;

    public World3CouplingEngine() {
        super("World3CouplingEngine", "World3 Coupled Dynamics", EngineTier.TIER_2_CLIODYNAMIC);
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
            float capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0f;
            float resource = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0f;
            float pollution = buffer.getPollutionLevel() != null ? buffer.getPollutionLevel()[i] : 0.0f;
            double areaKm2 = 100.0;

            double resourceRatio = Math.max(0.01, Math.min(1.0, resource / initialGlobalResourceStock));
            double fcaor = 0.05 + 0.90 * Math.pow(1.0 - resourceRatio, 2.0);
            double industrialOutput = capital * (1.0 - fcaor);

            double ppg = (industrialOutput / areaKm2) * 0.01 * dtYears;
            double ppaFactor = Math.exp(-0.05 * Math.max(0.001, dtYears));
            double targetPollution = Math.max(0.0, pollution * ppaFactor + ppg);
            acc.dPollutionLevel += (float) (targetPollution - pollution);
        }
    }

    public static double getInitialGlobalResourceStock() {
        return initialGlobalResourceStock;
    }

    public static void setInitialGlobalResourceStock(double stock) {
        initialGlobalResourceStock = Math.max(1.0, stock);
    }

    public static void resetState() {
        initialGlobalResourceStock = 1000.0;
    }

    @Override
    public void reset() {
        resetState();
    }
}
