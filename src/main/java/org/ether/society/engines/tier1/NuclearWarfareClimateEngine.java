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
import org.ether.society.h3.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global Nuclear Winter & Stratospheric Soot Climate Engine.
 * Models the macro-environmental consequences of high-intensity nuclear conflict:
 * 1. <b>Stratospheric Soot Injection (Tg Soot)</b>: Biomass & urban firestorms inject carbonaceous aerosol into the upper atmosphere.
 * 2. <b>Solar Irradiance Attenuation (S_0 * e^-Ï„)</b>: Atmospheric soot optical depth (Ï„) attenuates surface solar irradiance S_0.
 * 3. <b>Nuclear Winter Cooling & Famine</b>: Sub-zero surface temperature anomaly (Î”T_cool) causes global crop failure and demographic collapse.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class NuclearWarfareClimateEngine extends AbstractSimulationEngine {
    public NuclearWarfareClimateEngine() {
        super("NuclearWarfareClimateEngine", "NuclearWarfareClimateEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(NuclearWarfareClimateEngine.class);

    /** Power flux threshold per capita indicating full nuclear capability */
    public static final double NUCLEAR_CAPABILITY_POWER_WATTS = 20000.0;

    /** Global stratospheric soot optical depth accumulation factor */
    private static double globalSootOpticalDepth = 0.0;
    private static long baseSeed = 42L;
    private static long invocationCounter = 0L;

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
        return Math.exp(-opticalDepth);
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processNuclearWarfareClimate(buf);
    }

    /**
     * Executes one nuclear conflict trigger, soot injection, and nuclear winter climate tick.
     */
    public static void processNuclearWarfareClimate(WorldBuffer buffer) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        int sootInjectionEvents = 0;
        invocationCounter++;

        float[] bioHuman = buffer.getBiomassHuman();
        float[] uraniumArr = buffer.getResourceUranium();
        float[] capArr = buffer.getResourceCapital();
        float[] pollutionArr = buffer.getPollutionLevel();
        float[] tempArr = buffer.getTemperature();
        float[] elevArr = buffer.getElevation();
        float[] agriArr = buffer.getBiomassAgriculture();
        long[] h3Arr = buffer.getH3Indexes();
        int n = buffer.getCapacity();

        // 1. Evaluate conflict triggers & stratospheric soot injection
        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 5000.0) continue;

            double uranium = uraniumArr[i];
            if (uranium <= 0.0) continue;

            double powerPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(buffer, i);
            double capital = capArr[i];

            if (powerPerCapita >= NUCLEAR_CAPABILITY_POWER_WATTS && capital > 20000.0) {
                long cellId = (h3Arr != null && h3Arr[i] != 0L) ? h3Arr[i] : i;
                double prng = org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, invocationCounter, cellId, 211L);
                if (prng < 0.0001) {
                    sootInjectionEvents++;
                    globalSootOpticalDepth += 0.50;

                    bioHuman[i] = (float) (bioHuman[i] * 0.20);
                    capArr[i] = (float) (capital * 0.10);
                    pollutionArr[i] = pollutionArr[i] + 25000.0f;
                }
            }
        }

        // 2. Apply Nuclear Winter global solar attenuation & temperature anomaly
        if (globalSootOpticalDepth > 0.01) {
            double transmittance = calculateSolarTransmittance(globalSootOpticalDepth);
            double tempAnomaly = -15.0 * (1.0 - transmittance);
            H3Service h3Service = H3Service.getInstance();

            for (int i = 0; i < n; i++) {
                double curTemp = tempArr[i];
                double elev = elevArr[i];
                double lat = 0.0;
                if (h3Arr != null && h3Arr[i] != 0L) {
                    try {
                        lat = h3Service.cellToLatLng(h3Arr[i]).lat;
                    } catch (Exception ignored) {
                        lat = 0.0;
                    }
                }
                double baselineTemp = 28.0 - 0.0065 * Math.max(0.0, elev) - 0.45 * Math.abs(lat);
                double targetTemp = baselineTemp + tempAnomaly;
                double thermalRelaxationFactor = 0.50;
                float nextTemp = (float) (curTemp + (targetTemp - curTemp) * thermalRelaxationFactor);
                tempArr[i] = nextTemp;

                if (nextTemp < 0.0f) {
                    agriArr[i] = Math.max(0.0f, agriArr[i] * 0.10f);
                }
            }

            globalSootOpticalDepth *= 0.98;

            logger.warn("Nuclear Winter Engine: Stratospheric soot optical depth tau={}. Global temperature anomaly: {} C across cells.",
                    String.format(java.util.Locale.ROOT, "%.2f", globalSootOpticalDepth), String.format(java.util.Locale.ROOT, "%.1f", tempAnomaly));
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

    public static void resetState() {
        globalSootOpticalDepth = 0.0;
        invocationCounter = 0L;
    }
}
