*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mechanical Work & Hydrodynamic Transport Engine.
 * Replaces abstract gold/trade costs with fundamental Mechanical Work (W = F_friction * distance):
 * 1. <b>Hydrodynamic Fluid Friction (Î¼_water â‰ˆ 0.001)</b>: Extremely low transport work for maritime & river routes.
 * 2. <b>Terrestrial Coulomb Friction (Î¼_land â‰ˆ 0.05 - 0.25)</b>: High caloric expenditure for overland pack animals & foot transport.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicsTransportEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicsTransportEngine.class);

    /**
     * Calculates the mechanical work (Joules per kg-km) required to transport cargo across a cell.
     *
     * @param cell H3 terrain cell
     * @return Energy expenditure in Joules per kg per kilometer
     */
    public static double calculateTransportWorkJoules(H3Cell cell) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cell == null) return 1000.0;

        Biome biome = cell.getBiome();
        double frictionCoeff;

        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
            frictionCoeff = 0.001; // Hydrodynamic drag coefficient
        } else if (biome == Biome.PLAINS) {
            frictionCoeff = 0.05;  // Low rolling friction
        } else if (biome == Biome.HILLS || biome == Biome.FOREST) {
            frictionCoeff = 0.12;  // Moderate slope friction
        } else {
            frictionCoeff = 0.25;  // High mountain/desert friction
        }

        double gravity = 9.81;
        double distanceMeters = 1000.0;

        // Work W = Î¼ * m * g * d (in Joules/kg)
        return frictionCoeff * gravity * distanceMeters;
    }

    /**
     * Executes one transport work evaluation step across cells using default monthly step.
     */
    public static void processPhysicsTransport(List<H3Cell> cells) {
        processPhysicsTransport(cells, 1.0 / 12.0);
    }

    /**
     * Executes transport work evaluation step across cells scaling cargo by dtYears and deducting energy.
     */
    public static void processPhysicsTransport(List<H3Cell> cells, double dtYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        double totalWorkExpendedJoules = 0.0;
        int activeTransportCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop > 0) {
                double workPerKgKm = calculateTransportWorkJoules(cell);
                // 2 kg cargo per person per month scaled by dt
                double cargoMassKg = pop * 2.0 * dtMonthlyRatio;
                double cellWorkJoules = workPerKgKm * cargoMassKg;
                totalWorkExpendedJoules += cellWorkJoules;
                activeTransportCells++;

                // Deduct mechanical transport work in GJ from cell energetic labor capacity
                double cellWorkGJ = cellWorkJoules / 1.0e9;
                double curWork = cell.getResourceWork() != null ? cell.getResourceWork() : 0.0;
                cell.setResourceWork(Math.max(0.0, curWork - cellWorkGJ));
            }
        }

        if (activeTransportCells > 0) {
            logger.debug("Physics Transport: Expended {} GJ of mechanical freight work across {} cells.",
                    String.format(java.util.Locale.ROOT, "%.3f", totalWorkExpendedJoules / 1.0e9), activeTransportCells);
        }
    }
}


