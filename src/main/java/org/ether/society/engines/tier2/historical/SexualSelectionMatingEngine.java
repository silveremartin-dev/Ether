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
 * David Buss Evolutionary Psychology Mating Model Variant B30.1 (Pure) & B30.2 (Hybrid).
 * Buss Law (Evolutionary Psychology): Asymmetric resource concentration leads to elite polygyny,
 * creating a surplus of unmated young males who are mobilized into high-risk military/colonial expansion
 * (Buss 1989; Henrich, Boyd & Richerson 2012).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SexualSelectionMatingEngine extends AbstractSimulationEngine {
    public SexualSelectionMatingEngine() {
        super("SexualSelectionMatingEngine", "SexualSelectionMatingEngine", EngineTier.TIER_2_CLIODYNAMIC);
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

        double gini = buffer.getGiniIndex()[i];
        double matingSkew = 1.0 / (1.0 + Math.exp(-10.0 * (gini - 0.45)));
        if (matingSkew > 0.05) {
            double capital = Math.max(50.0, buffer.getResourceCapital()[i]);
            acc.dResourceCapital += (float) (capital * 0.03 * matingSkew * dtYears);
        }
    }
}
