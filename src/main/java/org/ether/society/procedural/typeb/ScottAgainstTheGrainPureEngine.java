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
 * James C. Scott Model Variant B17.1 (Pure) & B17.2 (Hybrid).
 * Scott Law (Against the Grain): Early grain-based agrarian states suffer initial drops in health,
 * higher epidemic vulnerability, and tax coercion friction compared to mobile foraging populations.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ScottAgainstTheGrainPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(ScottAgainstTheGrainPureEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;

            double tech = cell.getTechnologyLevel();
            // Early agrarian state transition window (Tech 1.5 to 2.5)
            if (tech >= 1.5 && tech <= 2.5) {
                // Scott penalty: initial health drop from grain monoculture and crowd disease
                if (cell.getLifespan() != null) {
                    cell.setLifespan(Math.max(22.0, cell.getLifespan() - 0.5 * deltaYears));
                }
                // Epidemic vulnerability increases in early grain towns
                if (cell.getPollutionLevel() != null) {
                    cell.setPollutionLevel(cell.getPollutionLevel() + 0.2 * deltaYears);
                }
            }
        }
    }
}

