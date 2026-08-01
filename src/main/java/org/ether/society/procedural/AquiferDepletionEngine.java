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
 * Groundwater Table Depletion & Aquifer Pumping Limits Engine.
 * Models:
 * 1. <b>Over-Pumping vs Natural Recharge</b>: Intensive agricultural irrigation depletes deep aquifers faster than rain recharge.
 * 2. <b>Well Depth Technology Limits</b>: When water table drops below well extraction depth, agricultural irrigation halts.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.4.0
 */
public class AquiferDepletionEngine {
    private static final Logger logger = LoggerFactory.getLogger(AquiferDepletionEngine.class);

    /**
     * Executes one aquifer depletion and well pumping depth tick across cells.
     */
    public static void processAquiferDepletion(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int depletedAquifers = 0;

        for (H3Cell cell : cells) {
            double aquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 1000.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            double naturalRecharge = rain * 0.15;
            double irrigationExtraction = pop * 0.8;

            double netAquiferChange = naturalRecharge - irrigationExtraction;
            double newAquifer = Math.max(0.0, aquifer + netAquiferChange);
            cell.setAccessibleAquifer(newAquifer);

            if (newAquifer < 100.0 && pop > 300) {
                depletedAquifers++;
                // Irrigation failure degrades agricultural yield
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                cell.setBiomassAgriculture(Math.max(50.0, currentAgri * 0.70));
            }
        }

        if (depletedAquifers > 0) {
            logger.info("Aquifer Engine: Critical groundwater table depletion affecting {} agricultural cells.", depletedAquifers);
        }
    }
}
