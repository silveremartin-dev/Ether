/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Vaclav Smil Model Variant B20.1 (Pure) &amp; B20.2 (Hybrid).
 *
 * <p>Smil Law (Energy and Civilization / Materials): Decarbonization and energy transitions
 * are physically bounded by the 50-70 year turnover inertia of the 4 material pillars
 * (Steel, Cement, Plastics, Ammonia).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SmilMaterialTransitionsPureEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(SmilMaterialTransitionsPureEngine.class);

    private static double transitionInertiaYears = 60.0; // 60-year average physical capital replacement time

    public SmilMaterialTransitionsPureEngine() {
        super("SmilMaterialTransitionsPureEngine", "Vaclav Smil Material Turnover Inertia", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getResourceCapital() == null) return;
        float cap = buffer.getResourceCapital()[i];
        if (cap > 0.001f) {
            double decayFactor = Math.exp(-dtYears / transitionInertiaYears);
            double target = cap * decayFactor + cap * (1.0 - decayFactor) * 0.95;
            acc.dResourceCapital += (float) (target - cap);
        }
    }

    public static double getTransitionInertiaYears() {
        return transitionInertiaYears;
    }

    public static void setTransitionInertiaYears(double years) {
        transitionInertiaYears = Math.max(1.0, years);
    }

    /**
     * Resets transition inertia years for scenario isolation.
     */
    public static void resetState() {
        transitionInertiaYears = 60.0;
    }

    @Override
    public void reset() {
        resetState();
    }
}
