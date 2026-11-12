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
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Leslie White Energy &amp; Cultural Evolution Engine.
 *
 * <p>Formalizes Leslie White's Law (1949, <i>The Science of Culture</i>): cultural evolution and
 * civilizational complexity scale directly with the per-capita energy captured and the physical
 * efficiency of structural capital tools (\(C = E \cdot T\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LeslieWhitePureEngine extends AbstractSimulationEngine {
    public LeslieWhitePureEngine() {
        super("LeslieWhitePureEngine", "LeslieWhitePureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    /**
     * Computes Leslie White's cultural complexity metric \(C = E \cdot \eta\).
     *
     * @param energyPerCapita per-capita energy flux \(E\)
     * @param toolEfficiency  structural tool efficiency \(\eta\)
     * @return cultural complexity index \(C\)
     */
    public static double calculateCulturalComplexity(double energyPerCapita, double toolEfficiency) {
        return Math.max(0.0, energyPerCapita) * Math.max(0.0, toolEfficiency);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0) return;

        double foodEnergy = buffer.getEnergyFoodConsumed()[i];
        if (foodEnergy <= 0.0) foodEnergy = 3.5 * pop;
        double exosomaticEnergy = buffer.getEnergyFire()[i]
                + buffer.getEnergyWind()[i]
                + buffer.getEnergySolar()[i]
                + (buffer.getBiomassLivestock()[i] * 15.0)
                + (buffer.getResourceCoal()[i] * 24.0)
                + (buffer.getResourceOil()[i] * 42.0);
        double totalEnergy = foodEnergy + exosomaticEnergy;
        double energyPerCapita = totalEnergy / pop;

        double capitalIncrement = pop * (energyPerCapita / 50.0) * 0.005 * dtYears;
        acc.dResourceCapital += (float) capitalIncrement;
    }
}
