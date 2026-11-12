/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Physical Infrastructure Inertia & Turnover Engine (Jancovici Capital Lock-in).
 * Models physical lifespan of industrial assets (power grids, buildings, heavy machinery)
 * and transition latency (tau_transition).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class InfrastructureInertiaEngine extends AbstractSimulationEngine {
    public InfrastructureInertiaEngine() {
        super("InfrastructureInertiaEngine", "InfrastructureInertiaEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureInertiaEngine.class);

    private static double baseCapitalTurnoverHalfLifeYears = 35.0; // Base 35-year physical turnover benchmark

    /**
     * Process infrastructure inertia.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double metal = buffer.getMetalResource()[i];
        double work = buffer.getInstitutionalComplexity()[i] * 100.0;
        double capital = buffer.getResourceCapital()[i];
        if (capital <= 0.0) return;

        double maintenanceRatio = (work + metal * 0.5) / capital;
        double longevityFactor = 0.65 + 0.35 * Math.min(2.5, Math.log1p(maintenanceRatio * 5.0));
        double dynamicHalfLife = baseCapitalTurnoverHalfLifeYears * Math.clamp(longevityFactor, 0.5, 2.0);
        double decayFactor = Math.exp(-dtYears / dynamicHalfLife);

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        double activeReplacement = 0.98 * org.ether.society.core.math.SmoothMath.sigmoid(pop, 10.0, 3.0);
        double netCapital = capital * decayFactor + capital * (1.0 - decayFactor) * activeReplacement;
        acc.dResourceCapital += (float) (netCapital - capital);
    }

    /**
     * Get capital turnover half life years.
     *
     * @return the resulting computation or state reference
     */
    public static double getCapitalTurnoverHalfLifeYears() { return baseCapitalTurnoverHalfLifeYears; }
    /**
     * Set capital turnover half life years operation.
     * <p>
     * Executes operational logic for {@code InfrastructureInertiaEngine} within the Tier 1 physical conservation solver.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param years the years argument (double)
     */
    public static void setCapitalTurnoverHalfLifeYears(double years) { baseCapitalTurnoverHalfLifeYears = Math.max(1.0, years); }

    /**
     * Resets infrastructure turnover benchmark for scenario isolation.
     */
    public static void resetState() {
        baseCapitalTurnoverHalfLifeYears = 35.0;
    }
}


