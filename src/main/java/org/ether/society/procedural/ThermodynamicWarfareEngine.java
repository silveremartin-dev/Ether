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
 * @version 1.0.0-beta.1
 */
public class ThermodynamicWarfareEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicWarfareEngine.class);

    /**
     * Executes one kinetic warfare and fortification breaching tick with physical time integration.
     */
    public static void processKineticWarfare(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);
        int structuralBreaches = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // Material Yield Strength (σ_yield in MPa): Wood (20 MPa) -> Bronze (200 MPa) -> Steel (500 MPa) -> Composites (1200+ MPa)
            double materialYieldStrengthMPa = Math.clamp(20.0 + tech * 15.0, 20.0, 2000.0);

            // Kinetic & Chemical Energy Output (Joules/sec = Watts) per mobilized force
            double kineticPowerOutputWatts = pop * (50.0 + Math.pow(Math.min(tech, 150.0), 2.2) * 200.0);

            // Fortification structural resistance in Joules (Yield Strength * Thickness * Reference Cross Section)
            double fortificationResistanceJoules = materialYieldStrengthMPa * 1e6 * 0.5; // 0.5m barrier thickness

            if (kineticPowerOutputWatts > fortificationResistanceJoules && capital > 100.0) {
                structuralBreaches++;
                // Infrastructure damage proportional to delivered kinetic work: E_kinetic = P_kinetic * dt
                double kineticEnergyDamageMegaJoules = (kineticPowerOutputWatts / 1e6) * dt;
                cell.setResourceCapital(Math.max(0.0, capital - kineticEnergyDamageMegaJoules));
            }
        }

        if (structuralBreaches > 0) {
            logger.debug("Warfare Engine: Kinetic structural breaches evaluated across {} fortified cells.", structuralBreaches);
        }
    }

    public static void processKineticWarfare(List<H3Cell> cells) {
        processKineticWarfare(cells, 30.0 / 365.25);
    }
}

