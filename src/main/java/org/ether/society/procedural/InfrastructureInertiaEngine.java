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

    private static double baseCapitalTurnoverHalfLifeYears = 35.0; // Base 35-year physical turnover benchmark

    public static void processInfrastructureInertia(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            // Dynamic half-life scaling: High technology and maintenance increase asset longevity
            // Tech 1.0 (primitive): ~24 years | Tech 5.0 (industrial benchmark): 35 years | Tech 10.0+ (advanced): 45-60 years
            double dynamicHalfLife = baseCapitalTurnoverHalfLifeYears * Math.max(0.5, 0.5 + 0.5 * Math.pow(tech / 5.0, 0.6));
            double decayFactor = Math.exp(-deltaYears / dynamicHalfLife);

            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            // Physical depreciation of industrial capital per cell
            cell.setResourceCapital(capital * decayFactor);
        }
    }

    public static double getCapitalTurnoverHalfLifeYears() { return baseCapitalTurnoverHalfLifeYears; }
    public static void setCapitalTurnoverHalfLifeYears(double years) { baseCapitalTurnoverHalfLifeYears = Math.max(1.0, years); }
}
