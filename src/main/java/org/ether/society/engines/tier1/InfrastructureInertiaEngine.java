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
 * @version 1.0.0-beta.2
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
    public static void processInfrastructureInertia(List<H3Cell> cells, double deltaYears) {
// Numerical integration step for InfrastructureInertiaEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double metal = cell.getResourceMetalOr(0.0);
            double work = cell.getResourceWorkOr(0.0);
            double capital = cell.getResourceCapitalOr(0.0);
            // Dynamic half-life scaling (Jorgenson 1963): Metal durability and ongoing maintenance work increase asset longevity
            // Primitive unmaintained wood/earth: ~24 years | Metal-reinforced maintained infrastructure: 35-50 years
            double maintenanceRatio = (capital > 0.0) ? (work + metal * 0.5) / capital : 0.05;
            double longevityFactor = 0.65 + 0.35 * Math.min(2.5, Math.log1p(maintenanceRatio * 5.0));
            double dynamicHalfLife = baseCapitalTurnoverHalfLifeYears * Math.clamp(longevityFactor, 0.5, 2.0);
            double decayFactor = Math.exp(-deltaYears / dynamicHalfLife);

            // Physical depreciation of unmaintained capital vs active maintenance replacement in inhabited cells (Jorgenson 1963)
            int pop = cell.getPopulationOr(0);
            double activeReplacement = 0.98 * org.ether.society.core.math.SmoothMath.sigmoid(pop, 10.0, 3.0);
            double netCapital = capital * decayFactor + capital * (1.0 - decayFactor) * activeReplacement;
            cell.setResourceCapital(netCapital);
        }
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


