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
 * Roman Imperial Cliodynamic &amp; Expansion Engine Variant B34.1 (Pure) &amp; B34.2 (Hybrid).
 *
 * <p>Models ancient imperial dynamics (Rome, Han, Carthage):
 * 1. Territorial expansion increases administrative overhead costs quadratically \(O(R^2)\).
 * 2. Elite overproduction &amp; corruption reduce tax extraction efficiency.
 * 3. Urban lead toxicity &amp; plague shocks cause periodic demographic collapse.
 * 4. Frontier Asabiyyah mismatch along Limes borders triggers barbarian invasions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class RomanImperialCliodynamicEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(RomanImperialCliodynamicEngine.class);

    private static double imperialRadiusKm = 1500.0;
    private static double eliteOverproductionFactor = 0.15;

    public RomanImperialCliodynamicEngine() {
        super("RomanImperialCliodynamicEngine", "Roman Imperial Overstretch", EngineTier.TIER_2_CLIODYNAMIC);
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
            double overstretchRate = (0.02 * Math.pow(imperialRadiusKm / 1000.0, 2.0) + 0.03 * eliteOverproductionFactor);
            double netCapital = capital * Math.exp(-overstretchRate * dtYears);
            acc.dResourceCapital += (float) (netCapital - capital);
        }
    }

    public static void resetState() {
        imperialRadiusKm = 1500.0;
        eliteOverproductionFactor = 0.15;
    }

    @Override
    public void reset() {
        resetState();
    }

    public static double getImperialRadiusKm() {
        return imperialRadiusKm;
    }

    public static void setImperialRadiusKm(double radius) {
        imperialRadiusKm = Math.max(100.0, radius);
    }

    public static double getEliteOverproductionFactor() {
        return eliteOverproductionFactor;
    }

    public static void setEliteOverproductionFactor(double factor) {
        eliteOverproductionFactor = Math.max(0.0, Math.min(1.0, factor));
    }
}
