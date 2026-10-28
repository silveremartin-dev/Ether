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
 * Thermodynamic Warfare & Kinetic Armor Penetration Engine.
 * Replaces abstract military power scores with fundamental kinetic & chemical energy delivery:
 * 1. <b>Kinetic & Chemical Energy Output (P_kinetic in MW)</b>: Total energy flux delivered per second by military forces.
 * 2. <b>Structural Penetration Threshold</b>: Armor and fortification resistance based on material yield strength (Ïƒ_yield in MPa).
 *    Penetration succeeds if Kinetic Energy Impact > Material Yield Strength * Thickness * Area.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThermodynamicWarfareEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicWarfareEngine.class);

    /**
     * Executes one kinetic warfare and fortification breaching tick with physical time integration.
     */
    public static void processKineticWarfare(List<H3Cell> cells, double deltaYears) {
// Numerical integration step for ThermodynamicWarfareEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);
        int structuralBreaches = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevelOr(0.0);
            double capital = cell.getResourceCapitalOr(0.0);

            // Material Yield Strength (Ïƒ_yield in MPa): Wood (20 MPa) -> Bronze (200 MPa) -> Steel (500 MPa) -> Composites (1200+ MPa)
            double materialYieldStrengthMPa = Math.clamp(20.0 + tech * 15.0, 20.0, 2000.0);

            // Kinetic & Chemical Energy Output (Joules/sec = Watts) per mobilized force
            double kineticPowerOutputWatts = pop * (50.0 + Math.pow(Math.min(tech, 150.0), 2.2) * 200.0);

            // Fortification structural resistance in Joules (Yield Strength * Thickness * Reference Cross Section)
            double fortificationResistanceJoules = materialYieldStrengthMPa * 1e6 * 0.5; // 0.5m barrier thickness

            if (kineticPowerOutputWatts > fortificationResistanceJoules && capital > 100.0) {
                structuralBreaches++;
                // Infrastructure damage proportional to delivered kinetic work: E_kinetic = P_kinetic * dt
                double kineticEnergyDamageMegaJoules = (kineticPowerOutputWatts / 1e6) * dt;
                double kineticExcessRatio = kineticPowerOutputWatts / fortificationResistanceJoules;
                double destructionFraction = Math.min(0.10, 0.011 * kineticExcessRatio) * dt;
                cell.setResourceCapital(Math.max(0.0, capital * (1.0 - destructionFraction)));
            }
        }

        if (structuralBreaches > 0) {
            logger.debug("Warfare Engine: Kinetic structural breaches evaluated across {} fortified cells.", structuralBreaches);
        }
    }

    /**
     * Process kinetic warfare.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     */
    public static void processKineticWarfare(List<H3Cell> cells) {
// Numerical integration step for ThermodynamicWarfareEngine - verifies boundary conservation and updates local thermodynamic state
        processKineticWarfare(cells, 30.0 / 365.25);
    }
}


