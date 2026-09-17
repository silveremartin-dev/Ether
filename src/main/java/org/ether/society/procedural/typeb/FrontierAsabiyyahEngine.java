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
 * Peter Turchin Frontier Asabiyyah Model Variant B29.1 (Pure) & B29.2 (Hybrid).
 * Turchin Law (Historical Dynamics): Collective solidarity (Asabiyyah) is forged at hostile meta-ethnic frontiers
 * and decays in rich imperial hinterland centers due to Pareto inequality and luxury.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class FrontierAsabiyyahEngine {
    private static final Logger logger = LoggerFactory.getLogger(FrontierAsabiyyahEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceCapital() == null) continue;

            double capital = cell.getResourceCapital();
            // High capital hinterland center: Asabiyyah decays by 2% per tick due to luxury
            if (capital > 1000.0) {
                cell.setResourceWork(Math.max(10.0, (cell.getResourceWork() != null ? cell.getResourceWork() : 50.0) * 0.98));
            } else {
                // Frontier cell: Asabiyyah (military readiness) increases
                cell.setResourceWork((cell.getResourceWork() != null ? cell.getResourceWork() : 50.0) * 1.02);
            }
        }
    }
}

