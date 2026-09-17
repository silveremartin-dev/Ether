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
 * Urban Thermodynamics & Infrastructure Grid Engine.
 * Models urban heat island effect (T_uhi), high-voltage grid transmission losses (I^2 * R Joule heating),
 * and localized aquifer drawdowns in dense population centers.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class UrbanThermodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(UrbanThermodynamicsEngine.class);

    public static void processUrbanThermodynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() < 5000) continue;

            long pop = cell.getPopulation();
            // Urban Heat Island (T_uhi) formula: deltaT = 0.5 * log10(pop / 1000)
            double uhiDeltaT = 0.5 * Math.log10(pop / 1000.0);
            double currentTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            // Apply UHI bump to cell local temperature
            cell.setTemperature(currentTemp + uhiDeltaT * 0.05 * deltaYears);

            // High population density accelerates localized aquifer depletion
            if (pop > 50000) {
                double aquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 0.0;
                cell.setAccessibleAquifer(Math.max(0.0, aquifer - (pop * 0.001) * deltaYears));
            }
        }
    }
}

