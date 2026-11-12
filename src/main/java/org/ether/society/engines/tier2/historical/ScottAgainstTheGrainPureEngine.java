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
 * James C. Scott Model Variant B17.1 (Pure) &amp; B17.2 (Hybrid).
 * Scott Law (Against the Grain): Early grain-based agrarian states suffer initial drops in health,
 * higher epidemic vulnerability, and tax coercion friction compared to mobile foraging populations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ScottAgainstTheGrainPureEngine extends AbstractSimulationEngine {
    public ScottAgainstTheGrainPureEngine() {
        super("ScottAgainstTheGrainPureEngine", "ScottAgainstTheGrainPureEngine", EngineTier.TIER_2_CLIODYNAMIC);
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

        double kPerCapita = buffer.getResourceCapital()[i] / pop;
        double scottVulnerability = Math.exp(-Math.pow(kPerCapita - 150.0, 2.0) / (2.0 * 35.0 * 35.0));

        if (scottVulnerability > 0.01) {
            float lifespan = buffer.getLifespan()[i];
            if (lifespan > 22.0f) {
                acc.dLifespan -= (float) Math.min(lifespan - 22.0, 0.5 * scottVulnerability * dtYears);
            }
            acc.dPollutionLevel += (float) (0.2 * scottVulnerability * dtYears);
        }
    }
}
