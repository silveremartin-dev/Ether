/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.HabitatType;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * In-Situ Resource Utilization (ISRU) and Autarky Engine.
 * Models sealed closed-loop off-world and high-frontier habitats.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class IsruAutarkyAndSpaceColonizationEngine extends AbstractSimulationEngine {
    public IsruAutarkyAndSpaceColonizationEngine() {
        super("IsruAutarkyAndSpaceColonizationEngine", "IsruAutarkyAndSpaceColonizationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(IsruAutarkyAndSpaceColonizationEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double dt = Math.max(0.001, dtYears);
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        double capPerCap = capital / pop;
        double isruEfficiency = SmoothMath.smoothClamp(0.20 + (capPerCap * 0.005), 0.20, 2.50, 20.0);
        acc.dMetalResource += (float) (pop * 0.50 * isruEfficiency * dt);
        acc.dResourceCapital += (float) (pop * 2.0 * isruEfficiency * dt);
    }
}
