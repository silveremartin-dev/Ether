/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marvin Harris Cultural Materialism Model Variant B13.1 (Pure) & B13.2 (Hybrid).
 * Postulates deterministic causal cascade: Infrastructure (Energy/Environment) -> Structure (Economy/Polity) -> Superstructure (Ideology).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class CulturalMaterialismPureEngine extends AbstractSimulationEngine {
    public CulturalMaterialismPureEngine() {
        super("CulturalMaterialismPureEngine", "CulturalMaterialismPureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(CulturalMaterialismPureEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double food = buffer.getFoodResource()[i];
        double pop = buffer.getBiomassHuman()[i];
        if (pop > 0.0 && (food / pop) < 0.05) {
            double decay = Math.max(0.0, 1.0 - 0.10 * dtYears);
            float capital = buffer.getResourceCapital()[i];
            acc.dResourceCapital += (float) (Math.max(0.0, capital * decay) - capital);
        }
    }
}
