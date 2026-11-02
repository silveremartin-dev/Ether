/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

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
 * @version 1.0.0-beta.2
 */
public class EcotoxicologyFertilityEngine {
    private static final Logger logger = LoggerFactory.getLogger(EcotoxicologyFertilityEngine.class);

    /** Endocrine toxicity decay constant k */
    public static final double ENDOCRINE_TOXICITY_DECAY_K = 0.0002;

    /** Baseline human crude birth rate (annual, births per capita) */
    public static final double BASELINE_CRUDE_BIRTH_RATE = 0.035;

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
     * Executes one ecotoxicology fertility stress tick across cells (defaults to monthly interval).
     */
    public static void processEcotoxicologyFertility(List<H3Cell> cells) {
        processEcotoxicologyFertility(cells, 1.0 / 12.0);
    }

    /**
     * Executes ecotoxicology fertility stress tick across cells with explicit time integration.
     *
     * @param cells spatial cells
     * @param deltaYears time step in years
     */
    public static void processEcotoxicologyFertility(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        int toxicStressCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double pollution = cell.getPollutionLevelOr(0.0);
            if (pollution > 100.0) {
                double fertilityFactor = calculateEcotoxicFertilityFactor(pollution);
                if (fertilityFactor < 0.999) {
                    toxicStressCells++;
                    int pop = cell.getPopulationOr(0);
                    if (pop > 0) {
                        // Endocrine disruption suppresses births proportionally to baseline birth rate and deltaYears
                        int suppressedBirths = (int) Math.round(pop * BASELINE_CRUDE_BIRTH_RATE * (1.0 - fertilityFactor) * deltaYears);
                        int targetPop = Math.max(0, pop - suppressedBirths);
                        org.ether.society.database.H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, targetPop);
                    }
                }
            }
        }

        if (toxicStressCells > 0) {
            logger.info("Ecotoxicology Engine: Endocrine fertility suppression active across {} highly polluted cells.", toxicStressCells);
        }
    }
}
