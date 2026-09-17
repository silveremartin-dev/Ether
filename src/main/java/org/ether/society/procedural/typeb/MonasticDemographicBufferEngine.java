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
 * Celibate Monastic Demographic Buffer Engine Variant B23.1 (Pure) & B23.2 (Hybrid).
 * Diverts 2% to 10% of adult population into celibate monastic orders (e.g. Medieval Europe 2-4%, Tibet 10%),
 * absorbing Malthusian demographic overpressure without triggering war.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MonasticDemographicBufferEngine {
    private static final Logger logger = LoggerFactory.getLogger(MonasticDemographicBufferEngine.class);

    private static double monasticFraction = 0.04; // 4% default monastic buffer fraction

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double pop = cell.getPopulation();

            // When food per capita drops below threshold, monastic vocation increases to buffer fertility
            if ((food / pop) < 0.10) {
                double celibateCount = pop * monasticFraction;
                // Reduce net birth rate by dampening fertile population fraction
                cell.setPopulation((int) (pop - celibateCount * 0.02 * deltaYears));
            }
        }
    }

    public static double getMonasticFraction() { return monasticFraction; }
    public static void setMonasticFraction(double fraction) { monasticFraction = Math.max(0.0, Math.min(0.20, fraction)); }
}

