/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural engine modeling Geoengineering interventions and Terrestrial Albedo Feedback loops.
 *
 * <p>Simulates Stratospheric Aerosol Injection (SAI - SO2), Marine Cloud Brightening (MCB),
 * and planetary surface albedo modifications. Calculates radiative forcing cooling offsets,
 * regional precipitation disruption, and crop yield impacts on agricultural biomass.</p>

 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class GeoengineeringAlbedoFeedbackEngine extends AbstractSimulationEngine {
    public GeoengineeringAlbedoFeedbackEngine() {
        super("GeoengineeringAlbedoFeedbackEngine", "GeoengineeringAlbedoFeedbackEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(GeoengineeringAlbedoFeedbackEngine.class);

    private static double globalSaiAerosolLoadingTg = 0.0; // Teragrams of SO2 in stratospheric injection
    /** Internal state: global cooling effect celsius (double). */
    private static double globalCoolingEffectCelsius = 0.0;

    /**
     * Processes geoengineering radiative forcing and albedo feedbacks.
     */
    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        double dtSafe = Math.max(0.0001, dtYears);
        float[] popArr = buf.getBiomassHuman();
        float[] capArr = buf.getResourceCapital();
        float[] tempArr = buf.getTemperature();
        float[] elevArr = buf.getElevation();
        float[] rainArr = buf.getRainfall();
        double totalIndustrialSaiCapacity = 0.0;
        for (int i = 0; i < n; i++) {
            double pop = popArr[i] / 0.06;
            if (pop > 1000.0) {
                double capPerCap = capArr[i] / pop;
                if (capPerCap >= 2500.0) { totalIndustrialSaiCapacity += Math.min(10.0, (capPerCap - 2500.0) / 1000.0); }
            }
        }
        if (totalIndustrialSaiCapacity > 50.0) {
            globalSaiAerosolLoadingTg = Math.min(20.0, totalIndustrialSaiCapacity * 0.1);
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        } else {
            globalSaiAerosolLoadingTg = Math.max(0.0, globalSaiAerosolLoadingTg - dtSafe);
            globalCoolingEffectCelsius = globalSaiAerosolLoadingTg * 0.10;
        }
        if (globalCoolingEffectCelsius <= 0.0) return;
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 1.5);
        double rainDecay = Math.exp(-0.02 * globalSaiAerosolLoadingTg * dtSafe);
        for (int i = 0; i < n; i++) {
            double curTemp = tempArr[i];
            double baselineTemp = 20.0 - 0.0065 * Math.max(0.0, elevArr[i]);
            tempArr[i] = (float) (curTemp + ((baselineTemp - globalCoolingEffectCelsius) - curTemp) * thermalRelaxationFactor);
            if (rainArr[i] > 50.0f) { rainArr[i] = (float) Math.max(50.0, rainArr[i] * rainDecay); }
        }
    }

    public static double getGlobalSaiAerosolLoadingTg() {
        return globalSaiAerosolLoadingTg;
    }

    public static double getGlobalCoolingEffectCelsius() {
        return globalCoolingEffectCelsius;
    }

    public static void resetState() {
        globalSaiAerosolLoadingTg = 0.0;
        globalCoolingEffectCelsius = 0.0;
    }
}
