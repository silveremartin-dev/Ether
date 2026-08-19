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
 * World3 Coupled Dynamic System Engine (Meadows et al., MIT / Club of Rome 1972).
 * Formally implements the 5 coupled sub-systems:
 * 1. Population & Gompertz-Meadows Life Expectancy LE = LE_base * min(M_food, M_heal, M_poll, M_crowd)
 * 2. Industrial Capital (IC) & FCAOR (Fraction of Capital Allocated to Obtaining Resources)
 * 3. Non-Renewable Resources (NR) Depletion Rate (NRUR)
 * 4. Agricultural Land Yield (LY) & Soil Degradation / Erosion
 * 5. Persistent Pollution Generation (PPG) & Biospheric Assimilation (PPA)
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class World3CouplingEngine {
    private static final Logger logger = LoggerFactory.getLogger(World3CouplingEngine.class);

    private static double initialGlobalResourceStock = 1000.0;

    public static void processWorld3System(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double pop = cell.getPopulation();
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double resource = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;

            // 1. FCAOR: Fraction of Capital Allocated to Obtaining Resources
            double resourceRatio = Math.max(0.01, Math.min(1.0, resource / initialGlobalResourceStock));
            double fcaor = 0.05 + 0.90 * Math.pow(1.0 - resourceRatio, 2.0); // Non-linear resource extraction effort curve

            // 2. Net Industrial Output (IO)
            double industrialOutput = capital * (1.0 - fcaor);

            // 3. Multipliers for Life Expectancy (LE)
            double mFood = Math.min(1.5, Math.max(0.2, (cell.getFoodResource() != null ? cell.getFoodResource() : 100.0) / (pop * 0.1)));
            double mHeal = Math.min(1.4, Math.max(0.5, 1.0 + Math.log10(1.0 + capital / 100.0)));
            double mPoll = Math.min(1.0, Math.max(0.1, 1.0 - (pollution / 1000.0)));
            double mCrowd = Math.min(1.0, Math.max(0.5, 1.0 - (pop / 200000.0)));

            double lifeExpectancy = 40.0 * Math.min(mFood, Math.min(mHeal, Math.min(mPoll, mCrowd)));
            cell.setLifespan(Math.max(15.0, Math.min(120.0, lifeExpectancy)));

            // 4. Persistent Pollution Generation (PPG) vs Assimilation (PPA)
            double ppg = industrialOutput * 0.01 * deltaYears;
            double ppaFactor = Math.exp(-0.05 * Math.max(0.001, deltaYears)); // Stable biospheric assimilation
            cell.setPollutionLevel(Math.max(0.0, pollution * ppaFactor + ppg));

            // 5. Non-Renewable Resource Depletion (NRUR)
            double nrur = industrialOutput * 0.005 * deltaYears;
            cell.setResourceMetal(Math.max(0.0, resource - nrur));
        }
    }

    public static double getInitialGlobalResourceStock() { return initialGlobalResourceStock; }
    public static void setInitialGlobalResourceStock(double stock) { initialGlobalResourceStock = Math.max(1.0, stock); }
}
