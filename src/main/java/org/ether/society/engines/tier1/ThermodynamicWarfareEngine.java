/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalUnitsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thermodynamic Warfare &amp; Kinetic Armor Penetration Engine.
 * <p>
 * Models kinetic and chemical ordnance energy dissipation against structural fortification
 * yield strength (\(\sigma_{\text{yield}}\) in \(\text{MPa}\)) during active socio-political strife
 * (Lanchester 1916; Richardson 1960; Dupuy 1987).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ThermodynamicWarfareEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicWarfareEngine.class);

    /**
     * Constructs the thermodynamic warfare engine.
     */
    public ThermodynamicWarfareEngine() {
        super("ThermodynamicWarfareEngine", "ThermodynamicWarfareEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null || dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0) return;

        double capital = buffer.getResourceCapital()[i];
        if (capital <= 0.0) return;

        // Smooth C^infty active strife mobilization gate: peaceful societies with low inequality and stable institutions
        // do not expend kinetic siege ordnance against their own infrastructure.
        double gini = buffer.getGiniIndex() != null ? buffer.getGiniIndex()[i] : 0.35;
        double complexity = buffer.getInstitutionalComplexity() != null ? buffer.getInstitutionalComplexity()[i] : 1.0;
        double strifeGate = SmoothMath.sigmoid(gini, 0.52, 0.05) * SmoothMath.sigmoid(1.2 - complexity, 0.0, 0.40);
        if (strifeGate < 1.0e-4) return;

        double dt = Math.max(0.0001, dtYears);
        double metal = buffer.getMetalResource()[i];
        double capPerCap = capital / Math.max(1.0, pop);
        double metalPerCap = metal / Math.max(1.0, pop);
        double powerW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(buffer, i);

        double materialYieldStrengthMPa = Math.clamp(
                25.0 + 350.0 * (1.0 - Math.exp(-metalPerCap / 50.0)) + 1500.0 * (1.0 - Math.exp(-capPerCap / 500.0)),
                20.0, 2000.0);
        double kineticPowerWatts = pop * strifeGate * (50.0 + powerW * 0.15);
        double campaignDurationSeconds = dt * PhysicalUnitsHelper.SECONDS_PER_JULIAN_YEAR * 0.01;
        double deliveredKineticJoules = kineticPowerWatts * campaignDurationSeconds;
        double capitalFactor = Math.max(1.0, capital / 100.0);
        double fortificationCapacityJoules = materialYieldStrengthMPa * 1.0e6 * 0.5 * 100.0 * capitalFactor;
        double mechanicalStressRatio = deliveredKineticJoules / Math.max(1.0, fortificationCapacityJoules);

        if (mechanicalStressRatio > 0.05) {
            double destructionFraction = 0.12 * ((mechanicalStressRatio * mechanicalStressRatio)
                    / (1.0 + mechanicalStressRatio * mechanicalStressRatio)) * dt;
            acc.dResourceCapital -= (float) (capital * destructionFraction);
        }
    }

    /**
     * Executes one kinetic warfare and fortification breaching tick with physical time integration across DoD buffer.
     *
     * @param buffer     target DoD world buffer
     * @param deltaYears time step in Julian years
     */
    public static void processKineticWarfare(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0 || deltaYears <= 0.0) return;

        ThermodynamicWarfareEngine instance = new ThermodynamicWarfareEngine();
        DerivativeAccumulator acc = new DerivativeAccumulator();
        int n = buffer.getCapacity();
        int structuralBreaches = 0;

        for (int i = 0; i < n; i++) {
            acc.clear();
            instance.accumulateCellularDerivatives(buffer, i, deltaYears, deltaYears * 12.0, acc);
            if (acc.dResourceCapital < 0.0f) {
                structuralBreaches++;
                buffer.getResourceCapital()[i] = Math.max(0.0f, buffer.getResourceCapital()[i] + acc.dResourceCapital);
            }
        }

        if (structuralBreaches > 0) {
            logger.debug("Warfare Engine: Kinetic structural breaches evaluated across {} fortified cells.", structuralBreaches);
        }
    }

    /**
     * Executes one default monthly kinetic warfare pass.
     *
     * @param buffer target DoD world buffer
     */
    public static void processKineticWarfare(WorldBuffer buffer) {
        processKineticWarfare(buffer, 1.0 / 12.0);
    }
}
