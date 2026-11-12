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
 * David Buss Evolutionary Psychology Mating Model Variant B30.1 (Pure) & B30.2 (Hybrid).
 * Buss Law (Evolutionary Psychology): Asymmetric resource concentration leads to elite polygyny,
 * creating a surplus of unmated young males who are mobilized into high-risk military/colonial expansion
 * (Buss 1989; Henrich, Boyd & Richerson 2012).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SexualSelectionMatingEngine extends AbstractSimulationEngine {
    public SexualSelectionMatingEngine() {
        super("SexualSelectionMatingEngine", "SexualSelectionMatingEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    private static final Logger logger = LoggerFactory.getLogger(SexualSelectionMatingEngine.class);

    /**
     * Process hybrid: Models evolutionary psychology mating skew and high-risk young male mobilization.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double gini = cell.getGiniIndexOr(0.35);

            // Inequality-driven young male mating skew (continuous logistic function)
            double matingSkew = 1.0 / (1.0 + Math.exp(-10.0 * (gini - 0.45)));
            if (matingSkew > 0.05) {
                double work = cell.getResourceWorkOr(50.0);
                // Mobilization boost bounded by available young adult male workforce fraction (~15% of pop)
                double youngMaleWorkforceShare = (cell.getPop15to24Or(0) > 0)
                        ? (double) cell.getPop15to24Or(0) / pop
                        : 0.15;
                double maxMobilizationFactor = 1.0 + 0.12 * matingSkew * (youngMaleWorkforceShare / 0.15);
                cell.setResourceWork(Math.min(work * maxMobilizationFactor, work * (1.0 + 0.03 * matingSkew * deltaYears)));
            }
        }
    }
}
