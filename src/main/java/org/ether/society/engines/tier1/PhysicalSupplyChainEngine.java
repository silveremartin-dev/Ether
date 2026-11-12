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

    /**
     * Process supply chains across spatial cells with physical friction and infrastructure damping.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears time step in years (double)
     */
    public static void processSupplyChains(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            // Physical logistics friction: differentiate between coastal/maritime corridors vs inland overland freight
            boolean isMaritime = Boolean.TRUE.equals(cell.getIsCoastal())
                    || cell.getBiome() == Biome.BEACH
                    || cell.getBiome() == Biome.OCEAN
                    || cell.getBiome() == Biome.DEEP_OCEAN
                    || cell.getBiome() == Biome.LAKE;

            // Hydrodynamic sea transport is an order of magnitude more energy-efficient than overland cartage (Smil 2017)
            double baseFrictionCoeff = isMaritime ? seaTransportFrictionCoeff : landTransportFrictionCoeff;

            // Maritime chokepoint blockade disproportionately disrupts maritime sea lines of communication (SLOC)
            // Overland routes experience secondary diversion strain
            double blockadeMultiplier = maritimeChokepointBlockadeActive ? (isMaritime ? 2.5 : 1.25) : 1.0;

            // Infrastructure capital (paved roads, canals, railroads) dampens mechanical transport resistance
            double work = cell.getResourceWorkOr(0.0);
            double capital = cell.getResourceCapitalOr(0.0);
            int pop = cell.getPopulationOr(1);
            double capPerCap = capital / Math.max(1.0, pop);
            double infraFrictionDamping = Math.clamp(1.0 / (1.0 + 0.25 * capPerCap), 0.15, 1.0);

            double effectiveFriction = baseFrictionCoeff * blockadeMultiplier * infraFrictionDamping;
            double logisticsLossFactor = Math.clamp(effectiveFriction * deltaYears, 0.0, 0.95);

            cell.setResourceWork(Math.max(0.0, work * (1.0 - logisticsLossFactor)));
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
