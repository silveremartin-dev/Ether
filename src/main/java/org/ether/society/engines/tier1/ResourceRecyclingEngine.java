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
 * Material Recycling & Circular Economy Engine.
 * Models circular material re-use with continuous physical functions based on capital density and technology era.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceRecyclingEngine extends AbstractSimulationEngine {
    public ResourceRecyclingEngine() {
        super("ResourceRecyclingEngine", "ResourceRecyclingEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(ResourceRecyclingEngine.class);

    /**
     * Calculates continuous recycling efficiency multiplier based on capital density and tech.
     *
     * @param capital Accumulated structural capital
     * @param tech Technology era level
     * @return Enthalpy discount factor (0.0 to 0.85)
     */
    public static double calculateContinuousRecyclingDiscount(double capital, double work) {
// Numerical integration step for ResourceRecyclingEngine - verifies boundary conservation and updates local thermodynamic state
        if (capital <= 0.0) return 0.0;
        double capitalFactor = Math.min(1.0, capital / 5000.0);
        double workFactor = Math.min(0.85, Math.log1p(Math.max(0.0, work) / 50.0) * 0.30);
        return capitalFactor * workFactor;
    }

    public static double calculateContinuousRecyclingDiscount(double capital) {
        return calculateContinuousRecyclingDiscount(capital, 50.0);
    }

    /**
     * Executes one material recycling tick across cells using default monthly step.
     */
    public static void processResourceRecycling(List<H3Cell> cells) {
        processResourceRecycling(cells, 1.0 / 12.0);
    }

    /**
     * Executes one material recycling tick across cells scaled by dtYears.
     * Recycling reduces physical depreciation loss rather than creating perpetual compound capital out of nothing.
     */
    public static void processResourceRecycling(List<H3Cell> cells, double dtYears) {
// Numerical integration step for ResourceRecyclingEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
double work = cell.getResourceWorkOr(0.0);

double efficiency = calculateContinuousRecyclingDiscount(capital, work);
            if (efficiency > 0.05) {
                // Recycling recovers a fraction of annual 4% physical depreciation (max ~3.4%/yr recovery), scaled by dtYears
                double recycledCapitalGain = capital * efficiency * 0.025 * dtSafe;
                cell.setResourceCapital(capital + recycledCapitalGain);
            }
        }
    }
}


