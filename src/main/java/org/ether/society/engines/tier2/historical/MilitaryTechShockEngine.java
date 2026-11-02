/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Military Technology Shock & Unification Engine Variant B24.1 (Pure) & B24.2 (Hybrid).
 * Japan Tanegashima Case (1543-1600): Revolutionary lethal military technology (arquebus) introduced into a fragmented
 * feudal system accelerates territorial unification velocity (5x) and strengthens resistance/expulsion of foreign influence.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MilitaryTechShockEngine {
    private static final Logger logger = LoggerFactory.getLogger(MilitaryTechShockEngine.class);

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
            if (cell == null || cell.getTechnologyLevel() == null) continue;

            double tech = cell.getTechnologyLevel();
            // Continuous Gaussian envelope of revolutionary military shock (e.g. gunpowder / arquebus introduction around Tech 3.6)
            // Eliminates knife-edge box filter boundaries in favor of C-infinity continuity
            double shockIntensity = Math.exp(-Math.pow(tech - 3.6, 2.0) / (2.0 * 0.20 * 0.20));
            if (shockIntensity > 0.005) {
                // Accelerate technology consolidation & military work output proportionally to shock intensity
                if (cell.getResourceWork() != null) {
                    cell.setResourceWork(cell.getResourceWork() * (1.0 + 0.15 * shockIntensity * deltaYears));
                }
                cell.setTechnologyLevel(Math.min(10.0, tech + 0.05 * shockIntensity * deltaYears));
            }
        }
    }
}


