/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Generic Hydrodynamic Maritime Highway & Coastal Trade Engine.
 * Modifies transport friction and capital accumulation based on hydrodynamic fluid efficiency
 * (Archimedes buoyancy vs dry land friction) and thermodynamic ice constraints.
 *
 * Replaces geographic latitude heuristics with universal topological and physical properties.
 * Applicable to any global, regional, or alien sea basin.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.1.0
 */
public class MaritimeHighwayEngine {
    private static final Logger logger = LoggerFactory.getLogger(MaritimeHighwayEngine.class);

    // Default physical transport parameters
    public static final double DEFAULT_CAPITAL_BOOST_RATE = 0.05; // +5% trade & capital return per tick
    public static final double DEFAULT_FRICTION_MULTIPLIER = 0.20; // 80% reduction in transport friction

    /**
     * Executes maritime transport & trade acceleration using default physical constants.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        processHybrid(cells, timeStepDays, DEFAULT_CAPITAL_BOOST_RATE, DEFAULT_FRICTION_MULTIPLIER);
    }

    /**
     * Executes maritime transport & trade acceleration with parameterizable constants.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays, double capitalBoostRate, double frictionMultiplier) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() <= 0) continue;

            // Universal physical check: coastal cell, polder, floating infrastructure, or water biome
            boolean isMaritimeBasin = cell.getIsCoastal()
                    || cell.getIsPolder()
                    || cell.getHasFloatingInfrastructure()
                    || cell.getBiome() == Biome.OCEAN
                    || cell.getBiome() == Biome.DEEP_OCEAN
                    || (cell.getElevation() != null && cell.getElevation() <= 0);

            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            if (isMaritimeBasin && tempC > 0.0) {
                // Coastal & maritime trade acceleration: buoyancy eliminates solid dry friction
                double currentCap = cell.getResourceCapital();
                cell.setResourceCapital(currentCap * (1.0 + capitalBoostRate));

                // Hydrodynamic friction reduction over open ice-free water
                double friction = cell.getMovementFriction();
                cell.setMovementFriction(Math.max(0.1, friction * frictionMultiplier));
            } else if (isMaritimeBasin && tempC <= 0.0) {
                // Sub-zero sea ice increases friction for maritime navigation
                double friction = cell.getMovementFriction();
                cell.setMovementFriction(Math.min(20.0, friction * 1.50));
            }
        }
    }
}
