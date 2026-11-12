/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Pre-Columbian Amerindian Ecosystem & Virgin-Soil Epidemics Engine Variant B32.1 (Pure) & B32.2 (Hybrid).
 * Models pre-Columbian Amerindian dynamics (Mesoamerica, Andes, North America):
 * 1. Lack of domesticated draft animals (due to Pleistocene megafauna overkill) limits mechanical work EROEI.
 * 2. Virgin-Soil Epidemic Shock: Old World pathogen contact causes catastrophic 85-95% demographic collapse.
 * 3. Agricultural intensification & drought vulnerability (e.g., Maya classic collapse).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AmerindianEcosystemEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(AmerindianEcosystemEngine.class);

    public AmerindianEcosystemEngine() {
        super("AmerindianEcosystemEngine", "Pre-Columbian Amerindian Ecosystem", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double biomassHuman = buffer.getBiomassHuman()[i];
        double pop = biomassHuman / 0.06;
        if (pop <= 0.0) return;

        double livestock = buffer.getBiomassLivestock()[i];
        double kpc = buffer.getResourceCapital()[i] / Math.max(1.0, pop);

        if (livestock < 1.0 && kpc < 1500.0) {
            // Draft megafauna absence constraint: muscle-only ceiling dampens capital accumulation
            acc.dResourceCapital -= 0.005 * buffer.getResourceCapital()[i] * dtYears;
        }

        if (oldWorldContactTriggered && livestock < 5.0) {
            double survivalRate = Math.max(0.01, 1.0 - epidemicMortalityRate * Math.min(1.0, 0.10 * dtYears));
            double newBiomass = Math.max(0.60, biomassHuman * survivalRate);
            acc.dBiomassHuman += (newBiomass - biomassHuman);
        }
    }

    @Override
    public void reset() {
        resetState();
    }

    /** Internal state: old world contact triggered (boolean). */
    private static boolean oldWorldContactTriggered = false;
    private static double epidemicMortalityRate = 0.90; // 90% virgin soil epidemic mortality upon contact

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        oldWorldContactTriggered = false;
        epidemicMortalityRate = 0.90;
    }

    /**
     * Is old world contact triggered.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isOldWorldContactTriggered() { return oldWorldContactTriggered; }
    /**
     * Set old world contact triggered operation.
     * <p>
     * Executes operational logic for {@code AmerindianEcosystemEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param contact the contact argument (boolean)
     */
    public static void setOldWorldContactTriggered(boolean contact) { oldWorldContactTriggered = contact; }

    /**
     * Get epidemic mortality rate.
     *
     * @return the resulting computation or state reference
     */
    public static double getEpidemicMortalityRate() { return epidemicMortalityRate; }
    /**
     * Set epidemic mortality rate operation.
     * <p>
     * Executes operational logic for {@code AmerindianEcosystemEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (double)
     */
    public static void setEpidemicMortalityRate(double rate) { epidemicMortalityRate = Math.max(0.1, Math.min(0.99, rate)); }
}


