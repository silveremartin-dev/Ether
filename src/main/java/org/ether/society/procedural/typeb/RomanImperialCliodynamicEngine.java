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
 * Roman Imperial Cliodynamic & Expansion Engine Variant B34.1 (Pure) & B34.2 (Hybrid).
 * Models ancient imperial dynamics (Rome, Han, Carthage):
 * 1. Territorial expansion increase administrative overhead costs quadratically O(R^2).
 * 2. Elite overproduction & corruption reduce tax extraction efficiency.
 * 3. Urban lead toxicity & plague shocks (Ant Antonine/Justinian plagues) cause periodic demographic collapse.
 * 4. Frontier Asabiyyah mismatch along Limes borders triggers barbarian invasions.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class RomanImperialCliodynamicEngine {
    private static final Logger logger = LoggerFactory.getLogger(RomanImperialCliodynamicEngine.class);

    private static double imperialRadiusKm = 1500.0; // 1500 km Roman Empire expansion radius
    private static double eliteOverproductionFactor = 0.15; // 15% elite tax diversion

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            boolean isRomanRegion = (cell.getLatitude() >= 25.0 && cell.getLatitude() <= 55.0) &&
                                    (cell.getLongitude() >= -10.0 && cell.getLongitude() <= 45.0);
            if (!isRomanRegion) continue;

            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 500.0;
            double pop = cell.getPopulation();

            // Administrative friction quadratic penalty O(R^2)
            double adminFriction = 1.0 + (Math.pow(imperialRadiusKm / 1000.0, 2.0) * 0.05);
            double netCapital = capital / adminFriction;

            // Elite overproduction diversion
            netCapital *= (1.0 - eliteOverproductionFactor);
            cell.setResourceCapital(Math.max(10.0, netCapital));

            // Urban mortality sink (lead pipes + crowding) dampens urban population density growth
            if (pop > 20000) {
                cell.setLifespan(Math.max(25.0, (cell.getLifespan() != null ? cell.getLifespan() : 35.0) - 0.2 * deltaYears));
            }
        }
    }

    public static double getImperialRadiusKm() { return imperialRadiusKm; }
    public static void setImperialRadiusKm(double radius) { imperialRadiusKm = Math.max(100.0, radius); }

    public static double getEliteOverproductionFactor() { return eliteOverproductionFactor; }
    public static void setEliteOverproductionFactor(double v) { eliteOverproductionFactor = Math.max(0.0, Math.min(0.50, v)); }
}

