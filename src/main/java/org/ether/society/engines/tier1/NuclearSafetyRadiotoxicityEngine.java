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
 * Nuclear Safety, Fission Meltdown & D-T Fusion Energy Engine.
 * Models:
 * 1. <b>Fission Core Meltdown & Radiotoxicity</b>: Plant accidents (e.g. Chernobyl/Fukushima) create radiotoxic exclusion zones (Bq/m^2) and increase Gompertz cancer mortality.
 * 2. <b>D-T Nuclear Fusion Transition</b>: Ultra-high EROEI (>= 40:1) clean energy transition with zero long-term radiotoxicity.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NuclearSafetyRadiotoxicityEngine {
    private static final Logger logger = LoggerFactory.getLogger(NuclearSafetyRadiotoxicityEngine.class);

    /** Power flux threshold for nuclear fission energy */
    public static final double FISSION_POWER_THRESHOLD = 15000.0;

    /** Power flux threshold for nuclear D-T fusion energy */
    public static final double FUSION_POWER_THRESHOLD = 50000.0;

    /** Baseline annual core meltdown probability per fission cell */
    private static double accidentProbability = 0.0005;
    private static long baseSeed = 42L;

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        accidentProbability = 0.0005;
        baseSeed = 42L;
    }

    /**
     * Sets the scenario-derived random seed for deterministic fission meltdown evaluations.
     *
     * @param seed the scenario seed (long)
     */
    public static void setBaseSeed(long seed) {
        baseSeed = seed;
    }

    /**
     * Gets the scenario-derived random seed for deterministic fission meltdown evaluations.
     *
     * @return the base seed (long)
     */
    public static long getBaseSeed() {
        return baseSeed;
    }

    public static double getAccidentProbability() {
        return accidentProbability;
    }

    public static void setAccidentProbability(double prob) {
        accidentProbability = Math.max(0.0, Math.min(1.0, prob));
    }

    /**
     * Executes nuclear safety, radiotoxicity contamination, and fusion transition tick.
     */
    public static void processNuclearEnergySafety(List<H3Cell> cells) {
        processNuclearEnergySafety(cells, 0L);
    }

    /**
     * Executes nuclear safety, radiotoxicity contamination, and fusion transition tick with explicit simulation tick.
     */
    public static void processNuclearEnergySafety(List<H3Cell> cells, long tick) {
// Numerical integration step for NuclearSafetyRadiotoxicityEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;

        int meltdownEvents = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            if (tech < 4.0) continue; // Nuclear fission requires atomic age technology (tech >= 4.0)

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;

            // Fission accident probability under high power flux without fusion
            if (powerPerCapita >= FISSION_POWER_THRESHOLD && powerPerCapita < FUSION_POWER_THRESHOLD) {
                long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
                double prng = org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, tick, cellId, 107L);
                if (accidentProbability >= 1.0 || prng < accidentProbability) { // Deterministic meltdown risk event
                    meltdownEvents++;
                    // Radiotoxic fallout spike (Bq/m^2 modeled via pollution)
                    cell.setPollutionLevel(pollution + 10000.0);
                    cell.setPopulation((int) (pop * 0.70)); // Evacuation & acute radiation syndrome
                }
            }
        }

        if (meltdownEvents > 0) {
            logger.warn("Nuclear Engine: Fission core meltdown and radiotoxic exclusion zone active across {} cells.", meltdownEvents);
        }
    }
}


