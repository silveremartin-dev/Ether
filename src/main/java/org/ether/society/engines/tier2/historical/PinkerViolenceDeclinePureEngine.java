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
 * Steven Pinker Model Variant B16.1 (Pure) & B16.2 (Hybrid).
 * Pinker Law (Better Angels / Enlightenment): State monopolization of violence (Leviathan), commerce,
 * and literacy drive long-term secular decline in interpersonal and inter-state violent mortality per capita.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PinkerViolenceDeclinePureEngine extends AbstractSimulationEngine {
    public PinkerViolenceDeclinePureEngine() {
        super("PinkerViolenceDeclinePureEngine", "PinkerViolenceDeclinePureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PinkerViolenceDeclinePureEngine.class);

    /**
     * Calculate violent death rate per capita.
     *
     * @param literacyRate the literacy rate parameter (double)
     * @param stateStrength the state strength parameter (double)
     * @param tradeOpenness the trade openness parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateViolentDeathRatePerCapita(double literacyRate, double stateStrength, double tradeOpenness) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        // Pinker decline formula: baseline violence drops exponentially with Leviathan + Trade + Enlightenment
        double pacificationFactor = (stateStrength * 0.40) + (tradeOpenness * 0.30) + (literacyRate * 0.30);
        return Math.max(0.00001, 0.005 * Math.exp(-3.0 * pacificationFactor));
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0) return;

        double kpc = buffer.getResourceCapital()[i] / pop;
        double literacy = kpc < 30.0 ? 0.0 : Math.clamp(Math.tanh((kpc - 30.0) / 1500.0), 0.0, 1.0);
        double stateStrength = kpc < 50.0 ? 0.0 : Math.clamp(Math.tanh((kpc - 50.0) / 800.0), 0.0, 1.0);
        double tradeOpenness = 0.50;

        double violentMortalityRate = calculateViolentDeathRatePerCapita(literacy, stateStrength, tradeOpenness);
        double violentDeaths = pop * violentMortalityRate * dtYears;
        if (violentDeaths > 0.0) {
            acc.dBiomassHuman -= (float) Math.min(Math.max(0.0, pop - 1.0), violentDeaths);
        }
    }
}


