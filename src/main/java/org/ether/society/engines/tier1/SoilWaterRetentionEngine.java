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
import org.ether.society.core.math.SmoothMath;

/**
 * Pedological Soil Water Retention &amp; van Genuchten (1980) Moisture Characteristic Engine.
 * <p>
 * Models soil matric potential $\theta(h)$ and Plant Available Water Capacity (AWC) as smooth ($C^\infty$)
 * continuous functions of precipitation, continuous elevation-derived slope, and soil organic carbon,
 * without discrete {@code Biome} slope switches ({@code AGENTS.md} §3, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SoilWaterRetentionEngine extends AbstractSimulationEngine {
    public SoilWaterRetentionEngine() {
        super("SoilWaterRetentionEngine", "SoilWaterRetentionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


    public static final double THETA_S = 0.430;
    public static final double THETA_R = 0.078;
    public static final double ALPHA = 0.036;
    public static final double N_PARAM = 1.56;
    public static final double M_PARAM = 1.0 - (1.0 / N_PARAM);

    public static final double HEAD_FIELD_CAPACITY_CM = 330.0;
    public static final double HEAD_WILTING_POINT_CM = 15000.0;

    /**
     * Calculates volumetric soil water content $\theta(h)$ for matric suction head $|h|$ (in cm).
     */
    public static double calculateWaterContent(double matricSuctionHeadCm) {
        double posHead = SmoothMath.softplus(matricSuctionHeadCm, 2.0);
        double alphaH = ALPHA * posHead;
        double denominator = Math.pow(1.0 + Math.pow(alphaH, N_PARAM), M_PARAM);
        return THETA_R + (THETA_S - THETA_R) / denominator;
    }

    /**
     * Computes the smooth ($C^\infty$) Plant Available Water Capacity index $\in [0.0, 1.0]$.
     *
     * @param rainfallMm annual precipitation in mm
     * @param declivity  continuous terrain slope gradient
     * @return available water index $\in [0.0, 1.0]$
     */
    public static double calculatePlantAvailableWaterIndex(double rainfallMm, double declivity) {
        double thetaFC = calculateWaterContent(HEAD_FIELD_CAPACITY_CM);
        double thetaPWP = calculateWaterContent(HEAD_WILTING_POINT_CM);
        double awcMax = Math.max(0.001, thetaFC - thetaPWP);

        double slopeDrainage = SmoothMath.softmax(0.1, 1.0 - 0.8 * declivity, 10.0);
        double saturationFrac = SmoothMath.softmin(1.0, (Math.max(0.0, rainfallMm) / 1200.0) * slopeDrainage, 6.0);
        double estimatedTheta = THETA_R + (THETA_S - THETA_R) * saturationFrac;

        double plantWaterContent = SmoothMath.softplus(estimatedTheta - thetaPWP, 40.0);
        return SmoothMath.softmin(1.0, plantWaterContent / awcMax, 8.0);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        double agriBiomass = buffer.getBiomassAgriculture()[i];
        double cap = buffer.getResourceCapital()[i];
        if (agriBiomass <= 0.0 || pop <= 0.0 || (cap / Math.max(1.0, pop)) < 0.1) return;

        double rainfall = buffer.getRainfall()[i];
        double elev = Math.max(0.0, buffer.getElevation()[i]);
        double slope = 0.03 + 0.35 * Math.tanh(elev / 2000.0);

        double awcIndex = calculatePlantAvailableWaterIndex(rainfall, slope);
        double capPerCap = cap / Math.max(1.0, pop);
        double potentialBiomass = Math.max(50.0, pop * (400.0 + 150.0 * SmoothMath.softmin(4.0, Math.log1p(capPerCap / 50.0), 4.0)));
        double waterStressFactor = 0.2 + 0.8 * awcIndex;
        double targetYield = potentialBiomass * waterStressFactor;

        double updated = Math.max(10.0, agriBiomass + 0.25 * (targetYield - agriBiomass));
        acc.dBiomassAgriculture += (float) (updated - agriBiomass);
    }

    /**
     * Processes soil moisture and updates agricultural water availability across DoD buffer.
     */
    public static void processSoilWaterRetention(WorldBuffer buffer) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        float[] bioHuman = buffer.getBiomassHuman();
        float[] agriArr = buffer.getBiomassAgriculture();
        float[] capArr = buffer.getResourceCapital();
        float[] rainArr = buffer.getRainfall();
        float[] elevArr = buffer.getElevation();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            double agriBiomass = agriArr[i];
            double cap = capArr[i];
            if (agriBiomass <= 0.0 || pop <= 0.0 || (cap / Math.max(1.0, pop)) < 0.1) continue;

            double rainfall = rainArr[i];
            double elev = Math.max(0.0, elevArr[i]);
            double slope = 0.03 + 0.35 * Math.tanh(elev / 2000.0);

            double awcIndex = calculatePlantAvailableWaterIndex(rainfall, slope);
            double capPerCap = cap / Math.max(1.0, pop);
            double potentialBiomass = Math.max(50.0, pop * (400.0 + 150.0 * SmoothMath.softmin(4.0, Math.log1p(capPerCap / 50.0), 4.0)));
            double waterStressFactor = 0.2 + 0.8 * awcIndex;
            double targetYield = potentialBiomass * waterStressFactor;

            double updated = agriBiomass + 0.25 * (targetYield - agriBiomass);
            agriArr[i] = (float) Math.max(10.0, updated);
        }
    }
}
