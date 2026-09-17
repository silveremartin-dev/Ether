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
 * Sakoku Edo Japan Isolationism & Forest Conservation Engine Variant B35.1 (Pure) & B35.2 (Hybrid).
 * Models Edo Period Japan (1603-1868):
 * 1. Sakoku isolationism prevents external trade contagion and resource drain.
 * 2. Tokugawa forest conservation & organic recycling establish a zero-growth ecological equilibrium.
 * 3. High population density sustained at steady state (~30 million) with high social stability.
 * 4. Ended by external military tech shock (Perry Black Ships 1853).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EdoJapanIsolationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EdoJapanIsolationEngine.class);

    private static boolean sakokuIsolationActive = true;
    private static double sustainableEquilibriumCap = 30.0; // 30M population equilibrium cap

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            // Apply Sakoku isolation only to Japan geographic region
            boolean isJapanRegion = (cell.getLatitude() >= 30.0 && cell.getLatitude() <= 45.0) &&
                                    (cell.getLongitude() >= 128.0 && cell.getLongitude() <= 146.0);

            if (sakokuIsolationActive && isJapanRegion) {
                // Forest conservation prevents soil erosion & degradation
                cell.setSoilOrganicCarbon(Math.max(15.0, cell.getSoilOrganicCarbon() != null ? cell.getSoilOrganicCarbon() : 15.0));

                // Stabilizes population at ecological equilibrium carrying capacity
                double pop = cell.getPopulation();
                double targetPop = sustainableEquilibriumCap * 1000.0; // Scaled per cell
                if (pop > targetPop) {
                    cell.setPopulation((int) (pop - (pop - targetPop) * 0.05 * deltaYears));
                } else if (pop < targetPop) {
                    cell.setPopulation((int) (pop + (targetPop - pop) * 0.02 * deltaYears));
                }

                // Zero pollution accumulation due to organic waste recycling
                cell.setPollutionLevel(Math.max(0.0, (cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0) - 1.0 * deltaYears));
            }
        }
    }

    public static boolean isSakokuIsolationActive() { return sakokuIsolationActive; }
    public static void setSakokuIsolationActive(boolean active) { sakokuIsolationActive = active; }

    public static double getSustainableEquilibriumCap() { return sustainableEquilibriumCap; }
    public static void setSustainableEquilibriumCap(double cap) { sustainableEquilibriumCap = Math.max(1.0, cap); }
}

