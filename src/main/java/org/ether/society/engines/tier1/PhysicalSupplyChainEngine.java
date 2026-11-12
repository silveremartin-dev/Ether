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
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Physical Supply Chain & Transport Bottleneck Engine.
 * Models transport friction (mu_sea vs mu_rail), physical commodity flows (Lithium, Rare Earths, Oil),
 * and supply chain cascade disruptions at maritime chokepoints.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicalSupplyChainEngine extends AbstractSimulationEngine {
    public PhysicalSupplyChainEngine() {
        super("PhysicalSupplyChainEngine", "PhysicalSupplyChainEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(PhysicalSupplyChainEngine.class);

    /** Internal state: maritime chokepoint blockade active (boolean). */
    private static boolean maritimeChokepointBlockadeActive = false;
    private static double seaTransportFrictionCoeff = 0.001; // Hydrodynamic friction coeff
    private static double landTransportFrictionCoeff = 0.05;  // Overland friction coeff

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        maritimeChokepointBlockadeActive = false;
        seaTransportFrictionCoeff = 0.001;
        landTransportFrictionCoeff = 0.05;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        byte biomeOrd = buffer.getBiomes()[i];
        boolean isMaritime = biomeOrd == (byte) Biome.BEACH.ordinal()
                || biomeOrd == (byte) Biome.OCEAN.ordinal()
                || biomeOrd == (byte) Biome.DEEP_OCEAN.ordinal()
                || biomeOrd == (byte) Biome.LAKE.ordinal();

        double baseFrictionCoeff = isMaritime ? seaTransportFrictionCoeff : landTransportFrictionCoeff;
        double blockadeMultiplier = maritimeChokepointBlockadeActive ? (isMaritime ? 2.5 : 1.25) : 1.0;

        double capital = buffer.getResourceCapital()[i];
        double capPerCap = capital / Math.max(1.0, pop);
        double infraFrictionDamping = Math.clamp(1.0 / (1.0 + 0.25 * capPerCap), 0.15, 1.0);

        double effectiveFriction = baseFrictionCoeff * blockadeMultiplier * infraFrictionDamping;
        double logisticsLossFactor = Math.clamp(effectiveFriction * dtYears, 0.0, 0.95);

        double energyFire = buffer.getEnergyFire()[i];
        acc.dEnergyFire -= (float) (energyFire * logisticsLossFactor);
    }

    /**
     * Process supply chains across DoD WorldBuffer with physical friction and infrastructure damping.
     *
     * @param buffer WorldBuffer DoD state
     * @param deltaYears time step in years (double)
     */
    public static void processSupplyChains(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        float[] bioHuman = buffer.getBiomassHuman();
        byte[] biomes = buffer.getBiomes();
        float[] capArr = buffer.getResourceCapital();
        float[] energyFireArr = buffer.getEnergyFire();
        byte beachOrd = (byte) Biome.BEACH.ordinal();
        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte lakeOrd = (byte) Biome.LAKE.ordinal();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 0.0) continue;

            byte biomeOrd = biomes[i];
            boolean isMaritime = biomeOrd == beachOrd
                    || biomeOrd == oceanOrd
                    || biomeOrd == deepOceanOrd
                    || biomeOrd == lakeOrd;

            double baseFrictionCoeff = isMaritime ? seaTransportFrictionCoeff : landTransportFrictionCoeff;
            double blockadeMultiplier = maritimeChokepointBlockadeActive ? (isMaritime ? 2.5 : 1.25) : 1.0;

            double capital = capArr[i];
            double capPerCap = capital / Math.max(1.0, pop);
            double infraFrictionDamping = Math.clamp(1.0 / (1.0 + 0.25 * capPerCap), 0.15, 1.0);

            double effectiveFriction = baseFrictionCoeff * blockadeMultiplier * infraFrictionDamping;
            double logisticsLossFactor = Math.clamp(effectiveFriction * deltaYears, 0.0, 0.95);

            energyFireArr[i] = (float) Math.max(0.0, energyFireArr[i] * (1.0 - logisticsLossFactor));
        }
    }

    // Getters and Setters
    /**
     * Is maritime chokepoint blockade active.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isMaritimeChokepointBlockadeActive() { return maritimeChokepointBlockadeActive; }

    /**
     * Set maritime chokepoint blockade active operation.
     *
     * @param blocked the blocked argument (boolean)
     */
    public static void setMaritimeChokepointBlockadeActive(boolean blocked) { maritimeChokepointBlockadeActive = blocked; }

    /**
     * Get sea transport friction coeff.
     *
     * @return the resulting computation or state reference
     */
    public static double getSeaTransportFrictionCoeff() { return seaTransportFrictionCoeff; }

    /**
     * Set sea transport friction coeff operation.
     *
     * @param coeff the coeff argument (double)
     */
    public static void setSeaTransportFrictionCoeff(double coeff) { seaTransportFrictionCoeff = Math.max(0.0001, coeff); }

    /**
     * Get land transport friction coeff.
     *
     * @return the resulting computation or state reference
     */
    public static double getLandTransportFrictionCoeff() { return landTransportFrictionCoeff; }

    /**
     * Set land transport friction coeff operation.
     *
     * @param coeff the coeff argument (double)
     */
    public static void setLandTransportFrictionCoeff(double coeff) { landTransportFrictionCoeff = Math.max(0.001, coeff); }
}
