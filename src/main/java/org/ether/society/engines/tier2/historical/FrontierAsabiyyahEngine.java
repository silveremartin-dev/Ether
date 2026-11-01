/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Peter Turchin Frontier Asabiyyah Model Variant B29.1 (Pure) &amp; B29.2 (Hybrid).
 * Turchin Law (Historical Dynamics): Collective solidarity (Asabiyyah) is forged at hostile meta-ethnic frontiers
 * and decays in rich imperial hinterland centers due to Pareto inequality and luxury.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class FrontierAsabiyyahEngine {
    private static final Logger logger = LoggerFactory.getLogger(FrontierAsabiyyahEngine.class);

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
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceCapital() == null) continue;

            double capital = cell.getResourceCapital();
            // Continuous Asabiyyah dynamics (Turchin 2003 "Historical Dynamics"):
            // Affluence decays solidarity via luxury, while rugged/frontier conditions build readiness
            double relAffluence = capital / 1000.0;
            double decayRate = 0.04 * (relAffluence / (1.0 + relAffluence));
            double growthRate = 0.04 / (1.0 + relAffluence);
            double netRate = (growthRate - decayRate) * deltaYears;

            double work = cell.getResourceWorkOr(50.0);
            cell.setResourceWork(Math.max(10.0, work * (1.0 + netRate)));
        }
    }
}
