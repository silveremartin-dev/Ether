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
 * World3 Hybrid Model Variant B1.2 (Meadows et al. + Ether H3 Grid Integration).
 * Injects Meadows' FCAOR capital extraction sink and persistent pollution mortality multiplier
 * directly into Ether's fundamental H3 cell mass/energy grid.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class World3HybridEngine {
    private static final Logger logger = LoggerFactory.getLogger(World3HybridEngine.class);

    public static void processPlugin(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;

            // FCAOR hypothesis: capital diverted to mining increases as local mineral stock drops below 50.0
            double fcaor = 0.05 + 0.85 * Math.pow(Math.max(0.0, 1.0 - (metal / 50.0)), 2.0);
            double netCapitalOutput = capital * (1.0 - fcaor);
            cell.setResourceCapital(netCapitalOutput);

            // Pollution mortality multiplier: LE reduction when pollution > 20.0
            if (pollution > 20.0 && cell.getLifespan() != null) {
                double pollPenalty = Math.min(0.40, (pollution - 20.0) * 0.005);
                cell.setLifespan(Math.max(20.0, cell.getLifespan() * (1.0 - pollPenalty)));
            }
        }
    }
}

