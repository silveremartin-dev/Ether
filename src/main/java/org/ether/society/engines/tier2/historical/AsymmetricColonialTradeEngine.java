/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Asymmetric Colonial Trade & Demographic Dilution Engine Variant B25.1 (Pure) & B25.2 (Hybrid).
 * Portuguese Empire Case (16th-17th C.): Trading non-renewable bullion (silver) for renewable perishables (spices)
 * coupled with a small home population leads to colonial fragmentation, local elite trade diversion, and monopoly decay.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AsymmetricColonialTradeEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(AsymmetricColonialTradeEngine.class);

    public AsymmetricColonialTradeEngine() {
        super("AsymmetricColonialTradeEngine", "Asymmetric Colonial Trade & Bullion Drain", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getMetalResource() == null) return;
        float metal = buffer.getMetalResource()[i];
        if (metal > 0.0f) {
            double drainFactor = metal / (metal + 15.0);
            float bullionOutflow = (float) (0.5 * drainFactor * dtYears);
            float capitalGain = (float) (0.2 * drainFactor * dtYears);
            buffer.getMetalResource()[i] = Math.max(0.0f, metal - bullionOutflow);
            acc.dResourceCapital += capitalGain;
        }
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceMetal() == null) continue;

            double metal = cell.getResourceMetal(); // Bullion stock
            double capital = cell.getResourceCapitalOr(0.0);

            // Asymmetric trade bullion drain: silver exchanged for perishable spices (Flynn & GirÃ¡ldez 1995)
            // Continuous Michaelis-Menten / Hill extraction envelope replacing discrete jump at metal > 10.0
            if (metal > 0.0) {
                double drainFactor = metal / (metal + 15.0);
                double bullionOutflow = 0.5 * drainFactor * deltaYears;
                double capitalGain = 0.2 * drainFactor * deltaYears;
                cell.setResourceMetal(Math.max(0.0, metal - bullionOutflow));
                cell.setResourceCapital(capital + capitalGain);
            }
        }
    }
}


