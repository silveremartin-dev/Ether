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
 * Gompertz-Makeham Biological Actuarial Mortality Engine.
 * Replaces simple age cohorts with the fundamental Gompertz-Makeham actuarial mortality law:
 * μ(x) = α * e^(β * x) + γ
 * 1. <b>α * e^(β * x)</b>: Exponential biological aging rate (cellular senescence).
 * 2. <b>γ</b>: Environmental baseline hazard rate (famine, disease, trauma).
 *
 * @author Silvere Martin-Michiellot
 * @version 3.1.0
 */
public class BiologicalDemographicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(BiologicalDemographicsEngine.class);

    /** Baseline actuarial aging constant α */
    public static final double GOMPERTZ_ALPHA = 0.0001;

    /** Cellular senescence rate β */
    public static final double GOMPERTZ_BETA = 0.08;

    /**
     * Calculates Gompertz-Makeham hazard rate μ(x) for age x.
     *
     * @param ageYears Age in years
     * @param environmentalHazardGamma Baseline environmental hazard γ (pollution, disease)
     * @return Mortality hazard rate μ(x)
     */
    public static double calculateGompertzHazardRate(double ageYears, double environmentalHazardGamma) {
        return GOMPERTZ_ALPHA * Math.exp(GOMPERTZ_BETA * ageYears) + environmentalHazardGamma;
    }

    /**
     * Executes one biological demographic mortality tick across cells.
     */
    public static void processBiologicalDemographics(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 500.0;

            // Physical carrying capacity K = baseline (100) + food * 50.0
            double carryingCapacity = 100.0 + food * 50.0;
            double stressRatio = (double) pop / carryingCapacity;

            // Over-population stress factor: when pop > carrying capacity, hazard γ increases
            double nutritionalStress = stressRatio > 1.0 ? Math.min(0.08, (stressRatio - 1.0) * 0.02) : 0.0;
            double environmentalHazardGamma = 0.01 + (pollution / 5000.0) + nutritionalStress;

            // Gompertz actuarial hazard rate for cohort mean age 30
            double hazardRate = calculateGompertzHazardRate(30.0, environmentalHazardGamma);

            int naturalDeaths = (int) (pop * hazardRate);
            cell.setPopulation(Math.max(0, pop - naturalDeaths));
        }
    }
}
