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
 * Elinor Ostrom Model Variant B19.1 (Pure) & B19.2 (Hybrid).
 * Ostrom Law (Governing the Commons - Nobel Prize 2009): Polycentric community-based governance
 * prevents Tragedy of the Commons without total privatization or top-down state coercion.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class OstromCommonsPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(OstromCommonsPureEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getAccessibleAquifer() == null) continue;

            double aquifer = cell.getAccessibleAquifer();
            // Ostrom polycentric governance protects aquifers from over-extraction
            if (aquifer < 50.0) {
                cell.setAccessibleAquifer(aquifer + 0.5 * deltaYears); // Sustainable recharge buffer
            }
        }
    }
}

