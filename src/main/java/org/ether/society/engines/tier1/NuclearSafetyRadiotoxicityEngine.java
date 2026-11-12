/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nuclear Safety, Fission Meltdown & D-T Fusion Energy Engine.
 * Models:
 * 1. <b>Fission Core Meltdown & Radiotoxicity</b>: Plant accidents (e.g. Chernobyl/Fukushima) create radiotoxic exclusion zones (Bq/m^2) and increase Gompertz cancer mortality.
 * 2. <b>D-T Nuclear Fusion Transition</b>: Ultra-high EROEI (>= 40:1) clean energy transition with zero long-term radiotoxicity.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NuclearSafetyRadiotoxicityEngine extends AbstractSimulationEngine {
    public NuclearSafetyRadiotoxicityEngine() {
        super("NuclearSafetyRadiotoxicityEngine", "NuclearSafetyRadiotoxicityEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

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
    public static void processNuclearEnergySafety(WorldBuffer buffer) {
        processNuclearEnergySafety(buffer, 0L, 1.0 / 12.0);
    }

    /**
     * Executes nuclear safety, radiotoxicity contamination, and fusion transition tick with explicit simulation tick.
     */
    public static void processNuclearEnergySafety(WorldBuffer buffer, long tick) {
        processNuclearEnergySafety(buffer, tick, 1.0 / 12.0);
    }

    /**
     * Executes nuclear safety, radiotoxicity contamination, and fusion transition tick with explicit tick and time delta.
     *
     * @param buffer WorldBuffer DoD state
     * @param tick simulation tick index
     * @param deltaYears time step in years
     */
    public static void processNuclearEnergySafety(WorldBuffer buffer, long tick, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        int meltdownEvents = 0;
        double eventThreshold = (accidentProbability >= 1.0)
                ? 1.0
                : (1.0 - Math.exp(-accidentProbability * Math.max(0.01, deltaYears)));

        float[] bioHuman = buffer.getBiomassHuman();
        float[] uraniumArr = buffer.getResourceUranium();
        float[] pollutionArr = buffer.getPollutionLevel();
        long[] h3Arr = buffer.getH3Indexes();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 0.0) continue;

            double uranium = uraniumArr[i];
            if (uranium <= 0.0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(buffer, i);
            double pollution = pollutionArr[i];

            if (powerPerCapita >= FISSION_POWER_THRESHOLD && powerPerCapita < FUSION_POWER_THRESHOLD) {
                long cellId = (h3Arr != null && h3Arr[i] != 0L) ? h3Arr[i] : i;
                double prng = org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, tick, cellId, 107L);
                if (accidentProbability >= 1.0 || prng < eventThreshold) {
                    meltdownEvents++;
                    pollutionArr[i] = (float) (pollution + 10000.0);
                    bioHuman[i] = (float) (bioHuman[i] * 0.70);
                }
            }
        }

        if (meltdownEvents > 0) {
            logger.warn("Nuclear Engine: Fission core meltdown and radiotoxic exclusion zone active across {} cells.", meltdownEvents);
        }
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processNuclearEnergySafety(buf, 0L, dtYears);
    }
}
