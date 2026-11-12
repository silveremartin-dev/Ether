/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Net Energy Return on Investment (EROEI) Master Cliodynamic Engine.
 * EROEI = Energy Extracted / Energy Expended to Extract.
 * Models the fundamental macro-variable driving civilizational growth, complexity, and collapse:
 * 1. <b>High EROEI (> 10:1)</b>: Enables urban specialization, scientific research, and complex institutions.
 * 2. <b>Low EROEI (1:1 to 3:1)</b>: Agrarian subsistence trap (90% of population must farm).
 * 3. <b>Negative EROEI (< 1:1)</b>: Immediate societal collapse, famine, and fragmentation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NetEnergyEROEIEngine extends AbstractSimulationEngine {
    public NetEnergyEROEIEngine() {
        super("NetEnergyEROEIEngine", "NetEnergyEROEIEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(NetEnergyEROEIEngine.class);

    /**
     * Calculates Net Energy Return on Investment (EROEI) for a cell.
     *
     * @param cell H3 terrain cell
     * @return EROEI ratio (e.g. 0.5 to 30.0)
     */
    public static double calculateEROEI(H3Cell cell) {
        if (cell == null) return 1.1;

        double areaScale = Math.max(0.05, cell.getEffectiveSurfaceAreaKm2() / 1770.0);
double woodBiomass = cell.getBiomassNaturalOr(0.0);
if (cell.getWoodResource() != null && cell.getWoodResource() > woodBiomass) {
    woodBiomass = cell.getWoodResource();
}

// Physical EROEI: ratio of harvestable exergy flux to harvesting work (Hall et al. 2014)
// Subsistence foragers/agrarians: 3:1 to 15:1; Industrial: 20:1 to 40:1; Severe resource depletion: < 1.0
double resourceAvailability = Math.clamp(woodBiomass / (250.0 * areaScale), 0.20, 4.0);
double capPerWorker = cell.getResourceCapitalOr(0.0) / Math.max(1.0, cell.getPopulationOr(1));
double extractionEquipmentEfficiency = 1.0 + 1.8 * Math.log1p(capPerWorker / 50.0);
double eroei = 2.5 * resourceAvailability * extractionEquipmentEfficiency;

        return Math.max(0.4, eroei);
    }

    /**
     * Executes one master EROEI cliodynamic tick across cells using default monthly step.
     */
    public static void processNetEnergyEROEI(List<H3Cell> cells) {
        processNetEnergyEROEI(cells, 1.0 / 12.0);
    }

    /**
     * Executes one master EROEI cliodynamic tick across cells scaled by dtYears.
     */
    public static void processNetEnergyEROEI(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        int collapseCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double eroei = calculateEROEI(cell);
            int pop = cell.getPopulationOr(0);

            // EROEI < 1.0 triggers societal energy deficit and demographic crisis
            if (eroei < 1.0 && pop > 50) {
                collapseCells++;
                int famineDeaths = (int) Math.clamp(pop * 0.05 * dtMonthlyRatio, 1, pop / 3);
                int targetPop = Math.max(0, pop - famineDeaths);
                H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, targetPop);
                cell.setBiomassHuman((double) targetPop);
                double curLife = cell.getLifespanOr(40.0);
                cell.setLifespan(Math.max(15.0, curLife - 1.0 * dtMonthlyRatio));
            }
        }

        if (collapseCells > 0) {
            logger.warn("EROEI Engine: Civilizational energy deficit collapse active across {} cells.", collapseCells);
        }
    }
}
