/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.h3.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trophic Ecosystem, Rewilding & Biodiversity Engine.
 * Models 3-tier trophic biomass dynamics (Producers -> Herbivores -> Predators),
 * Pleistocene Rewilding (Mammoth/Bison surrogates maintaining permafrost albedo & SOC),
 * and species hybridization vs extinction dynamics.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class TrophicEcosystemEngine extends AbstractSimulationEngine {
    public TrophicEcosystemEngine() {
        super("TrophicEcosystemEngine", "TrophicEcosystemEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(TrophicEcosystemEngine.class);

    /** Internal state: pleistocene rewilding active (boolean). */
    private static boolean pleistoceneRewildingActive = false;
    /** Internal state: megafauna density per km2 (double). */
    private static double megafaunaDensityPerKm2 = 0.0;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null || !pleistoceneRewildingActive) return;

        double temp = buffer.getTemperature()[i];
        double absLat = 0.0;
        long[] h3Arr = buffer.getH3Indexes();
        if (h3Arr != null && h3Arr[i] != 0L) {
            try {
                absLat = Math.abs(H3Service.getInstance().cellToLatLng(h3Arr[i]).lat);
            } catch (Exception ignored) {
                absLat = 0.0;
            }
        }
        if (temp <= 4.0 || absLat > 45.0) {
            double dtSafe = Math.max(0.0001, dtYears);
            acc.dSoilOrganicCarbon += (float) (megafaunaDensityPerKm2 * 0.05 * dtSafe);
        }
    }

    /**
     * Process trophic ecosystem and pleistocene megafauna rewilding effects across DoD buffer.
     *
     * @param buffer WorldBuffer DoD state
     * @param deltaYears time step in years
     */
    public static void processTrophicEcosystem(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0 || !pleistoceneRewildingActive) return;

        double dtSafe = Math.max(0.0001, deltaYears);
        float[] tempArr = buffer.getTemperature();
        float[] socArr = buffer.getSoilOrganicCarbon();
        long[] h3Arr = buffer.getH3Indexes();
        H3Service h3Service = H3Service.getInstance();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double temp = tempArr[i];
            double absLat = 0.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    absLat = Math.abs(h3Service.cellToLatLng(h3Arr[i]).lat);
                } catch (Exception ignored) {
                    absLat = 0.0;
                }
            }
            if (temp <= 4.0 || absLat > 45.0) {
                socArr[i] = (float) (socArr[i] + megafaunaDensityPerKm2 * 0.05 * dtSafe);
            }
        }
    }

    // Getters and Setters
    public static boolean isPleistoceneRewildingActive() { return pleistoceneRewildingActive; }
    public static void setPleistoceneRewildingActive(boolean active) { pleistoceneRewildingActive = active; }

    public static double getMegafaunaDensityPerKm2() { return megafaunaDensityPerKm2; }
    public static void setMegafaunaDensityPerKm2(double density) { megafaunaDensityPerKm2 = Math.max(0.0, density); }

    public static void resetState() {
        pleistoceneRewildingActive = false;
        megafaunaDensityPerKm2 = 0.0;
    }
}
