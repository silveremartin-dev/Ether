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
 * Global Nuclear Winter & Stratospheric Soot Climate Engine.
 * Models the macro-environmental consequences of high-intensity nuclear conflict:
 * 1. <b>Stratospheric Soot Injection (Tg Soot)</b>: Biomass & urban firestorms inject carbonaceous aerosol into the upper atmosphere.
 * 2. <b>Solar Irradiance Attenuation (S_0 * e^-τ)</b>: Atmospheric soot optical depth (τ) attenuates surface solar irradiance S_0.
 * 3. <b>Nuclear Winter Cooling & Famine</b>: Sub-zero surface temperature anomaly (ΔT_cool) causes global crop failure and demographic collapse.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class NuclearWarfareClimateEngine {
    private static final Logger logger = LoggerFactory.getLogger(NuclearWarfareClimateEngine.class);

    /** Power flux threshold per capita indicating full nuclear capability */
    public static final double NUCLEAR_CAPABILITY_POWER_WATTS = 20000.0;

    /** Global stratospheric soot optical depth accumulation factor */
    private static double globalSootOpticalDepth = 0.0;

    /**
     * Calculates surface solar irradiance attenuation multiplier based on stratospheric soot optical depth.
     *
     * @param opticalDepth Stratospheric soot optical depth τ
     * @return Solar transmittance fraction (0.0 to 1.0)
     */
    public static double calculateSolarTransmittance(double opticalDepth) {
        return Math.exp(-opticalDepth);
    }

    /**
     * Executes one nuclear conflict trigger, soot injection, and nuclear winter climate tick.
     */
    public static void processNuclearWarfareClimate(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int sootInjectionEvents = 0;

        // 1. Evaluate conflict triggers & stratospheric soot injection
        for (H3Cell cell : cells) {
            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            // Nuclear war trigger: High power capability & extreme population density conflict
            if (powerPerCapita >= NUCLEAR_CAPABILITY_POWER_WATTS && pop > 5000 && capital > 20000.0) {
                if (Math.random() < 0.0001) { // Macro war event probability per tick
                    sootInjectionEvents++;
                    globalSootOpticalDepth += 0.50; // Inject stratospheric soot (Tg)

                    // Immediate local destruction
                    cell.setPopulation((int) (pop * 0.20));
                    cell.setResourceCapital(capital * 0.10);
                    cell.setPollutionLevel((cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0) + 25000.0);
                }
            }
        }

        // 2. Apply Nuclear Winter global solar attenuation & temperature anomaly
        if (globalSootOpticalDepth > 0.01) {
            double transmittance = calculateSolarTransmittance(globalSootOpticalDepth);
            double tempAnomaly = -15.0 * (1.0 - transmittance); // Up to -15°C global temperature drop

            for (H3Cell cell : cells) {
                // Surface temperature drop anomaly
                double currentTemp = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
                cell.setTemperature(currentTemp + tempAnomaly);

                // Agricultural crop failure under nuclear winter freezing
                if (currentTemp + tempAnomaly < 0.0) {
                    double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                    cell.setBiomassAgriculture(Math.max(0.0, currentAgri * 0.10));
                }
            }

            // Stratospheric soot atmospheric decay per tick (half-life ~ 5-10 years)
            globalSootOpticalDepth *= 0.98;

            logger.warn("Nuclear Winter Engine: Stratospheric soot optical depth τ={}. Global temperature anomaly: {}°C across cells.",
                    String.format("%.2f", globalSootOpticalDepth), String.format("%.1f", tempAnomaly));
        }

        if (sootInjectionEvents > 0) {
            logger.warn("Nuclear Engine: High-intensity nuclear conflict injected soot into stratosphere across {} cells.", sootInjectionEvents);
        }
    }

    public static double getGlobalSootOpticalDepth() {
        return globalSootOpticalDepth;
    }

    public static void setGlobalSootOpticalDepth(double depth) {
        globalSootOpticalDepth = Math.max(0.0, depth);
    }
}

