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
 * Ecotoxicology & Pollution Endocrine Stress Meta-Engine.
 * Models:
 * 1. <b>Chemical & Endocrine Toxicity Stress</b>: Industrial chemical pollution accumulation (microplastics, heavy metals).
 * 2. <b>Human Fertility Reduction Law</b>: Effective biological fertility drops exponentially under toxic environmental load:
 *    Fertility_eff = Fertility_base * e^(-k * Pollution).
 *
 * @author Silvere Martin-Michiellot
 * @version 3.5.0
 */
public class EcotoxicologyFertilityEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcotoxicologyFertilityEngine.class);

    /** Endocrine toxicity decay constant k */
    public static final double ENDOCRINE_TOXICITY_DECAY_K = 0.0002;

    /**
     * Calculates effective biological fertility factor based on chemical pollution load.
     *
     * @param pollutionLevel Pollution load in cell
     * @return Fertility multiplier between 0.35 and 1.0
     */
    public static double calculateEcotoxicFertilityFactor(double pollutionLevel) {
        if (pollutionLevel <= 0.0) return 1.0;
        return Math.max(0.35, Math.exp(-ENDOCRINE_TOXICITY_DECAY_K * pollutionLevel));
    }

    /**
     * Executes one ecotoxicology fertility stress tick across cells.
     */
    public static void processEcotoxicologyFertility(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int toxicStressCells = 0;

        for (H3Cell cell : cells) {
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            if (pollution > 1000.0) {
                toxicStressCells++;
                double fertilityFactor = calculateEcotoxicFertilityFactor(pollution);

                // Reduce birth rate / demographic growth factor in polluted cells
                int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
                if (pop > 0) {
                    int suppressedBirths = (int) (pop * (1.0 - fertilityFactor) * 0.02);
                    cell.setPopulation(Math.max(0, pop - suppressedBirths));
                }
            }
        }

        if (toxicStressCells > 0) {
            logger.info("Ecotoxicology Engine: Endocrine fertility suppression active across {} highly polluted cells.", toxicStressCells);
        }
    }
}
