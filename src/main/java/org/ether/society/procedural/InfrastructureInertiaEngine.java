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
 * Physical Infrastructure Inertia & Turnover Engine (Jancovici Capital Lock-in).
 * Models physical lifespan of industrial assets (power grids, buildings, heavy machinery)
 * and transition latency (tau_transition).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class InfrastructureInertiaEngine {
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureInertiaEngine.class);

    private static double capitalTurnoverHalfLifeYears = 35.0; // 35 years physical turnover constraint

    public static void processInfrastructureInertia(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double decayFactor = Math.exp(-deltaYears / capitalTurnoverHalfLifeYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            // Physical depreciation of industrial capital
            cell.setResourceCapital(capital * decayFactor);
        }
    }

    public static double getCapitalTurnoverHalfLifeYears() { return capitalTurnoverHalfLifeYears; }
    public static void setCapitalTurnoverHalfLifeYears(double years) { capitalTurnoverHalfLifeYears = Math.max(1.0, years); }
}
