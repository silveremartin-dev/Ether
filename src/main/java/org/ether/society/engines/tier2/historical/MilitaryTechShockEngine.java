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
 * Military Revolution &amp; Tactical Shock Engine.
 *
 * <p>Models historical military revolutions (Parker 1996, Roberts 1956) where pyrotechnic and metallurgical
 * breakthroughs induce intense military capital investments and mobilization.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MilitaryTechShockEngine extends AbstractSimulationEngine {
    public MilitaryTechShockEngine() {
        super("MilitaryTechShockEngine", "MilitaryTechShockEngine", EngineTier.TIER_2_CLIODYNAMIC);
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

        double kPerCap = buffer.getResourceCapital()[i] / pop;
        double shockIntensity = Math.exp(-Math.pow(kPerCap - 1500.0, 2.0) / (2.0 * 300.0 * 300.0));
        if (shockIntensity > 0.005) {
            acc.dResourceCapital += (float) (pop * 5.0 * shockIntensity * dtYears);
        }
    }
}
