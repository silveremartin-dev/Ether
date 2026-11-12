/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thermodynamic Warfare & Kinetic Armor Penetration Engine.
 * Replaces abstract military power scores with fundamental kinetic & chemical energy delivery:
 * 1. <b>Kinetic & Chemical Energy Output (P_kinetic in MW)</b>: Total energy flux delivered per second by military forces.
 * 2. <b>Structural Penetration Threshold</b>: Armor and fortification resistance based on material yield strength (Ïƒ_yield in MPa).
 *    Penetration succeeds if Kinetic Energy Impact > Material Yield Strength * Thickness * Area.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ThermodynamicWarfareEngine extends AbstractSimulationEngine {
    public ThermodynamicWarfareEngine() {
        super("ThermodynamicWarfareEngine", "ThermodynamicWarfareEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicWarfareEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double dt = Math.max(0.001, dtYears);
        double capital = buffer.getResourceCapital()[i];
        double metal = buffer.getMetalResource()[i];
        double capPerCap = capital / Math.max(1.0, pop);
        double metalPerCap = metal / Math.max(1.0, pop);
        double powerW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(buffer, i);

        double materialYieldStrengthMPa = Math.clamp(25.0 + 350.0 * (1.0 - Math.exp(-metalPerCap / 50.0)) + 1500.0 * (1.0 - Math.exp(-capPerCap / 500.0)), 20.0, 2000.0);
        double kineticPowerWatts = pop * (50.0 + powerW * 0.15);
        double deliveredKineticJoules = kineticPowerWatts * (dt * 315576.0);
        double capitalFactor = Math.max(1.0, capital / 100.0);
        double fortificationCapacityJoules = materialYieldStrengthMPa * 1e6 * 0.5 * 100.0 * capitalFactor;
        double mechanicalStressRatio = deliveredKineticJoules / Math.max(1.0, fortificationCapacityJoules);

        if (mechanicalStressRatio > 0.05 && capital > 0.0) {
            double destructionFraction = 0.12 * ((mechanicalStressRatio * mechanicalStressRatio) / (1.0 + mechanicalStressRatio * mechanicalStressRatio)) * dt;
            acc.dResourceCapital -= (float) (capital * destructionFraction);
        }
    }

    /**
     * Executes one kinetic warfare and fortification breaching tick with physical time integration across DoD buffer.
     */
    public static void processKineticWarfare(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        double dt = Math.max(0.001, deltaYears);
        int structuralBreaches = 0;

        float[] bioHuman = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] metalArr = buffer.getMetalResource();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 0.0) continue;

            double capital = capArr[i];
            double metal = metalArr[i];
            double capPerCap = capital / Math.max(1.0, pop);
            double metalPerCap = metal / Math.max(1.0, pop);
            double powerW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(buffer, i);

            double materialYieldStrengthMPa = Math.clamp(25.0 + 350.0 * (1.0 - Math.exp(-metalPerCap / 50.0)) + 1500.0 * (1.0 - Math.exp(-capPerCap / 500.0)), 20.0, 2000.0);
            double kineticPowerWatts = pop * (50.0 + powerW * 0.15);
            double deliveredKineticJoules = kineticPowerWatts * (dt * 315576.0);
            double capitalFactor = Math.max(1.0, capital / 100.0);
            double fortificationCapacityJoules = materialYieldStrengthMPa * 1e6 * 0.5 * 100.0 * capitalFactor;

            double mechanicalStressRatio = deliveredKineticJoules / Math.max(1.0, fortificationCapacityJoules);
            if (mechanicalStressRatio > 0.05 && capital > 0.0) {
                structuralBreaches++;
                double destructionFraction = 0.12 * ((mechanicalStressRatio * mechanicalStressRatio) / (1.0 + mechanicalStressRatio * mechanicalStressRatio)) * dt;
                capArr[i] = (float) Math.max(0.0, capital * (1.0 - destructionFraction));
            }
        }

        if (structuralBreaches > 0) {
            logger.debug("Warfare Engine: Kinetic structural breaches evaluated across {} fortified cells.", structuralBreaches);
        }
    }

    public static void processKineticWarfare(WorldBuffer buffer) {
        processKineticWarfare(buffer, 30.0 / 365.25);
    }
}


