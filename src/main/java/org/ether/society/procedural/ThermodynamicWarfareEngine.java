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
 * Thermodynamic Warfare & Kinetic Armor Penetration Engine.
 * Replaces abstract military power scores with fundamental kinetic & chemical energy delivery:
 * 1. <b>Kinetic & Chemical Energy Output (P_kinetic in MW)</b>: Total energy flux delivered per second by military forces.
 * 2. <b>Structural Penetration Threshold</b>: Armor and fortification resistance based on material yield strength (σ_yield in MPa).
 *    Penetration succeeds if Kinetic Energy Impact > Material Yield Strength * Thickness * Area.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.0.0
 */
public class ThermodynamicWarfareEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicWarfareEngine.class);

    /**
     * Executes one kinetic warfare and fortification breaching tick.
     */
    public static void processKineticWarfare(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int structuralBreaches = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // Material Yield Strength (σ_yield in MPa): Wood (20 MPa) -> Bronze (200 MPa) -> Steel (500 MPa)
            double materialYieldStrengthMPa = 20.0 + tech * 50.0;

            // Kinetic & Chemical Energy Output (Joules/sec = Watts)
            double kineticPowerOutputWatts = pop * (100.0 + Math.pow(tech, 2.5) * 500.0);

            // Fortification penetration check
            double fortificationResistanceJoules = materialYieldStrengthMPa * 1e6 * 0.5; // 0.5m thickness

            if (kineticPowerOutputWatts > fortificationResistanceJoules && capital > 1000.0) {
                structuralBreaches++;
                // Infrastructure damage proportional to excess kinetic energy
                cell.setResourceCapital(Math.max(0.0, capital - (kineticPowerOutputWatts / 1e6)));
            }
        }

        if (structuralBreaches > 0) {
            logger.info("Warfare Engine: Kinetic structural breaches evaluated across {} fortified cells.", structuralBreaches);
        }
    }
}
