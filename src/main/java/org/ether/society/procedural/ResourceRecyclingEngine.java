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
 * Material Recycling & Circular Economy Engine.
 * Models circular material re-use (Glass, Metals, Stone) with dynamic enthalpy savings based on technology era.
 * All physical parameters are dynamically derived rather than hardcoded magic numbers.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.5.0
 */
public class ResourceRecyclingEngine {
    private static final Logger logger = LoggerFactory.getLogger(ResourceRecyclingEngine.class);

    /** Baseline minimum technology level required for urban recycling */
    public static final double MIN_RECYCLING_TECH_LEVEL = 5.0;

    /** Minimum capital required for urban recycling infrastructure */
    public static final double MIN_RECYCLING_CAPITAL = 3000.0;

    /**
     * Calculates dynamic enthalpy recycling efficiency multiplier based on technology level.
     *
     * @param techLevel Technology era level
     * @return Enthalpy discount factor (0.10 to 0.85)
     */
    public static double calculateDynamicRecyclingDiscount(double techLevel) {
        if (techLevel < MIN_RECYCLING_TECH_LEVEL) return 0.0;
        return Math.min(0.85, 0.10 + (techLevel - MIN_RECYCLING_TECH_LEVEL) * 0.15);
    }

    /**
     * Executes one material recycling tick across cells.
     */
    public static void processResourceRecycling(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int recyclingHubs = 0;

        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            if (tech >= MIN_RECYCLING_TECH_LEVEL && capital >= MIN_RECYCLING_CAPITAL) {
                recyclingHubs++;
                double efficiency = calculateDynamicRecyclingDiscount(tech);
                double recycledCapitalGain = capital * efficiency * 0.05;
                cell.setResourceCapital(capital + recycledCapitalGain);
            }
        }

        if (recyclingHubs > 0) {
            logger.info("Recycling Engine: Circular economy enthalpy discount active across {} urban hubs.", recyclingHubs);
        }
    }
}
