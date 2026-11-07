/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Leslie White Energy &amp; Cultural Evolution Engine.
 *
 * <p>Formalizes Leslie White's Law (1949, <i>The Science of Culture</i>): cultural evolution and
 * civilizational complexity scale directly with the per-capita energy captured and the physical
 * efficiency of structural capital tools (\(C = E \cdot T\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LeslieWhitePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(LeslieWhitePureEngine.class);

    public static double calculateCulturalComplexity(double energyPerCapita, double toolEfficiencyFactor) {
        return energyPerCapita * toolEfficiencyFactor;
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            int pop = cell.getPopulation();
            double foodEnergy = cell.getEnergyFoodConsumedOr(3.5 * pop);
            double exosomaticEnergy = cell.getEnergyFireOr(0.0)
                    + cell.getEnergyWindOr(0.0)
                    + cell.getEnergySolarOr(0.0)
                    + (cell.getBiomassLivestockOr(0.0) * 15.0)
                    + (cell.getResourceCoalOr(0.0) * 24.0)
                    + (cell.getResourceOilOr(0.0) * 42.0);
            double totalEnergy = foodEnergy + exosomaticEnergy;
            double energyPerCapita = totalEnergy / pop;

            // Exosomatic energy capture enables physical capital deepening (tools, machinery, infrastructure)
            double capitalIncrement = pop * (energyPerCapita / 50.0) * 0.005 * deltaYears;
            cell.setResourceCapital(cell.getResourceCapitalOr(0.0) + capitalIncrement);
        }
    }
}
