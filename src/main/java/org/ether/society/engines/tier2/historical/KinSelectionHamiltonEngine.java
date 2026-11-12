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
 * E.O. Wilson &amp; W.D. Hamilton Kin Selection Model Variant B28.1 (Pure) &amp; B28.2 (Hybrid).
 * Hamilton Law (r * B &gt; C): Altruistic cooperation is bounded by genetic/cultural relatedness r.
 * Under food scarcity, inter-group hostility increases toward culturally distant outgroups.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KinSelectionHamiltonEngine extends AbstractSimulationEngine {
    public KinSelectionHamiltonEngine() {
        super("KinSelectionHamiltonEngine", "KinSelectionHamiltonEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    public static boolean checkHamiltonRule(double relatednessR, double benefitB, double costC) {
        return (relatednessR * benefitB) > costC;
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

        double food = buffer.getFoodResource()[i];
        double foodPerCapita = food / pop;
        double scarcityStress = 1.0 / (1.0 + Math.exp(20.0 * (foodPerCapita - 0.05)));
        if (scarcityStress > 0.01) {
            double capital = Math.max(50.0, buffer.getResourceCapital()[i]);
            acc.dResourceCapital += (float) (capital * 0.08 * scarcityStress * dtYears);
        }
    }
}
