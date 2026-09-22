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
 * @version 1.0.0-beta.1
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
     * Executes one biological demographic mortality tick across cells with dt.
     */
    public static void processBiologicalDemographics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            // Physical carrying capacity K = food available / annual metabolic requirement (3.362 GJ)
            double carryingCapacity = Math.max(0.05, food / org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
            double stressRatio = (double) pop / carryingCapacity;

            // Famine / Nutritional stress factor
            double nutritionalStress = stressRatio > 1.0 ? Math.min(0.40, (stressRatio - 1.0) * 0.04) : 0.0;

            // Altitude Hypoxia Hazard (HAPE/AMS above 3000m, death zone above 5500m)
            double epas1 = cell.getMovementFriction() != null ? Math.clamp(1.0 - (cell.getMovementFriction() / 3.0), 0.0, 1.0) : 0.05;
            double hypoxiaHazard = 0.0;
            if (elev > 3000.0) {
                double excessElev = (elev - 3000.0) / 1000.0;
                double altitudeVulnerability = Math.max(0.1, 1.0 - epas1 * 0.85);
                hypoxiaHazard = excessElev * 0.08 * altitudeVulnerability;
            }

            // Extreme Cold Hazard (Hypothermia / Frostbite below -10°C)
            double coldHazard = tempC < -10.0 ? Math.min(0.25, (-10.0 - tempC) * 0.012) : 0.0;

            double environmentalHazardGamma = 0.01 + (pollution / 5000.0) + nutritionalStress + hypoxiaHazard + coldHazard;

            // Gompertz actuarial hazard rate for cohort mean age 30
            double hazardRate = calculateGompertzHazardRate(30.0, environmentalHazardGamma);

            double deathProb = 1.0 - Math.exp(-hazardRate * dt);
            double expectedDeaths = pop * deathProb;
            int naturalDeaths = (int) expectedDeaths;
            double fractionalDeath = expectedDeaths - naturalDeaths;
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < fractionalDeath) {
                naturalDeaths++;
            }
            naturalDeaths = Math.min(pop, naturalDeaths);

            int finalPop = Math.max(0, pop - naturalDeaths);
            cell.setPopulation(finalPop);
            if (finalPop > 0) {
                cell.updateAgePyramidFromTotal(cell.getTechnologyLevel() > 0 ? cell.getTechnologyLevel() : 1.0);
            }
        }
    }

    public static void processBiologicalDemographics(List<H3Cell> cells) {
        processBiologicalDemographics(cells, 30.0 / 365.25);
    }
}

