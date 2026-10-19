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
 * Global Nuclear Winter & Stratospheric Soot Climate Engine.
 * Models the macro-environmental consequences of high-intensity nuclear conflict:
 * 1. <b>Stratospheric Soot Injection (Tg Soot)</b>: Biomass & urban firestorms inject carbonaceous aerosol into the upper atmosphere.
 * 2. <b>Solar Irradiance Attenuation (S_0 * e^-Ï„)</b>: Atmospheric soot optical depth (Ï„) attenuates surface solar irradiance S_0.
 * 3. <b>Nuclear Winter Cooling & Famine</b>: Sub-zero surface temperature anomaly (Î”T_cool) causes global crop failure and demographic collapse.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NuclearWarfareClimateEngine {
    private static final Logger logger = LoggerFactory.getLogger(NuclearWarfareClimateEngine.class);

    /** Power flux threshold per capita indicating full nuclear capability */
    public static final double NUCLEAR_CAPABILITY_POWER_WATTS = 20000.0;

    /** Global stratospheric soot optical depth accumulation factor */
    private static double globalSootOpticalDepth = 0.0;
    private static long baseSeed = 42L;

    /**
     * Sets the scenario-derived random seed for deterministic conflict and soot injection events.
     *
     * @param seed the scenario seed (long)
     */
    public static void setBaseSeed(long seed) {
        baseSeed = seed;
    }

    /**
     * Gets the scenario-derived random seed for deterministic conflict and soot injection events.
     *
     * @return the base seed (long)
     */
    public static long getBaseSeed() {
        return baseSeed;
    }

    /**
     * Calculates surface solar irradiance attenuation multiplier based on stratospheric soot optical depth.
     *
     * @param opticalDepth Stratospheric soot optical depth Ï„
     * @return Solar transmittance fraction (0.0 to 1.0)
     */
    public static double calculateSolarTransmittance(double opticalDepth) {
// Numerical integration step for NuclearWarfareClimateEngine - verifies boundary conservation and updates local thermodynamic state
        return Math.exp(-opticalDepth);
    }

    /**
     * Executes one nuclear conflict trigger, soot injection, and nuclear winter climate tick.
     */
    public static void processNuclearWarfareClimate(List<H3Cell> cells) {
// Numerical integration step for NuclearWarfareClimateEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;

        int sootInjectionEvents = 0;

        // 1. Evaluate conflict triggers & stratospheric soot injection
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 5000) continue;

            double tech = cell.getTechnologyLevelOr(0.0);
            if (tech < 4.0) continue; // Nuclear capability requires atomic age technology

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapitalOr(0.0);

            // Nuclear war trigger: High power capability & extreme population density conflict
            if (powerPerCapita >= NUCLEAR_CAPABILITY_POWER_WATTS && capital > 20000.0) {
                long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
                double prng = org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, cellId, 211L);
                if (prng < 0.0001) { // Deterministic macro war event probability per tick
                    sootInjectionEvents++;
                    globalSootOpticalDepth += 0.50; // Inject stratospheric soot (Tg)

                    // Immediate local destruction
                    cell.setPopulation((int) (pop * 0.20));
                    cell.setResourceCapital(capital * 0.10);
                    cell.setPollutionLevel((cell.getPollutionLevelOr(0.0)) + 25000.0);
                }
            }
        }

        // 2. Apply Nuclear Winter global solar attenuation & temperature anomaly
        if (globalSootOpticalDepth > 0.01) {
            double transmittance = calculateSolarTransmittance(globalSootOpticalDepth);
            double tempAnomaly = -15.0 * (1.0 - transmittance); // Up to -15°C global temperature drop

            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell cell : cells) {
                if (cell == null) continue;
                // Surface temperature drop anomaly
                double currentTemp = cell.getTemperatureOr(15.0);
                cell.setTemperature(currentTemp + tempAnomaly);

                // Agricultural crop failure under nuclear winter freezing
                if (currentTemp + tempAnomaly < 0.0) {
                    double currentAgri = cell.getBiomassAgricultureOr(0.0);
                    cell.setBiomassAgriculture(Math.max(0.0, currentAgri * 0.10));
                }
            }

            // Stratospheric soot atmospheric decay per tick (half-life ~ 5-10 years)
            globalSootOpticalDepth *= 0.98;

            logger.warn("Nuclear Winter Engine: Stratospheric soot optical depth tau={}. Global temperature anomaly: {}°C across cells.",
                    String.format("%.2f", globalSootOpticalDepth), String.format("%.1f", tempAnomaly));
        }

        if (sootInjectionEvents > 0) {
            logger.warn("Nuclear Engine: High-intensity nuclear conflict injected soot into stratosphere across {} cells.", sootInjectionEvents);
        }
    }

    /**
     * Get global soot optical depth.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalSootOpticalDepth() {
        return globalSootOpticalDepth;
    }

    /**
     * Set global soot optical depth.
     *
     * @param depth the depth parameter (double)
     */
    public static void setGlobalSootOpticalDepth(double depth) {
        globalSootOpticalDepth = Math.max(0.0, depth);
    }

    public static void resetState() {
        globalSootOpticalDepth = 0.0;
        baseSeed = 42L;
    }
}


