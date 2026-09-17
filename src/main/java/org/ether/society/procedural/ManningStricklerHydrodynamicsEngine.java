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
 * Open-Channel River Hydrodynamics & Floodplain Inundation Engine (Manning-Strickler Formula).
 *
 * <p>Models gravity-driven open-channel water flow velocity and volumetric discharge in river basins:</p>
 * <ul>
 *   <li><b>Manning-Strickler Mean Velocity Formula</b>:
 *     $$v = \frac{1}{n} R_h^{2/3} S^{1/2} = K_{\text{strickler}} \cdot R_h^{2/3} \cdot S^{1/2} \quad [\text{m/s}]$$
 *     where:
 *     - $n$: Manning roughness coefficient ($n \approx 0.030\text{ s}\cdot\text{m}^{-1/3}$ for natural clean channels).
 *     - $R_h$: Hydraulic radius ($R_h = A / P_{\text{wetted}} \approx \text{depth}$ in wide channels).
 *     - $S$: Channel bed hydraulic slope ($S = |\Delta z| / d$).
 *   </li>
 *   <li><b>Volumetric Discharge ($Q$)</b>: $Q = v \cdot A \quad [\text{m}^3/\text{s}]$.</li>
 *   <li><b>Floodplain Overtop & Inundation</b>: When discharge $Q > Q_{\text{bankfull}}$, water overtops natural levees,
 *       depositing fertile alluvial silt and replenishing soil moisture in riparian floodplains (Nile, Tigris-Euphrates, Indus).
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ManningStricklerHydrodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(ManningStricklerHydrodynamicsEngine.class);

    /** Standard natural river channel Manning roughness coefficient (s/m^(1/3)) */
    public static final double MANNING_ROUGHNESS_N = 0.035;

    /**
     * Calculates open channel flow velocity (m/s) using Manning-Strickler equation.
     *
     * @param hydraulicRadiusM Hydraulic radius R_h in meters (approximated by river depth)
     * @param bedSlope Bed slope gradient S (unitless, e.g. 0.001 = 1m drop per 1km)
     * @param manningN Roughness coefficient n
     * @return Mean water velocity in m/s
     */
    public static double calculateFlowVelocity(double hydraulicRadiusM, double bedSlope, double manningN) {
        if (hydraulicRadiusM <= 0.0 || bedSlope <= 0.0 || manningN <= 0.0) {
            return 0.0;
        }
        double s = Math.max(1e-5, bedSlope);
        double rh = Math.max(0.1, hydraulicRadiusM);
        return (1.0 / manningN) * Math.pow(rh, 2.0 / 3.0) * Math.sqrt(s);
    }

    /**
     * Calculates river discharge Q (m³/s) for a rectangular channel of given width and depth.
     */
    public static double calculateDischarge(double widthM, double depthM, double bedSlope) {
        if (widthM <= 0.0 || depthM <= 0.0) return 0.0;
        double area = widthM * depthM;
        double wettedPerimeter = widthM + 2.0 * depthM;
        double rh = area / wettedPerimeter;
        double v = calculateFlowVelocity(rh, bedSlope, MANNING_ROUGHNESS_N);
        return v * area;
    }

    /**
     * Processes river hydrodynamics and floodplain agricultural fertilization across H3 cells.
     */
    public static void processRiverHydrodynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double rainfall = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            // Slope estimated by terrain
            double slope = (cell.getBiome() == Biome.MOUNTAINS) ? 0.02 : (cell.getBiome() == Biome.HILLS ? 0.005 : 0.0005);
            double estimatedDepth = Math.max(0.5, (rainfall / 1000.0) * 3.0);
            double estimatedWidth = Math.max(10.0, Math.sqrt(rainfall) * 2.5);

            double dischargeQ = calculateDischarge(estimatedWidth, estimatedDepth, slope);

            // Fertile floodplain silt bonus for large lowland rivers (Plains/Beach with high discharge)
            if (dischargeQ > 50.0 && (cell.getBiome() == Biome.PLAINS || cell.getBiome() == Biome.BEACH)) {
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 50.0;
                double floodplainFertilityBoost = Math.min(25.0, dischargeQ * 0.05 * deltaYears);
                cell.setBiomassAgriculture(Math.min(1000.0, currentAgri + floodplainFertilityBoost));
            }
        }
    }
}

