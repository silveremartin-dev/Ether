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
 * Entropic Metal Dissipation Engine (Bihouix Recycling Limits).
 * Models irreversible physical dissipation of metals into environment due to entropy generation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EntropicMetalDissipationEngine extends AbstractSimulationEngine {
    public EntropicMetalDissipationEngine() {
        super("EntropicMetalDissipationEngine", "EntropicMetalDissipationEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(EntropicMetalDissipationEngine.class);

    private static double annualDissipationRate = 0.015; // 1.5% annual thermodynamic loss rate

    /**
     * Gets current annual thermodynamic dissipation rate \(\lambda_{\text{diss}}\).
     *
     * @return annual dissipation fraction in \(\text{yr}^{-1}\)
     */
    public static double getAnnualDissipationRate() { return annualDissipationRate; }

    /**
     * Sets annual thermodynamic dissipation rate with physical clamping in \([0.0, 0.20]\).
     *
     * @param rate dissipation fraction in \(\text{yr}^{-1}\)
     */
    public static void setAnnualDissipationRate(double rate) { annualDissipationRate = Math.max(0.0, Math.min(0.20, rate)); }
    
    /**
     * Resets thermodynamic metal dissipation rate for scenario isolation.
     */
    public static void resetState() {
        annualDissipationRate = 0.015;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double metal = buffer.getMetalResource()[i];
        if (metal <= 0.0) return;

        double remainingMetal = Math.max(0.0, metal * Math.exp(-annualDissipationRate * dtYears));
        double dissipated = metal - remainingMetal;
        if (dissipated > 0.0) {
            acc.dMetalResource -= (float) dissipated;
            acc.dPollutionLevel += (float) ((dissipated / 1770.49) * 0.1);
        }
    }
}


