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
 * David Buss Evolutionary Psychology Mating Model Variant B30.1 (Pure) & B30.2 (Hybrid).
 * Buss Law (Evolutionary Psychology): Asymmetric resource concentration leads to elite polygyny,
 * creating a surplus of unmated young males who are mobilized into high-risk military/colonial expansion.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SexualSelectionMatingEngine {
    private static final Logger logger = LoggerFactory.getLogger(SexualSelectionMatingEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceCapital() == null) continue;

            double capital = cell.getResourceCapital();
            // High capital inequality creates surplus unmated male military mobilization
            if (capital > 500.0) {
                double work = cell.getResourceWork() != null ? cell.getResourceWork() : 50.0;
                cell.setResourceWork(work * (1.0 + 0.05 * deltaYears)); // Increased military mobilization
            }
        }
    }
}

