/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecological Degradation & Malthusian Feedback Engine.
 * Models:
 * 1. Soil Organic Carbon (SOC) depletion & soil erosion from over-farming.
 * 2. Deforestation & natural biomass depletion due to wood/fuel extraction.
 * 3. Malthusian population crashes & forced migration when carrying capacity is exceeded.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.4.0
 */
public class EcologicalDegradationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcologicalDegradationEngine.class);

    /**
     * Executes one ecological tick across all land cells.
     */
    public static void processEcologicalDegradation(List<H3Cell> cells, double techLevel) {
        if (cells == null || cells.isEmpty()) return;

        int malthusianEvents = 0;

        for (H3Cell cell : cells) {
            if (cell.getElevation() <= 0 || cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                continue;
            }

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) {
                // Soil & vegetation slow natural recovery when unoccupied
                cell.setSoilOrganicCarbon(Math.min(100.0, cell.getSoilOrganicCarbon() + 0.5));
                cell.setBiomassNatural(Math.min(1000.0, cell.getBiomassNatural() + 2.0));
                continue;
            }

            // 1. Calculate Ecological Carrying Capacity based on soil carbon, rainfall, and tech
            double baseCap = calculateCarryingCapacity(cell, techLevel);

            // 2. Over-farming soil carbon depletion
            if (pop > baseCap * 0.7) {
                double overUseRatio = (pop - baseCap * 0.7) / baseCap;
                double socDepletion = 0.5 * overUseRatio;
                cell.setSoilOrganicCarbon(Math.max(5.0, cell.getSoilOrganicCarbon() - socDepletion));
            }

            // 3. Deforestation for firewood & construction
            double woodDemand = pop * 0.1;
            if (cell.getBiomassNatural() > woodDemand) {
                cell.setBiomassNatural(cell.getBiomassNatural() - woodDemand);
            } else {
                cell.setBiomassNatural(0.0);
            }

            // 4. Malthusian Feedback Loop: Pop exceeds Carrying Capacity
            if (pop > baseCap * 1.3) {
                malthusianEvents++;
                int excessPop = (int) (pop - baseCap);
                
                // 15% mortality rate from starvation/hardship
                int mortality = (int) (excessPop * 0.15);
                cell.setPopulation(Math.max(0, pop - mortality));

                // Re-evaluate age pyramid after mortality
                cell.updateAgePyramidFromTotal(cell.getTechnologyLevel() > 0 ? cell.getTechnologyLevel() : techLevel);
            }
        }

        if (malthusianEvents > 0) {
            logger.info("Ecological Malthusian feedback loop triggered across {} over-populated cells.", malthusianEvents);
        }
    }

    /**
     * Calculates maximum sustainable population carrying capacity for an H3 cell.
     */
    public static double calculateCarryingCapacity(H3Cell cell, double techLevel) {
        if (cell == null) return 100.0;

        double soilFactor = cell.getSoilOrganicCarbon() > 0 ? (cell.getSoilOrganicCarbon() / 50.0) : 1.0;
        double rainFactor = Math.min(2.0, Math.max(0.1, cell.getRainfall() / 500.0));
        double waterFactor = cell.getWaterResource() > 0 ? 1.2 : 0.8;

        // Base capacity per km²
        double baseCap = 50.0 * soilFactor * rainFactor * waterFactor;

        // Technology multiplication (Agriculture, Irrigation, Industrial Tech)
        double techMultiplier = 1.0 + (techLevel * 0.8) + (techLevel >= 5.0 ? Math.pow(techLevel - 4.0, 1.5) : 0.0);

        return Math.max(10.0, baseCap * techMultiplier);
    }
}
