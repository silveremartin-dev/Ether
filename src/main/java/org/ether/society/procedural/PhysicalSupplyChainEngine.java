/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 * @version 4.0.0
 */
public class PhysicalSupplyChainEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalSupplyChainEngine.class);

    private static boolean maritimeChokepointBlockadeActive = false;
    private static double seaTransportFrictionCoeff = 0.001; // Hydrodynamic friction coeff
    private static double landTransportFrictionCoeff = 0.05;  // Overland friction coeff

    public static void processSupplyChains(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double globalFrictionMultiplier = maritimeChokepointBlockadeActive ? 2.5 : 1.0;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            // Energy spent on logistics reduces available labor/work output
            double work = cell.getResourceWork() != null ? cell.getResourceWork() : 0.0;
            double logisticsLossFactor = 0.02 * globalFrictionMultiplier * deltaYears;

            cell.setResourceWork(Math.max(0.0, work * (1.0 - logisticsLossFactor)));
        }
    }

    // Getters and Setters
    public static boolean isMaritimeChokepointBlockadeActive() { return maritimeChokepointBlockadeActive; }
    public static void setMaritimeChokepointBlockadeActive(boolean blocked) { maritimeChokepointBlockadeActive = blocked; }

    public static double getSeaTransportFrictionCoeff() { return seaTransportFrictionCoeff; }
    public static void setSeaTransportFrictionCoeff(double coeff) { seaTransportFrictionCoeff = Math.max(0.0001, coeff); }

    public static double getLandTransportFrictionCoeff() { return landTransportFrictionCoeff; }
    public static void setLandTransportFrictionCoeff(double coeff) { landTransportFrictionCoeff = Math.max(0.001, coeff); }
}
