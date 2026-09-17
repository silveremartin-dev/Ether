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
 * Mechanical Work & Hydrodynamic Transport Engine.
 * Replaces abstract gold/trade costs with fundamental Mechanical Work (W = F_friction * distance):
 * 1. <b>Hydrodynamic Fluid Friction (μ_water ≈ 0.001)</b>: Extremely low transport work for maritime & river routes.
 * 2. <b>Terrestrial Coulomb Friction (μ_land ≈ 0.05 - 0.25)</b>: High caloric expenditure for overland pack animals & foot transport.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
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

        // Work W = μ * m * g * d (in Joules/kg)
        return frictionCoeff * gravity * distanceMeters;
    }

    /**
     * Executes one transport work evaluation step across trade networks.
     */
    public static void processPhysicsTransport(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double totalWorkExpendedJoules = 0.0;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                double workPerKgKm = calculateTransportWorkJoules(cell);
                double cargoMassKg = cell.getPopulation() * 2.0; // 2kg cargo per person per tick
                totalWorkExpendedJoules += workPerKgKm * cargoMassKg;
            }
        }
    }
}

