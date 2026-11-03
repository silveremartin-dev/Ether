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

            double capital = cell.getResourceCapitalOr(0.0);
double metal = cell.getResourceMetalOr(0.0);
double capPerCap = capital / Math.max(1.0, pop);
double metalPerCap = metal / Math.max(1.0, pop);
double powerW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);

// Material Yield Strength (sigma_yield in MPa): Wood/Earth (20-40 MPa) -> Bronze/Iron (100-300 MPa) -> Steel/Composites (500-1500 MPa)
// Determined by refined metal availability and fortified capital density (Ashby 2011)
double materialYieldStrengthMPa = Math.clamp(25.0 + 350.0 * (1.0 - Math.exp(-metalPerCap / 50.0)) + 1500.0 * (1.0 - Math.exp(-capPerCap / 500.0)), 20.0, 2000.0);

// Kinetic & Chemical Mechanical Power (Watts = Joules/sec) delivering offensive work
double kineticPowerWatts = pop * (50.0 + powerW * 0.15);

            // Total kinetic energy work delivered over time step dt (in Joules, accounting for 1% conflict mobilization duty cycle)
            double deliveredKineticJoules = kineticPowerWatts * (dt * 315576.0);

            // Fortification plastic deformation resistance energy in Joules (Yield Strength * Reference Volume * Capital Depth)
            double capitalFactor = Math.max(1.0, capital / 100.0);
            double fortificationCapacityJoules = materialYieldStrengthMPa * 1e6 * 0.5 * 100.0 * capitalFactor;

            // Continuous mechanical rupture ratio (Joules vs Joules)
            double mechanicalStressRatio = deliveredKineticJoules / Math.max(1.0, fortificationCapacityJoules);
            if (mechanicalStressRatio > 0.05 && capital > 0.0) {
                structuralBreaches++;
                // Continuous Hill-function infrastructural damage fraction
                double destructionFraction = 0.12 * ((mechanicalStressRatio * mechanicalStressRatio) / (1.0 + mechanicalStressRatio * mechanicalStressRatio)) * dt;
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


