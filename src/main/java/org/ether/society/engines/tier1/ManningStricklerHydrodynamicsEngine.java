/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

import java.util.List;

/**
 * Open-Channel River Hydrodynamics &amp; Floodplain Inundation Engine (Manning-Strickler Formula).
 * <p>
 * Models gravity-driven open-channel water velocity $v = \frac{1}{n} R_h^{2/3} S^{1/2}$ and alluvial
 * floodplain silt deposition using smooth ($C^\infty$) functions of continuous elevation $z$ and
 * meteoric/catchment runoff ({@code AGENTS.md} §3, §10.2), without discrete {@code Biome} slope switches.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ManningStricklerHydrodynamicsEngine {

    /** Standard natural river channel Manning roughness coefficient ($\text{s}\cdot\text{m}^{-1/3}$). */
    public static final double MANNING_ROUGHNESS_N = 0.035;

    /**
     * Calculates open-channel flow velocity (m/s) using the Manning-Strickler equation.
     */
    public static double calculateFlowVelocity(double hydraulicRadiusM, double bedSlope, double manningN) {
        if (hydraulicRadiusM <= 0.0 || bedSlope <= 0.0 || manningN <= 0.0) {
            return 0.0;
        }
        double s = SmoothMath.softmax(1e-5, bedSlope, 1e4);
        double rh = SmoothMath.softmax(0.1, hydraulicRadiusM, 20.0);
        return (1.0 / manningN) * Math.pow(rh, 2.0 / 3.0) * Math.sqrt(s);
    }

    /**
     * Calculates river discharge $Q$ ($\text{m}^3/\text{s}$) for a rectangular channel of given width and depth.
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
     * Processes river hydrodynamics and alluvial floodplain fertilization across H3 cells using
     * continuous elevation-driven catchment partitioning and bed slope.
     */
    public static void processRiverHydrodynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double totalCatchmentRunoff = 0.0;
        double upstreamWeightSum = 0.0;
        double lowlandWeightSum = 0.0;
        boolean isWindowed = false;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            if (cell.isBoundaryCell()) isWindowed = true;
            Biome b = cell.getBiome();
            if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) continue;

            double elev = cell.getElevationOr(0.0);
            if (elev <= 0.0) continue;
            double headwaterWeight = SmoothMath.sigmoid(elev, 150.0, 50.0);
            double rain = cell.getRainfallOr(500.0);
            totalCatchmentRunoff += rain * 0.40 * headwaterWeight;
            upstreamWeightSum += headwaterWeight;
            lowlandWeightSum += (1.0 - headwaterWeight);
        }

        if (isWindowed && totalCatchmentRunoff < 1000.0) {
            int boundaryRiverChannels = Math.max(1, (int) Math.sqrt(cells.size()));
            totalCatchmentRunoff += 1200.0 * 0.35 * boundaryRiverChannels;
            upstreamWeightSum += boundaryRiverChannels;
        }

        double catchmentRunoffPerLowland = upstreamWeightSum > 0.0
                ? (totalCatchmentRunoff / Math.max(1.0, lowlandWeightSum))
                : 0.0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome b = cell.getBiome();
            if (b == Biome.OCEAN || b == Biome.DEEP_OCEAN) continue;
            double elev = cell.getElevationOr(0.0);
            if (elev < 0.0) continue;

            double rainfall = cell.getRainfallOr(500.0);
            double lowlandFactor = 1.0 - SmoothMath.sigmoid(elev, 120.0, 40.0);
            double effectiveFlowRainfall = rainfall + catchmentRunoffPerLowland * lowlandFactor;

            // Smooth continuous hydraulic bed slope S(z): 0.0005 on flat alluvial plains -> 0.025 in high mountains
            double slope = 0.0004 + 0.025 * Math.tanh(Math.max(0.0, elev) / 1800.0);
            double estimatedDepth = SmoothMath.softmax(0.5, (effectiveFlowRainfall / 1000.0) * 3.0, 4.0);
            double estimatedWidth = SmoothMath.softmax(10.0, Math.sqrt(Math.max(0.0, effectiveFlowRainfall)) * 2.5, 0.5);

            double dischargeQ = calculateDischarge(estimatedWidth, estimatedDepth, slope);

            // Smooth alluvial silt deposition in lowland floodplains when discharge exceeds bankfull threshold (~50 m^3/s)
            double overbankGate = SmoothMath.sigmoid(dischargeQ, 50.0, 15.0) * lowlandFactor;
            if (overbankGate > 0.01) {
                int pop = cell.getPopulationOr(0);
                double currentAgri = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                        ? cell.getBiomassAgriculture()
                        : Math.min(2000.0, Math.max(50.0, pop * 2.5));
                double floodplainFertilityBoost = SmoothMath.softmin(25.0, dischargeQ * 0.05 * deltaYears, 0.2) * overbankGate;
                cell.setBiomassAgriculture(currentAgri + floodplainFertilityBoost);
                double alluvialWater = SmoothMath.softmin(1000.0, dischargeQ * 2.5 * overbankGate, 0.02);
                cell.setWaterResource(Math.max(cell.getWaterResourceOr(0.0), alluvialWater));
            }
        }
    }
}
