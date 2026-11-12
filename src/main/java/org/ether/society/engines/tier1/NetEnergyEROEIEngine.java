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

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double biomassHuman = buffer.getBiomassHuman()[i];
        double pop = biomassHuman / 0.06;
        if (pop <= 50.0) return;

        double woodBiomass = Math.max(buffer.getBiomassNatural()[i], buffer.getWoodResource()[i]);
        double resourceAvailability = Math.clamp(woodBiomass / 250.0, 0.20, 4.0);
        double capPerWorker = buffer.getResourceCapital()[i] / Math.max(1.0, pop);
        double extractionEquipmentEfficiency = 1.0 + 1.8 * Math.log1p(capPerWorker / 50.0);
        double eroei = Math.max(0.4, 2.5 * resourceAvailability * extractionEquipmentEfficiency);

        if (eroei < 1.0) {
            double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));
            double famineFraction = Math.clamp(0.05 * dtMonthlyRatio, 0.0, 0.33);
            acc.dBiomassHuman -= (float) (biomassHuman * famineFraction);
            acc.dLifespan -= (float) (1.0 * dtMonthlyRatio);
        }
    }
}
