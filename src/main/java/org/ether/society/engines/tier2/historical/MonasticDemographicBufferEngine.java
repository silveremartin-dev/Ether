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
 * Celibate Monastic Demographic Buffer Engine Variant B23.1 (Pure) &amp; B23.2 (Hybrid).
 * Diverts 2% to 10% of adult population into celibate monastic orders (e.g. Medieval Europe 2-4%, Tibet 10%),
 * absorbing Malthusian demographic overpressure by dampening fertility without triggering war.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class MonasticDemographicBufferEngine extends AbstractSimulationEngine {
    public MonasticDemographicBufferEngine() {
        super("MonasticDemographicBufferEngine", "MonasticDemographicBufferEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(MonasticDemographicBufferEngine.class);

    private static double monasticFraction = 0.04; // 4% default monastic buffer fraction

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0 || dtYears <= 0.0) return;

        double food = buffer.getFoodResource()[i];
        double foodPerCapita = food / Math.max(1.0, pop);
        double malthusianDeficit = Math.max(0.0, 1.0 - foodPerCapita / 0.10);
        if (malthusianDeficit > 0.0) {
            double fertility = buffer.getFertility()[i];
            double monasticDamping = monasticFraction * malthusianDeficit * 0.20 * dtYears;
            acc.dFertility -= (float) Math.min(Math.max(0.0, fertility - 1.0), monasticDamping);
        }
    }

    /**
     * Get monastic fraction.
     *
     * @return the resulting computation or state reference
     */
    public static double getMonasticFraction() { return monasticFraction; }

    /**
     * Set monastic fraction operation.
     * <p>
     * Executes operational logic for {@code MonasticDemographicBufferEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param fraction the fraction argument (double)
     */
    public static void setMonasticFraction(double fraction) { monasticFraction = Math.max(0.0, Math.min(0.20, fraction)); }

    /**
     * Resets monastic fraction for scenario isolation.
     */
    public static void resetState() {
        monasticFraction = 0.04;
    }
}
