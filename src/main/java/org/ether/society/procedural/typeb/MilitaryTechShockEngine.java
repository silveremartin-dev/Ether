/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

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
 * @version 4.0.0
 */
public class MilitaryTechShockEngine {
    private static final Logger logger = LoggerFactory.getLogger(MilitaryTechShockEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;

            double tech = cell.getTechnologyLevel();
            // Transition window of revolutionary military shock (e.g. gunpowder / arquebus introduction at Tech 3.5)
            if (tech >= 3.4 && tech <= 3.8) {
                // Accelerate technology consolidation & military work output
                if (cell.getResourceWork() != null) {
                    cell.setResourceWork(cell.getResourceWork() * (1.0 + 0.15 * deltaYears));
                }
                cell.setTechnologyLevel(tech + 0.05 * deltaYears);
            }
        }
    }
}
