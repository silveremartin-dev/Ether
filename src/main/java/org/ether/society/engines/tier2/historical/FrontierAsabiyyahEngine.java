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
 * Peter Turchin Frontier Asabiyyah Model Variant B29.1 (Pure) &amp; B29.2 (Hybrid).
 * Turchin Law (Historical Dynamics): Collective solidarity (Asabiyyah) is forged at hostile meta-ethnic frontiers
 * and decays in rich imperial hinterland centers due to Pareto inequality and luxury.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class FrontierAsabiyyahEngine extends AbstractSimulationEngine {
    public FrontierAsabiyyahEngine() {
        super("FrontierAsabiyyahEngine", "FrontierAsabiyyahEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double capital = buffer.getResourceCapital()[i];
        double relAffluence = capital / 1000.0;
        double decayRate = 0.04 * (relAffluence / (1.0 + relAffluence));
        double growthRate = 0.04 / (1.0 + relAffluence);
        double netRate = (growthRate - decayRate) * dtYears;
        acc.dInstitutionalComplexity += (float) netRate;
    }
}
