/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * W. Brian Arthur's Combinatorial Technology Evolution Engine.
 *
 * <p>Models technology as an evolving metabolic web of combinations (Arthur 2009, <i>The Nature of
 * Technology</i>). Novel techniques emerge from existing combinations, compounding into specialized
 * physical machinery, infrastructure, and tools (\(\Delta K\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ArthurCombinatorialTechnologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(ArthurCombinatorialTechnologyEngine.class);

    @Override
    public String getName() {
        return "ArthurCombinatorialTechnologyEngine";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(10.0);
            double kPerCapita = capital / pop;

            // Combinatorial capital formation scales with structural tool density and network interaction
            double rAndDIntensity = SmoothMath.smoothClamp(kPerCapita / 100.0, 0.0, 5.0, 0.1);
            double popNetworkFactor = Math.min(3.0, Math.log1p(pop) / 5.0);
            double capitalIncrement = capital * 0.002 * rAndDIntensity * popNetworkFactor * deltaYears;
            cell.setResourceCapital(capital + capitalIncrement);
        }
    }
}
