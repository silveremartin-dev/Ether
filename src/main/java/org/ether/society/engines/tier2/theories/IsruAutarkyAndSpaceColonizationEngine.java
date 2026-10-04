/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.HabitatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Type B Cliodynamic Engine: ISRU Autarky, Earth Dependence & Space Colonization Dynamics.
 *
 * Models:
 * 1. In-Situ Resource Utilization (ISRU): Sintering local regolith, harvesting volatile ice and mineral veins
 *    to generate local physical capital K(x) and construction metals.
 * 2. Autarky vs Earth Supply-Chain Ratio: Evaluates the technological and economic transition from
 *    metropolitan dependency (Tsiolkovsky rocket logistics) to autonomous off-world civilization.
 * 3. Pioneer Social Cohesion (Asabiyyah boost): Extreme environmental hardship strengthens group solidarity
 *    and innovation rates among dome inhabitants.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class IsruAutarkyAndSpaceColonizationEngine {
    private static final Logger logger = LoggerFactory.getLogger(IsruAutarkyAndSpaceColonizationEngine.class);

    /*
     * Process space colonization cliodynamics.
     * Enforces physical invariants and updates associated state variables within {@code IsruAutarkyAndSpaceColonizationEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param preset the preset parameter (PlanetPreset)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processSpaceColonizationCliodynamics(List<H3Cell> cells, PlanetPreset preset, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            HabitatType habitatType = cell.getHabitatType();
            if (habitatType == HabitatType.NONE) continue;

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

            // 1. ISRU Extraction & Transformation Rate
            // Local mineral exploitation efficiency scales with technology level
            double isruEfficiency = Math.clamp(0.20 + (tech * 0.15), 0.20, 2.50);
            double metalYield = pop * 0.50 * isruEfficiency * dt;
            double capitalGrowth = pop * 2.0 * isruEfficiency * dt;

            cell.setResourceMetal(metal + metalYield);
            cell.setResourceCapital(capital + capitalGrowth);

            // 2. High Pioneer Social Cohesion (Asabiyyah)
            // Life under hostile dome conditions elevates collective mutual aid
            double workAvailable = cell.getResourceWork() != null ? cell.getResourceWork() : 0.0;
            cell.setResourceWork(workAvailable + (pop * 1.10 * dt));

            // 3. Technical Knowledge & Information Entropy Mitigation
            // Advanced ECLSS monitoring accelerates local technological progress
            if (tech < 8.0) {
                cell.setTechnologyLevel(tech + (0.01 * dt));
            }
        }
    }
}
