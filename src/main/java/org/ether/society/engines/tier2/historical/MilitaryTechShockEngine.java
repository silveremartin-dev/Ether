/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

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
public class MilitaryTechShockEngine {
    private static final Logger logger = LoggerFactory.getLogger(MilitaryTechShockEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double kPerCap = cell.getResourceCapitalOr(0.0) / pop;
            // Continuous Gaussian envelope of revolutionary military mobilization (Parker 1996)
            double shockIntensity = Math.exp(-Math.pow(kPerCap - 1500.0, 2.0) / (2.0 * 300.0 * 300.0));
            if (shockIntensity > 0.005) {
                if (cell.getResourceWork() != null) {
                    cell.setResourceWork(cell.getResourceWork() * (1.0 + 0.15 * shockIntensity * deltaYears));
                }
                double currentCap = cell.getResourceCapitalOr(0.0);
                cell.setResourceCapital(currentCap + pop * 5.0 * shockIntensity * deltaYears);
            }
        }
    }
}
