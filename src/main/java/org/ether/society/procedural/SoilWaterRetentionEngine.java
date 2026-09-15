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
 * Pédological Soil Water Retention & van Genuchten Moisture Characteristic Engine.
 *
 * <p>Models soil water matric potential and Available Water Capacity (AWC) via the
 * standard van Genuchten (1980) hydraulic formulation:</p>
 * <pre>
 *   θ(h) = θ_r + (θ_s - θ_r) / [1 + (α * |h|)^n]^m
 * </pre>
 * where:
 * <ul>
 *   <li><b>h</b>: Soil matric suction head (cm or kPa).</li>
 *   <li><b>θ_s</b>: Saturated water content (~0.43 cm³/cm³ for loam).</li>
 *   <li><b>θ_r</b>: Residual water content (~0.078 cm³/cm³).</li>
 *   <li><b>α</b>: Inverse of air-entry suction parameter (~0.036 cm⁻¹).</li>
 *   <li><b>n</b>: Pore-size distribution index (~1.56).</li>
 *   <li><b>m</b>: Mualem constraint $m = 1 - 1/n \approx 0.359$.</li>
 *   <li><b>Field Capacity ($\theta_{FC}$)</b>: Measured at suction head $h = -330\text{ cm}$ ($\text{pF} = 2.5$).</li>
 *   <li><b>Permanent Wilting Point ($\theta_{PWP}$)</b>: Measured at suction head $h = -15000\text{ cm}$ ($\text{pF} = 4.2$).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.3.0
 */
public class SoilWaterRetentionEngine {
    private static final Logger logger = LoggerFactory.getLogger(SoilWaterRetentionEngine.class);

    // Standard USDA Loam parameters
    public static final double THETA_S = 0.430;  // Saturated volumetric water content
    public static final double THETA_R = 0.078;  // Residual volumetric water content
    public static final double ALPHA = 0.036;    // cm^-1
    public static final double N_PARAM = 1.56;   // Pore size index
    public static final double M_PARAM = 1.0 - (1.0 / N_PARAM); // Mualem condition (~0.35897)

    // Standard matric suction heads (in cm of water head)
    public static final double HEAD_FIELD_CAPACITY_CM = 330.0;       // -33 kPa (pF 2.5)
    public static final double HEAD_WILTING_POINT_CM = 15000.0;     // -1500 kPa (pF 4.2)

    /**
     * Calculates volumetric soil water content θ(h) for matric suction head h (in cm).
     *
     * @param matricSuctionHeadCm Suction head |h| in cm of water
     * @return Volumetric water content θ (cm³/cm³)
     */
    public static double calculateWaterContent(double matricSuctionHeadCm) {
        if (matricSuctionHeadCm <= 0.0) {
            return THETA_S;
        }
        double alphaH = ALPHA * matricSuctionHeadCm;
        double denominator = Math.pow(1.0 + Math.pow(alphaH, N_PARAM), M_PARAM);
        return THETA_R + (THETA_S - THETA_R) / denominator;
    }

    /**
     * Computes the Plant Available Water Capacity fraction (0.0 = total drought / wilting, 1.0 = optimal field capacity).
     *
     * @param rainfallMm Annual precipitation in mm
     * @param declivity Slope gradient
     * @return Available water index [0.0, 1.0]
     */
    public static double calculatePlantAvailableWaterIndex(double rainfallMm, double declivity) {
        double thetaFC = calculateWaterContent(HEAD_FIELD_CAPACITY_CM);
        double thetaPWP = calculateWaterContent(HEAD_WILTING_POINT_CM);
        double awcMax = Math.max(0.001, thetaFC - thetaPWP);

        // Effective soil moisture modulated by rainfall and drainage slope
        double slopeDrainage = Math.max(0.1, 1.0 - 0.8 * declivity);
        double estimatedTheta = THETA_R + (THETA_S - THETA_R) * Math.min(1.0, (rainfallMm / 1200.0) * slopeDrainage);

        double plantWaterContent = Math.max(0.0, estimatedTheta - thetaPWP);
        return Math.min(1.0, plantWaterContent / awcMax);
    }

    /**
     * Processes soil moisture and updates agricultural water availability across H3 cells.
     */
    public static void processSoilWaterRetention(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            // Slope gradient approximated by biome / elevation
            double slope = (cell.getBiome() == Biome.MOUNTAINS) ? 0.35 : (cell.getBiome() == Biome.HILLS ? 0.15 : 0.03);

            double awcIndex = calculatePlantAvailableWaterIndex(rainfall, slope);

            // Modulate agricultural biomass based on van Genuchten plant available water
            double agriBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 100.0;
            double waterStressFactor = 0.2 + 0.8 * awcIndex;
            cell.setBiomassAgriculture(Math.max(10.0, agriBiomass * waterStressFactor));
        }
    }
}
