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
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Deforestation Soil Erosion & Root Retention Loss Engine.
 * Models:
 * 1. <b>Root Retention Loss</b>: Clear-cutting timber biomass removes root soil binding.
 * 2. <b>Runoff Soil Carbon Washout</b>: Rainfall on deforested slopes washes away soil organic carbon (SOC).
 * 3. <b>Arid Biome Degradation</b>: Severe SOC loss converts fertile forest/hills into barren desert terrain.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class DeforestationErosionEngine extends AbstractSimulationEngine {
    public DeforestationErosionEngine() {
        super("DeforestationErosionEngine", "DeforestationErosionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(DeforestationErosionEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));
        double effectiveCanopy = Math.max(buffer.getBiomassNatural()[i], buffer.getWoodResource()[i]);
        double soc = buffer.getSoilOrganicCarbon()[i];
        double rain = buffer.getRainfall()[i];
        double elev = buffer.getElevation()[i];

        if (effectiveCanopy < 100.0 && elev > 200.0 && rain > 400.0 && soc > 2.0) {
            double socLoss = (rain / 1000.0) * 0.5 * dtMonthlyRatio;
            double newSoc = Math.max(2.0, soc - socLoss);
            acc.dSoilOrganicCarbon += (float) (newSoc - soc);
        }
    }
}
