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
 * Nikolai Kardashev Model Variant B11.1 (Pure) & B11.2 (Hybrid).
 * Kardashev Scale K = (log10(P) - 6) / 10 where P is total power in Watts.
 * Type I = 10^16 W, Type II = 10^26 W, Type III = 10^36 W.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class KardashevPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(KardashevPureEngine.class);

    public static double calculateKardashevScale(double totalPowerWatts) {
        if (totalPowerWatts <= 1e6) return 0.0;
        return (Math.log10(totalPowerWatts) - 6.0) / 10.0;
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double totalGlobalWatts = 0.0;
        for (H3Cell cell : cells) {
            if (cell != null && cell.getResourceWork() != null) {
                totalGlobalWatts += cell.getResourceWork() * 1e9; // Convert work units to Watts
            }
        }

        double kScale = calculateKardashevScale(totalGlobalWatts);
        if (kScale >= 1.0) {
            logger.info("⚡ Kardashev Engine: Civilization reached Type I Planetary Threshold (K = {})", kScale);
        }
    }
}

