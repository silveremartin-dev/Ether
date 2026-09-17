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
 * Net Energy Return on Investment (EROEI) Master Cliodynamic Engine.
 * EROEI = Energy Extracted / Energy Expended to Extract.
 * Models the fundamental macro-variable driving civilizational growth, complexity, and collapse:
 * 1. <b>High EROEI (> 10:1)</b>: Enables urban specialization, scientific research, and complex institutions.
 * 2. <b>Low EROEI (1:1 to 3:1)</b>: Agrarian subsistence trap (90% of population must farm).
 * 3. <b>Negative EROEI (< 1:1)</b>: Immediate societal collapse, famine, and fragmentation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class NetEnergyEROEIEngine {
    private static final Logger logger = LoggerFactory.getLogger(NetEnergyEROEIEngine.class);

    /**
     * Calculates Net Energy Return on Investment (EROEI) for a cell.
     *
     * @param cell H3 terrain cell
     * @return EROEI ratio (e.g. 1.1 to 30.0)
     */
    public static double calculateEROEI(H3Cell cell) {
        if (cell == null) return 1.1;

        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
        double woodBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
        double metalOre = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

        // Baseline energy extraction ratio
        double energyExtracted = (woodBiomass * 18.5) + (metalOre * 10.0) + (tech * 50.0);
        double energyExpended = Math.max(10.0, 100.0 / Math.max(0.1, tech + 1.0));

        return Math.max(0.5, energyExtracted / energyExpended);
    }

    /**
     * Executes one master EROEI cliodynamic tick across cells.
     */
    public static void processNetEnergyEROEI(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int collapseCells = 0;

        for (H3Cell cell : cells) {
            double eroei = calculateEROEI(cell);
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            // EROEI < 1.0 triggers societal energy collapse
            if (eroei < 1.0 && pop > 100) {
                collapseCells++;
                int famineDeaths = (int) (pop * 0.10);
                cell.setPopulation(Math.max(0, pop - famineDeaths));
            }
        }

        if (collapseCells > 0) {
            logger.warn("EROEI Engine: Civilizational energy deficit collapse active across {} cells.", collapseCells);
        }
    }
}

