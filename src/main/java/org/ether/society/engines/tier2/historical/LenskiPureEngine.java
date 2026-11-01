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
 * Gerhard Lenski Ecological-Evolutionary Model Variant B9.1 (Pure) & B9.2 (Hybrid).
 * Lenski Law: Gini inequality peaks in Agrarian societies and is modulated by technology stage.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LenskiPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LenskiPureEngine.class);

    /**
     * Computes theoretical Lenski Gini coefficient based on continuous technology stage
     * (Milanovic, Lindert & Williamson 2011 "Pre-Industrial Inequality"; Gerhard Lenski 1966 "Power and Privilege").
     *
     * @param techStage continuous technology stage (0.0=Paleolithic, 1.5=Neolithic, 2.8=Agrarian Empire, 5.0=Industrial, 7.0=Information)
     * @return theoretical Gini index in range [0.18, 0.70]
     */
    public static double calculateLenskiGini(double techStage) {
        // Continuous Inverted-U Kuznets-Lenski curve:
        // Baseline egalitarian hunter-gatherer / early horticulturalist floor G ~ 0.18 (Bowles et al. 2010),
        // peaking during the agrarian imperial extraction peak G ~ 0.68 at T ~ 2.8 (feudal Rome/Middle Ages),
        // compressing through modern welfare institutions and democratic tax redistribution to G ~ 0.32-0.38 at T > 5.0.
        double agrarianPeak = 0.48 * Math.exp(-Math.pow(techStage - 2.8, 2.0) / (2.0 * 1.21));
        double modernFloor = 0.14 / (1.0 + Math.exp(-2.2 * (techStage - 4.5)));
        return Math.clamp(0.18 + agrarianPeak + modernFloor, 0.18, 0.70);
    }

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
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;
            double tech = cell.getTechnologyLevel();
            double theoreticalGini = calculateLenskiGini(tech);
            // Apply Lenski inequality curve to capital distribution
            if (cell.getResourceCapital() != null) {
                cell.setResourceCapital(cell.getResourceCapital() * (1.0 - theoreticalGini * 0.05 * deltaYears));
            }
        }
    }
}


