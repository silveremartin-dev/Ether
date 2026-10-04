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
 * Vaclav Smil Model Variant B20.1 (Pure) & B20.2 (Hybrid).
 * Smil Law (Energy and Civilization / Materials): Decarbonization and energy transitions
 * are physically bounded by the 50-70 year turnover inertia of the 4 material pillars (Steel, Cement, Plastics, Ammonia).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SmilMaterialTransitionsPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(SmilMaterialTransitionsPureEngine.class);

    private static double transitionInertiaYears = 60.0; // 60-year average physical capital replacement time

    /*
     * Process hybrid.
     * Enforces physical invariants and updates associated state variables within {@code SmilMaterialTransitionsPureEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double decayFactor = Math.exp(-deltaYears / transitionInertiaYears);

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceCapital() == null) continue;

            double capital = cell.getResourceCapital();
            // Capital replacement constrained by 60-year physical material turnover
            cell.setResourceCapital(capital * decayFactor + capital * (1.0 - decayFactor) * 0.95);
        }
    }

    /*
     * Get transition inertia years.
     * Enforces physical invariants and updates associated state variables within {@code SmilMaterialTransitionsPureEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getTransitionInertiaYears() { return transitionInertiaYears; }
}


