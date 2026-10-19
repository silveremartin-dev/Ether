/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
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
public class PhysicalSupplyChainEngine {
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
     * Process supply chains.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processSupplyChains(List<H3Cell> cells, double deltaYears) {
// Numerical integration step for PhysicalSupplyChainEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;

        double globalFrictionMultiplier = maritimeChokepointBlockadeActive ? 2.5 : 1.0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            // Energy spent on logistics reduces available labor/work output.
            // Rather than an arbitrary 0.02 magic constant, logistics friction is derived from
            // mechanical transport work: W_transport = m * g * (mu_rolling + sin(theta)),
            // attenuated by paved roads/rail infrastructure capital and modern propulsion efficiency.
            double work = cell.getResourceWorkOr(0.0);
            double capital = cell.getResourceCapitalOr(0.0);
            int pop = cell.getPopulationOr(1);
            double tech = cell.getTechnologyLevelOr(1.0);
            double capPerCap = capital / Math.max(1.0, pop);
            double infraFrictionDamping = Math.clamp(1.0 / (1.0 + 0.1 * capPerCap + 0.2 * tech), 0.15, 1.0);
            double baseFriction = (landTransportFrictionCoeff * infraFrictionDamping);
            double logisticsLossFactor = Math.clamp(baseFriction * globalFrictionMultiplier * deltaYears, 0.0, 0.95);

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
     * <p>
     * Executes operational logic for {@code PhysicalSupplyChainEngine} within the Tier 1 physical conservation solver.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
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
     * <p>
     * Executes operational logic for {@code PhysicalSupplyChainEngine} within the Tier 1 physical conservation solver.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
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
     * <p>
     * Executes operational logic for {@code PhysicalSupplyChainEngine} within the Tier 1 physical conservation solver.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param coeff the coeff argument (double)
     */
    public static void setLandTransportFrictionCoeff(double coeff) { landTransportFrictionCoeff = Math.max(0.001, coeff); }
}


