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
 * Cryospheric Thermodynamic & Positive Degree-Day (PDD) Glacial Melt Engine.
 *
 * <p>Models ice sheet and glacier ablation based on exact 1st Law Thermodynamics:</p>
 * <ul>
 *   <li><b>Latent Heat of Fusion</b>: $L_f = 333.55 \times 10^3 \text{ J/kg}$.</li>
 *   <li><b>Ice Density</b>: $\rho_{ice} = 917.0 \text{ kg/m}^3$.</li>
 *   <li><b>Positive Degree-Days (PDD)</b>: $\text{PDD} = \max(0, T - T_{melt})$ integrated over time.</li>
 *   <li><b>Melt Volume & Mass Balance</b>: $\Delta M_{ice} = \frac{Q_{net}}{L_f} = \frac{k_{pdd} \cdot \text{PDD} \cdot \Delta t}{L_f}$.</li>
 *   <li><b>Eustatic Sea Level Coupling</b>: $\Delta S_L = \frac{\sum \Delta M_{ice}}{\rho_{water} \cdot A_{ocean}}$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class GlacialThermodynamicMeltEngine {
    private static final Logger logger = LoggerFactory.getLogger(GlacialThermodynamicMeltEngine.class);

    /** Latent heat of ice fusion in J/kg */
    public static final double LATENT_HEAT_OF_FUSION_J_KG = 333550.0;

    /** Density of pure glacial ice in kg/m³ */
    public static final double ICE_DENSITY_KG_M3 = 917.0;

    /** Density of liquid water in kg/m³ */
    public static final double WATER_DENSITY_KG_M3 = 1000.0;

    /** Total planetary ocean surface area in m² (~3.61 x 10^14 m²) */
    public static final double GLOBAL_OCEAN_AREA_M2 = 3.61e14;

    /** PDD thermal exchange factor (W / (m² · K)) */
    public static final double PDD_HEAT_TRANSFER_COEFF = 9.8;

    /**
     * Computes glacial melt depth (in meters of ice) for a given surface temperature and time step.
     *
     * @param surfaceTempC Surface temperature in °C
     * @param deltaYears Time step in years
     * @return Ice melt depth in meters
     */
    public static double calculateGlacialMeltDepthMeters(double surfaceTempC, double deltaYears) {
        if (surfaceTempC <= 0.0 || deltaYears <= 0.0) {
            return 0.0;
        }

        double totalSeconds = deltaYears * 365.25 * 86400.0;
        double netThermalEnergyJoulesPerM2 = PDD_HEAT_TRANSFER_COEFF * surfaceTempC * totalSeconds;

        // Mass melted per m²: kg/m² = J/m² / (J/kg)
        double massMeltedKgPerM2 = netThermalEnergyJoulesPerM2 / LATENT_HEAT_OF_FUSION_J_KG;

        // Ice depth: m = (kg/m²) / (kg/m³)
        return massMeltedKgPerM2 / ICE_DENSITY_KG_M3;
    }

    /**
     * Processes cryospheric ablation across all glacial and tundra cells and computes global sea level rise.
     *
     * @param cells List of H3 cells
     * @param deltaYears Simulation time step in years
     * @return Global eustatic sea level increment in meters
     */
    public static double processGlacialMelt(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return 0.0;

        double totalMeltedMassKg = 0.0;
        int activeGlacialCells = 0;

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            boolean isGlacial = (biome == Biome.GLACIER || biome == Biome.SNOW || biome == Biome.TUNDRA
                    || (cell.getElevation() != null && cell.getElevation() > 2500.0));
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 0.0;

            if (isGlacial && tempC > 0.0) {
                double meltDepthM = calculateGlacialMeltDepthMeters(tempC, deltaYears);
                if (meltDepthM > 0.0) {
                    activeGlacialCells++;
                    // Typical H3 cell area at Res 3 is ~11,000 km² = 1.1 x 10^10 m²
                    double cellAreaM2 = 1.1e10;
                    double cellMeltMassKg = meltDepthM * ICE_DENSITY_KG_M3 * cellAreaM2;
                    totalMeltedMassKg += cellMeltMassKg;

                    // Reduce ice sheet thickness if present
                    if (cell.getIceSheetThicknessMeters() != null && cell.getIceSheetThicknessMeters() > 0.0) {
                        cell.setIceSheetThicknessMeters(Math.max(0.0, cell.getIceSheetThicknessMeters() - meltDepthM));
                    }

                    // Release meltwater into local water resource
                    double currentWater = cell.getWaterResource() != null ? cell.getWaterResource() : 500.0;
                    cell.setWaterResource(Math.min(1000.0, currentWater + Math.min(100.0, meltDepthM * 10.0)));
                }
            }
        }

        // Eustatic sea level rise: ΔS_L = Total Mass / (ρ_water * A_ocean)
        double eustaticRiseMeters = totalMeltedMassKg / (WATER_DENSITY_KG_M3 * GLOBAL_OCEAN_AREA_M2);

        if (activeGlacialCells > 0) {
            logger.debug("Glacial Thermodynamic Melt: {} active cells, total melt = {:.2e} kg, eustatic rise = {:.4f} mm",
                    activeGlacialCells, totalMeltedMassKg, eustaticRiseMeters * 1000.0);
        }

        return eustaticRiseMeters;
    }
}

