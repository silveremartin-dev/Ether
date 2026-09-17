/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Infrastructure Investment & Friction Reduction Engine.
 * Models:
 * 1. <b>Embodied Energy Infrastructure Investment</b>: Capital investment in stone paving, roads, and canals.
 * 2. <b>Friction Reduction (μ_land: 0.25 -> 0.05)</b>: Built infrastructure reduces overland mechanical transport friction.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class InfrastructureEnergyEngine {
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureEnergyEngine.class);

    /**
     * Executes one infrastructure investment and friction reduction tick across cells.
     */
    public static void processInfrastructureEnergy(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int pavedCells = 0;

        for (H3Cell cell : cells) {
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;

            // High capital & tech (Tech >= 2.5) builds paved roads & canals, lowering friction
            if (tech >= 2.5 && capital > 1000.0) {
                pavedCells++;
                double minFriction = 0.05; // Paved road / canal friction limit
                double currentFriction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.max(minFriction, currentFriction * 0.90));
            }
        }

        if (pavedCells > 0) {
            logger.info("Infrastructure Engine: Transport friction reduction active across {} paved infrastructure cells.", pavedCells);
        }
    }
}

