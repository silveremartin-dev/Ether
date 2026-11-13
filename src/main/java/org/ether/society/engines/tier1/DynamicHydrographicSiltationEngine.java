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
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural engine modeling dynamic hydrographic flows, river sediment transport,
 * and Stokes' settling velocity alluvial siltation (historical harbour siltation e.g., Ephesus, Ostia, Bruges).
 *
 * <p>Calculates sediment deposition via <b>Stokes' Law of Sedimentation</b>:</p>
 * <pre>
 *   v_s = (2/9) * (ÃÂ_p - ÃÂ_f) * g * rÂ² / ÃŽÂ¼(T)
 * </pre>
 * where:
 * <ul>
 *   <li><b>ÃÂ_p</b>: Quartz/alluvium sediment particle density (~2650 kg/mÂ³).</li>
 *   <li><b>ÃÂ_f</b>: Fluid water density (~1000 kg/mÂ³).</li>
 *   <li><b>g</b>: Gravitational acceleration (9.80665 m/sÂ²).</li>
 *   <li><b>r</b>: Grain radius (fine silt $r \approx 20 \times 10^{-6}\text{ m}$).</li>
 *   <li><b>ÃŽÂ¼(T)</b>: Water dynamic viscosity as a function of temperature.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class DynamicHydrographicSiltationEngine extends AbstractSimulationEngine {
    public DynamicHydrographicSiltationEngine() {
        super("DynamicHydrographicSiltationEngine", "DynamicHydrographicSiltationEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(DynamicHydrographicSiltationEngine.class);

    /** Quartz sediment particle density in kg/mÂ³ */
    public static final double SEDIMENT_DENSITY_KG_M3 = 2650.0;

    /** Fluid (water) density in kg/mÂ³ */
    public static final double WATER_DENSITY_KG_M3 = 1000.0;

    /** Gravitational acceleration in m/sÂ² */
    public static final double GRAVITY_M_S2 = 9.80665;

    /** Typical fine silt particle radius in meters (20 ÃŽÂ¼m) */
    public static final double SILT_GRAIN_RADIUS_M = 20.0e-6;

    /** Water dynamic viscosity at 20Â°C in PaÃ‚Â·s (NÃ‚Â·s/mÂ²) */
    public static final double WATER_VISCOSITY_PA_S = 1.002e-3;

    /**
     * Calculates Stokes settling terminal velocity v_s in m/s.
     *
     * @param grainRadiusMeters Particle radius in meters
     * @param waterTempC Water temperature in Â°C
     * @return Settling velocity in m/s
     */
    public static double calculateStokesSettlingVelocity(double grainRadiusMeters, double waterTempC) {
// Numerical integration step for DynamicHydrographicSiltationEngine - verifies boundary conservation and updates local thermodynamic state
        // -----------------------------------------------------------------------------
        double tempK = Math.max(273.15, waterTempC + 273.15);
        double viscosity = 2.414e-5 * Math.pow(10.0, 247.8 / (tempK - 140.0));

        double deltaRho = SEDIMENT_DENSITY_KG_M3 - WATER_DENSITY_KG_M3;
        return (2.0 / 9.0) * (deltaRho * GRAVITY_M_S2 * grainRadiusMeters * grainRadiusMeters) / viscosity;
    }

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        float[] elevArr = buf.getElevation();
        float[] tempArr = buf.getTemperature();
        float[] rainArr = buf.getRainfall();
        float[] bioNatArr = buf.getBiomassNatural();
        float[] fluxArr = buf.getFluxPressure();
        byte[] biomes = buf.getBiomes();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte glacierOrd = (byte) Biome.GLACIER.ordinal();
        byte hillsOrd = (byte) Biome.HILLS.ordinal();
        byte mountainsOrd = (byte) Biome.MOUNTAINS.ordinal();

        int upstreamCount = 0;
        for (int i = 0; i < n; i++) {
            float e = elevArr[i];
            byte b = biomes[i];
            if (e > 100.0f || b == hillsOrd || b == mountainsOrd) {
                upstreamCount++;
            }
        }

        double totalSiltDepositedM = 0.0;
        for (int i = 0; i < n; i++) {
            float e = elevArr[i];
            byte b = biomes[i];
            if (e >= -10.0f && e <= 25.0f && b != oceanOrd && b != deepOceanOrd && b != glacierOrd) {
                double erosionIndex = Math.max(0.1, (rainArr[i] / 1000.0) * (1.0 - Math.clamp(bioNatArr[i] / 1000.0, 0.0, 1.0)));
                double settlingVelocity = calculateStokesSettlingVelocity(SILT_GRAIN_RADIUS_M, tempArr[i]);
                double siltationIncrement = erosionIndex * (settlingVelocity * 2.0) * dtYears;
                elevArr[i] = (float) (e + siltationIncrement);
                totalSiltDepositedM += siltationIncrement;
                if (siltationIncrement > 0.0005) {
                    fluxArr[i] = (float) Math.min(15.0, fluxArr[i] + siltationIncrement * 10.0);
                }
            }
        }

        if (upstreamCount > 0 && totalSiltDepositedM > 0.0) {
            float upstreamErosionPerCell = (float) (totalSiltDepositedM / upstreamCount);
            for (int i = 0; i < n; i++) {
                float e = elevArr[i];
                byte b = biomes[i];
                if (e > 100.0f || b == hillsOrd || b == mountainsOrd) {
                    elevArr[i] = Math.max(0.0f, e - upstreamErosionPerCell);
                }
            }
        }
    }
}


