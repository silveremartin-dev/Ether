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
 * Human Self-Domestication &amp; Prosociality Engine.
 *
 * <p>Formalizes Wrangham (2019, <i>The Goodness Paradox</i>) and Hare (2017): selection against
 * reactive aggression in dense social networks expands cooperative capacity, communal pooling of
 * physical labor, and shared capital formation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SelfDomesticationEngine extends AbstractSimulationEngine {
    public SelfDomesticationEngine() {
        super("SelfDomesticationEngine", "SelfDomesticationEngine", EngineTier.TIER_2_CLIODYNAMIC);
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

        double popDensity = pop / 1000.0;
        double cooperationFactor = SmoothMath.sigmoid(popDensity, 1.0, 0.3);
        acc.dResourceCapital += (float) (pop * 0.05 * cooperationFactor * Math.log1p(popDensity) * dtYears);
    }
}
