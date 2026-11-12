/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
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
 * @version 1.0.0-beta.3
 */
public class ManningStricklerHydrodynamicsEngine extends AbstractSimulationEngine {
    public ManningStricklerHydrodynamicsEngine() {
        super("ManningStricklerHydrodynamicsEngine", "ManningStricklerHydrodynamicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


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

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        byte[] biomes = buf.getBiomes();
        float[] elevArr = buf.getElevation();
        float[] rainArr = buf.getRainfall();
        float[] popArr = buf.getBiomassHuman();
        float[] agriArr = buf.getBiomassAgriculture();
        float[] waterArr = buf.getWaterResource();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();

        double totalCatchmentRunoff = 0.0;
        double upstreamWeightSum = 0.0;
        double lowlandWeightSum = 0.0;
        for (int i = 0; i < n; i++) {
            byte b = biomes[i];
            if (b == oceanOrd || b == deepOceanOrd) continue;
            float elev = elevArr[i];
            if (elev <= 0.0f) continue;
            double headwaterWeight = SmoothMath.sigmoid(elev, 150.0, 50.0);
            totalCatchmentRunoff += rainArr[i] * 0.40 * headwaterWeight;
            upstreamWeightSum += headwaterWeight;
            lowlandWeightSum += (1.0 - headwaterWeight);
        }
        double catchmentRunoffPerLowland = upstreamWeightSum > 0.0
                ? (totalCatchmentRunoff / Math.max(1.0, lowlandWeightSum))
                : 0.0;

        for (int i = 0; i < n; i++) {
            byte b = biomes[i];
            if (b == oceanOrd || b == deepOceanOrd) continue;
            float elev = elevArr[i];
            if (elev < 0.0f) continue;
            double rainfall = rainArr[i];
            double lowlandFactor = 1.0 - SmoothMath.sigmoid(elev, 120.0, 40.0);
            double effectiveFlowRainfall = rainfall + catchmentRunoffPerLowland * lowlandFactor;
            double slope = 0.0004 + 0.025 * Math.tanh(Math.max(0.0, elev) / 1800.0);
            double estimatedDepth = SmoothMath.softmax(0.5, (effectiveFlowRainfall / 1000.0) * 3.0, 4.0);
            double estimatedWidth = SmoothMath.softmax(10.0, Math.sqrt(Math.max(0.0, effectiveFlowRainfall)) * 2.5, 0.5);
            double dischargeQ = calculateDischarge(estimatedWidth, estimatedDepth, slope);
            double overbankGate = SmoothMath.sigmoid(dischargeQ, 50.0, 15.0) * lowlandFactor;
            if (overbankGate > 0.01) {
                double pop = popArr[i] / 0.06;
                double currentAgri = agriArr[i] > 0.0f ? agriArr[i] : Math.min(2000.0, Math.max(50.0, pop * 2.5));
                double floodplainFertilityBoost = SmoothMath.softmin(25.0, dischargeQ * 0.05 * dtYears, 0.2) * overbankGate;
                agriArr[i] = (float) (currentAgri + floodplainFertilityBoost);
                double alluvialWater = SmoothMath.softmin(1000.0, dischargeQ * 2.5 * overbankGate, 0.02);
                waterArr[i] = (float) Math.max(waterArr[i], alluvialWater);
            }
        }
    }
}
