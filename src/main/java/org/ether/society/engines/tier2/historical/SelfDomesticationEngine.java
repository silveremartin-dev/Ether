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
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Human Self-Domestication &amp; Prosociality Engine.
 *
 * <p>Formalizes Wrangham (2019, <i>The Goodness Paradox</i>) and Hare (2017): selection against
 * reactive aggression in dense social networks expands cooperative capacity, communal pooling of
 * physical labor, and shared capital formation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SelfDomesticationEngine extends AbstractSimulationEngine {
    public SelfDomesticationEngine() {
        super("SelfDomesticationEngine", "SelfDomesticationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    private static final Logger logger = LoggerFactory.getLogger(SelfDomesticationEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            int pop = cell.getPopulation();
            double popDensity = pop / 1000.0;
            double cooperationFactor = SmoothMath.sigmoid(popDensity, 1.0, 0.3);

            // Boosts cooperative collective labor availability
            double currentWork = cell.getResourceWorkOr(0.0);
            cell.setResourceWork(currentWork + pop * 0.05 * cooperationFactor * Math.log1p(popDensity) * deltaYears);
        }
    }
}
