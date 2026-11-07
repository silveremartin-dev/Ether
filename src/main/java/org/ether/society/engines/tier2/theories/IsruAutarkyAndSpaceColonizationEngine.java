/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.HabitatType;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * In-Situ Resource Utilization (ISRU) and Autarky Engine.
 * Models sealed closed-loop off-world and high-frontier habitats.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class IsruAutarkyAndSpaceColonizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(IsruAutarkyAndSpaceColonizationEngine.class);

    public static void processSpaceColonizationCliodynamics(List<H3Cell> cells, Object ignored, double deltaYears) {
        processIsruAndAutarky(cells, deltaYears);
    }

    public static void processIsruAndAutarky(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;
        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            HabitatType habitatType = cell.getHabitatType();
            if (habitatType == HabitatType.NONE) continue;

            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double metal = cell.getResourceMetalOr(0.0);

            // 1. ISRU Extraction & Transformation Rate
            // Local mineral exploitation efficiency scales with capital intensity
            double capPerCap = capital / pop;
            double isruEfficiency = SmoothMath.smoothClamp(0.20 + (capPerCap * 0.005), 0.20, 2.50, 20.0);
            double metalYield = pop * 0.50 * isruEfficiency * dt;
            double capitalGrowth = pop * 2.0 * isruEfficiency * dt;

            cell.setResourceMetal(metal + metalYield);
            cell.setResourceCapital(capital + capitalGrowth);

            // 2. High Pioneer Social Cohesion (Asabiyyah)
            // Life under hostile dome conditions elevates collective mutual aid
            double workAvailable = cell.getResourceWorkOr(0.0);
            cell.setResourceWork(workAvailable + (pop * 1.10 * dt));
        }
    }
}
