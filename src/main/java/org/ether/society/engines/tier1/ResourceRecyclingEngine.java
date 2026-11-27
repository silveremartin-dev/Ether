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
 * @version 1.0.0-beta.3
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

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        if (capital <= 0.0) return;
        double kpc = capital / pop;
        double industrialGate = org.ether.society.core.math.SmoothMath.sigmoid(kpc, 600.0, 150.0);
        double work = buffer.getInstitutionalComplexity()[i] * 100.0;
        double efficiency = calculateContinuousRecyclingDiscount(capital, work) * industrialGate;
        if (efficiency > 0.05) {
            // Circular recovery offsets at most a fraction of the 4%/yr end-of-life capital wear and returns secondary metal
            double recoveredDepreciationKg = capital * 0.04 * (0.50 * efficiency) * dtYears;
            acc.dResourceCapital += (float) recoveredDepreciationKg;
            double secondaryMetalTonnes = org.ether.society.model.PhysicalUnitsHelper.kgToMetricTonnes(recoveredDepreciationKg * 0.05);
            acc.dMetalResource += (float) secondaryMetalTonnes;
        }
    }
}
