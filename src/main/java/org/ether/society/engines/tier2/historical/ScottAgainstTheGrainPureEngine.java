/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
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
 * James C. Scott Model Variant B17.1 (Pure) &amp; B17.2 (Hybrid).
 * Scott Law (Against the Grain): Early grain-based agrarian states suffer initial drops in health,
 * higher epidemic vulnerability, and tax coercion friction compared to mobile foraging populations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScottAgainstTheGrainPureEngine extends AbstractSimulationEngine {
    public ScottAgainstTheGrainPureEngine() {
        super("ScottAgainstTheGrainPureEngine", "ScottAgainstTheGrainPureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    private static final Logger logger = LoggerFactory.getLogger(ScottAgainstTheGrainPureEngine.class);

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        // grounded strictly in conserved physical observables (AGENTS.md §10.1, §10.2).
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double kPerCapita = cell.getResourceCapitalOr(0.0) / pop;
            // Continuous Gaussian early-agrarian health & epidemic vulnerability envelope (Scott 2017 "Against the Grain")
            // Peak vulnerability centered at K/N = 150 kg/capita (early sedentary grain state) with standard deviation 35.0
            double scottVulnerability = Math.exp(-Math.pow(kPerCapita - 150.0, 2.0) / (2.0 * 35.0 * 35.0));

            if (scottVulnerability > 0.01) {
                // Scott penalty: initial health drop from grain monoculture and crowd disease
                if (cell.getLifespan() != null) {
                    cell.setLifespan(Math.max(22.0, cell.getLifespan() - 0.5 * scottVulnerability * deltaYears));
                }
                // Epidemic vulnerability increases in early grain towns
                if (cell.getPollutionLevel() != null) {
                    cell.setPollutionLevel(cell.getPollutionLevel() + 0.2 * scottVulnerability * deltaYears);
                }
            }
        }
    }
}
