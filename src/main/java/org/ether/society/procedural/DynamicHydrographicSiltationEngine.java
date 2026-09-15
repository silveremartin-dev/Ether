/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 *   v_s = (2/9) * (ρ_p - ρ_f) * g * r² / μ(T)
 * </pre>
 * where:
 * <ul>
 *   <li><b>ρ_p</b>: Quartz/alluvium sediment particle density (~2650 kg/m³).</li>
 *   <li><b>ρ_f</b>: Fluid water density (~1000 kg/m³).</li>
 *   <li><b>g</b>: Gravitational acceleration (9.80665 m/s²).</li>
 *   <li><b>r</b>: Grain radius (fine silt $r \approx 20 \times 10^{-6}\text{ m}$).</li>
 *   <li><b>μ(T)</b>: Water dynamic viscosity as a function of temperature.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.3.0
 */
public class DynamicHydrographicSiltationEngine {
    private static final Logger logger = LoggerFactory.getLogger(DynamicHydrographicSiltationEngine.class);

    /** Quartz sediment particle density in kg/m³ */
    public static final double SEDIMENT_DENSITY_KG_M3 = 2650.0;

    /** Fluid (water) density in kg/m³ */
    public static final double WATER_DENSITY_KG_M3 = 1000.0;

    /** Gravitational acceleration in m/s² */
    public static final double GRAVITY_M_S2 = 9.80665;

    /** Typical fine silt particle radius in meters (20 μm) */
    public static final double SILT_GRAIN_RADIUS_M = 20.0e-6;

    /** Water dynamic viscosity at 20°C in Pa·s (N·s/m²) */
    public static final double WATER_VISCOSITY_PA_S = 1.002e-3;

    /**
     * Calculates Stokes settling terminal velocity v_s in m/s.
     *
     * @param grainRadiusMeters Particle radius in meters
     * @param waterTempC Water temperature in °C
     * @return Settling velocity in m/s
     */
    public static double calculateStokesSettlingVelocity(double grainRadiusMeters, double waterTempC) {
        // Temperature-dependent dynamic viscosity: μ(T) ≈ 2.414e-5 * 10^(247.8 / (T + 133.15))
        double tempK = Math.max(273.15, waterTempC + 273.15);
        double viscosity = 2.414e-5 * Math.pow(10.0, 247.8 / (tempK - 140.0));

        double deltaRho = SEDIMENT_DENSITY_KG_M3 - WATER_DENSITY_KG_M3;
        return (2.0 / 9.0) * (deltaRho * GRAVITY_M_S2 * grainRadiusMeters * grainRadiusMeters) / viscosity;
    }

    /**
     * Processes river discharge, sediment erosion, and estuarine harbor siltation using Stokes settling velocity.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processHydrographicSiltation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getElevation() == null) continue;

            // Coastal / Estuary cells (elevation near sea level: -10m to +25m)
            boolean isEstuaryOrCoast = cell.getElevation() >= -10.0 && cell.getElevation() <= 25.0
                    && (cell.getBiome() == Biome.BEACH || cell.getBiome() == Biome.PLAINS);

            if (isEstuaryOrCoast) {
                double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
                double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
                double naturalVeg = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 500.0;

                // Erosion index based on rainfall and canopy/vegetation cover
                double erosionIndex = Math.max(0.1, (rainfall / 1000.0) * (1.0 - Math.clamp(naturalVeg / 1000.0, 0.0, 1.0)));

                // Stokes settling velocity (m/s)
                double settlingVelocity = calculateStokesSettlingVelocity(SILT_GRAIN_RADIUS_M, tempC);

                // Annual silt deposition depth (m/year) scaled by Stokes settling kinetics and erosion index
                // settlingVelocity ≈ 0.00072 m/s -> scaled to geological river basin sediment budget
                double siltationIncrement = erosionIndex * (settlingVelocity * 100.0) * deltaYears;
                cell.setElevation(cell.getElevation() + siltationIncrement);

                // Movement friction penalty: Harbor siltation increases maritime navigation cost
                double currentFriction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                if (siltationIncrement > 0.005) {
                    double newFriction = Math.min(15.0, currentFriction + (siltationIncrement * 3.0));
                    cell.setMovementFriction(newFriction);
                }
            }
        }
    }
}
