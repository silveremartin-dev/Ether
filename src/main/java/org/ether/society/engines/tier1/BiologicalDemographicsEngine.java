/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;

import java.util.List;

/**
 * Gompertz-Makeham Biological Actuarial Mortality Engine.
 * <p>
 * Models actuarial cellular senescence ($\alpha e^{\beta x}$) and smooth ($C^\infty$) environmental
 * physiological hazards (high-altitude hypobaric hypoxia and extreme thermal cold stress), updating
 * Lotka age pyramids directly from conserved demographic observables ({@code AGENTS.md} Â§10.1, Â§10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BiologicalDemographicsEngine extends AbstractSimulationEngine {
    public BiologicalDemographicsEngine() {
        super("BiologicalDemographicsEngine", "BiologicalDemographicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


    /** Baseline actuarial aging constant $\alpha$. */
    public static final double GOMPERTZ_ALPHA = 0.0001;

    /** Cellular senescence rate $\beta$. */
    public static final double GOMPERTZ_BETA = 0.08;

    /**
     * Calculates Gompertz-Makeham hazard rate $\mu(x) = \alpha e^{\beta x} + \gamma$ for age $x$.
     *
     * @param ageYears                 age in years
     * @param environmentalHazardGamma baseline extrinsic environmental hazard $\gamma$
     * @return mortality hazard rate $\mu(x)$ in $\text{yr}^{-1}$
     */
    public static double calculateGompertzHazardRate(double ageYears, double environmentalHazardGamma) {
        return GOMPERTZ_ALPHA * Math.exp(GOMPERTZ_BETA * ageYears) + environmentalHazardGamma;
    }

    /**
     * Executes one biological demographic mortality tick across cells over {@code deltaYears}.
     */
    public static void processBiologicalDemographics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double elev = cell.getElevationOr(0.0);
            double tempC = cell.getTemperatureOr(15.0);

            // Altitude Hypoxia Hazard (barometric PO2 drop above ~3000 m, West 2012)
            double epas1Adaptation = cell.getMovementFriction() != null
                    ? Math.clamp(1.0 - (cell.getMovementFriction() / 3.0), 0.0, 1.0)
                    : 0.05;
            double excessElevKm = Math.max(0.0, (elev - 3000.0) / 1000.0);
            double altitudeVulnerability = Math.max(0.10, 1.0 - epas1Adaptation * 0.85);
            double hypoxiaHazard = excessElevKm * 0.08 * altitudeVulnerability;

            // Extreme Cold Hazard (hypothermia / frostbite below -10A°C, mitigated by shelter capital)
            double capPerCap = cell.getResourceCapitalOr(0.0) / Math.max(1.0, pop);
            double shelterProtection = 1.0 - 0.70 * Math.tanh(capPerCap / 150.0);
            double subZeroExcess = Math.max(0.0, -10.0 - tempC);
            double coldHazard = Math.min(0.25, subZeroExcess * 0.012 * shelterProtection);

            double environmentalHazard = hypoxiaHazard + coldHazard;
            if (environmentalHazard > 1e-6) {
                double survivalRate = Math.exp(-environmentalHazard * dt);
                int survivingPop = (int) Math.round(pop * survivalRate);
                org.ether.society.database.H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, survivingPop));
                pop = cell.getPopulationOr(0);
            }

            if (pop > 0) {
                cell.updateAgePyramidFromObservables();
            }
        }
    }

    /**
     * Executes one monthly biological demographic mortality tick across cells.
     */
    public static void processBiologicalDemographics(List<H3Cell> cells) {
        processBiologicalDemographics(cells, 30.0 / 365.25);
    }
}
