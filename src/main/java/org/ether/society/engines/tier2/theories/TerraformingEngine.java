/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural Terraforming & Orbital Resource Physics Engine.
 * Models planetary atmospheric pressure adaptation (P_atm), greenhouse gas injection,
 * orbital solar mirror insolation multipliers, Sabatier O2/CH4 conversion,
 * and off-world asteroid mining EROEI logistics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class TerraformingEngine extends AbstractSimulationEngine {
    public TerraformingEngine() {
        super("TerraformingEngine", "TerraformingEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(TerraformingEngine.class);

    // Global Terraforming State Variables
    private static boolean terraformingActive = false;
    /** Internal state: atmospheric pressure atm (double). */
    private static double atmosphericPressureAtm = 1.0;
    /** Internal state: co2ppm (double). */
    private static double co2Ppm = 280.0;
    private static double solarMirrorInsolationMultiplier = 1.0; // Multiplier from orbital mirrors
    /** Internal state: asteroid mining flux tonnes per year (double). */
    private static double asteroidMiningFluxTonnesPerYear = 0.0;
    private static double offWorldEroeiRatio = 15.0; // Asteroid mining EROEI

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        if (!terraformingActive && asteroidMiningFluxTonnesPerYear <= 0.0 && solarMirrorInsolationMultiplier == 1.0) {
            return;
        }
        int n = buf.getCapacity();
        double safeCo2 = Math.max(1.0, co2Ppm);
        double radiativeForcingWm2 = 5.35 * Math.log(safeCo2 / 280.0) + (solarMirrorInsolationMultiplier - 1.0) * 1361.0;
        double globalTempAnomaly = radiativeForcingWm2 * 0.8;
        double dtSafe = Math.max(0.0001, dtYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 5.0);

        float[] tempArr = buf.getTemperature();
        float[] elevArr = buf.getElevation();
        float[] popArr = buf.getBiomassHuman();
        float[] metalArr = buf.getMetalResource();

        for (int i = 0; i < n; i++) {
            double curTemp = tempArr[i];
            double elev = elevArr[i];
            double baselineTemp = 28.0 - 0.0065 * Math.max(0.0, elev);
            double targetTemp = baselineTemp + globalTempAnomaly;
            tempArr[i] = (float) (curTemp + (targetTemp - curTemp) * thermalRelaxationFactor);

            double pop = popArr[i];
            if (asteroidMiningFluxTonnesPerYear > 0.0 && pop > 0.0) {
                double popWeight = pop / (500.0 + pop);
                double mineralBoost = (asteroidMiningFluxTonnesPerYear / n) * 0.0001 * popWeight * dtSafe;
                metalArr[i] += (float) mineralBoost;
            }
        }
    }

    // Getters and Setters
    public static boolean isTerraformingActive() { return terraformingActive; }
    public static void setTerraformingActive(boolean active) { terraformingActive = active; }

    public static double getAtmosphericPressureAtm() { return atmosphericPressureAtm; }
    public static void setAtmosphericPressureAtm(double pressure) { atmosphericPressureAtm = Math.max(0.01, pressure); }

    public static double getCo2Ppm() { return co2Ppm; }
    public static void setCo2Ppm(double ppm) { co2Ppm = Math.max(10.0, ppm); }

    public static double getSolarMirrorInsolationMultiplier() { return solarMirrorInsolationMultiplier; }
    public static void setSolarMirrorInsolationMultiplier(double mult) { solarMirrorInsolationMultiplier = Math.max(0.5, Math.min(5.0, mult)); }

    public static double getAsteroidMiningFluxTonnesPerYear() { return asteroidMiningFluxTonnesPerYear; }
    public static void setAsteroidMiningFluxTonnesPerYear(double flux) { asteroidMiningFluxTonnesPerYear = Math.max(0.0, flux); }

    public static double getOffWorldEroeiRatio() { return offWorldEroeiRatio; }
    public static void setOffWorldEroeiRatio(double eroei) { offWorldEroeiRatio = Math.max(0.1, eroei); }

    public static void resetState() {
        terraformingActive = false;
        atmosphericPressureAtm = 1.0;
        co2Ppm = 280.0;
        solarMirrorInsolationMultiplier = 1.0;
        asteroidMiningFluxTonnesPerYear = 0.0;
        offWorldEroeiRatio = 15.0;
    }
}
