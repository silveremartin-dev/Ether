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
 * Elinor Ostrom Model Variant B19.1 (Pure) & B19.2 (Hybrid).
 * Ostrom Law (Governing the Commons - Nobel Prize 2009): Polycentric community-based governance
 * prevents Tragedy of the Commons without total privatization or top-down state coercion.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class OstromCommonsPureEngine extends AbstractSimulationEngine {
    public OstromCommonsPureEngine() {
        super("OstromCommonsPureEngine", "OstromCommonsPureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double aquifer = buffer.getFreshwaterAquifer()[i];
        if (aquifer < 50.0) {
            double rainfall = buffer.getRainfall()[i];
            double naturalInflowCap = Math.max(0.0, (rainfall / 500.0) * 0.5 * dtYears);
            double sustainableRecharge = Math.min(0.5 * dtYears, naturalInflowCap);
            acc.dFreshwaterAquifer += (float) sustainableRecharge;
        }
    }
}


