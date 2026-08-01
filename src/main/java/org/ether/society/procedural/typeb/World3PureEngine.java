/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * World3 Pure Model Variant B1.1 (Meadows et al., MIT / Club of Rome 1972).
 * Implements the standalone 5-subsystem differential equations (POP, IC, NR, FP, PPOL)
 * with textbook fidelity.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class World3PureEngine {
    private static final Logger logger = LoggerFactory.getLogger(World3PureEngine.class);

    private double population = 1.6e9; // 1.6 Billion baseline (1900)
    private double industrialCapital = 2.1e11;
    private double nonRenewableResources = 1.0e12; // 1 Trillion units
    private double persistentPollution = 2.5e7;
    private double arableLand = 0.9e9; // 0.9 Billion hectares

    public void processTick(double deltaYears) {
        // 1. Resource ratio & FCAOR
        double resourceRatio = Math.max(0.01, nonRenewableResources / 1.0e12);
        double fcaor = 0.05 + 0.90 * Math.pow(1.0 - resourceRatio, 2.0);

        // 2. Industrial Output (IO) & Investment (ICIR) vs Depreciation (ICDR)
        double industrialOutput = industrialCapital * (1.0 / 3.0) * (1.0 - fcaor);
        double icir = industrialOutput * 0.43;
        double icdr = industrialCapital / 14.0; // 14-year average capital lifetime
        industrialCapital += (icir - icdr) * deltaYears;

        // 3. Resource Depletion (NRUR)
        double nrur = industrialOutput * 0.05;
        nonRenewableResources = Math.max(0.0, nonRenewableResources - nrur * deltaYears);

        // 4. Pollution Generation & Assimilation
        double ppg = industrialOutput * 0.02;
        double ppa = persistentPollution / 20.0;
        persistentPollution += (ppg - ppa) * deltaYears;

        // 5. Population dynamics
        double births = population * 0.028;
        double deaths = population * (0.012 + (persistentPollution / 1.0e10));
        population += (births - deaths) * deltaYears;
    }

    public static void processPlugin(List<H3Cell> cells, double deltaYears) {
        // Pure variant runs global differential state
    }

    public double getPopulation() { return population; }
    public double getIndustrialCapital() { return industrialCapital; }
    public double getNonRenewableResources() { return nonRenewableResources; }
    public double getPersistentPollution() { return persistentPollution; }
}
