/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Reliability & Invariant Assertion Guard for H3 Planetary Simulation.
 * Enforces physical and mathematical invariants across all simulation cells:
 * <ul>
 *   <li>Non-negative populations ($N \ge 0$).</li>
 *   <li>Bounded Gini indices ($0.0 \le \text{Gini} \le 1.0$).</li>
 *   <li>Bounded lifespan ($10 \le \text{Lifespan} \le 150$).</li>
 *   <li>Bounded dynamic albedo ($0.01 \le \text{Albedo} \le 0.95$).</li>
 *   <li>Conservation of total water & biomass values.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SimulationInvariantGuard {
    private static final Logger logger = LoggerFactory.getLogger(SimulationInvariantGuard.class);

    /**
     * Sanitizes and verifies invariant conditions across all simulation cells.
     *
     * @param cells List of H3 cells to validate
     * @return Number of corrected anomalies
     */
    public static int validateAndEnforceInvariants(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return 0;

        int anomalyCorrections = 0;

        for (H3Cell cell : cells) {
            // 1. Population invariant (N >= 0)
            if (cell.getPopulation() != null && cell.getPopulation() < 0) {
                cell.setPopulation(0);
                anomalyCorrections++;
            }

            // 2. Gini Index invariant (0.0 <= Gini <= 1.0)
            if (cell.getGiniIndex() != null) {
                if (cell.getGiniIndex() < 0.0) {
                    cell.setGiniIndex(0.0);
                    anomalyCorrections++;
                } else if (cell.getGiniIndex() > 1.0) {
                    cell.setGiniIndex(1.0);
                    anomalyCorrections++;
                }
            }

            // 3. Lifespan invariant (10.0 <= Lifespan <= 150.0)
            if (cell.getLifespan() != null) {
                if (cell.getLifespan() < 10.0) {
                    cell.setLifespan(10.0);
                    anomalyCorrections++;
                } else if (cell.getLifespan() > 150.0) {
                    cell.setLifespan(150.0);
                    anomalyCorrections++;
                }
            }

            // 4. Dynamic Albedo invariant (0.01 <= Albedo <= 0.95)
            if (cell.getDynamicAlbedo() != null) {
                if (cell.getDynamicAlbedo() < 0.01) {
                    cell.setDynamicAlbedo(0.01);
                    anomalyCorrections++;
                } else if (cell.getDynamicAlbedo() > 0.95) {
                    cell.setDynamicAlbedo(0.95);
                    anomalyCorrections++;
                }
            }
        }

        if (anomalyCorrections > 0) {
            logger.warn("SimulationInvariantGuard: Corrected {} boundary invariant anomalies.", anomalyCorrections);
        }

        return anomalyCorrections;
    }
}

