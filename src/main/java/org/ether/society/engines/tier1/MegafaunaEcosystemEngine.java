/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Megafauna Steppe Ecosystem Engineering & Overkill Feedback Engine.
 * Eliminates arbitrary threshold cutoffs in favor of continuous physical state equations:
 * 1. <b>Hunting Pressure Ratio</b>: Overhunting rate = f(Population Density / Wild Biomass Density).
 * 2. <b>Continuous Biomass & Soil Organic Carbon Depletion</b>.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MegafaunaEcosystemEngine extends AbstractSimulationEngine {
    public MegafaunaEcosystemEngine() {
        super("MegafaunaEcosystemEngine", "MegafaunaEcosystemEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(MegafaunaEcosystemEngine.class);

    @Override
    public boolean supportsCellularDerivatives() { return true; }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        byte biome = buffer.getBiomes()[i];
        boolean isCryosphericSteppe = (biome == (byte) Biome.SNOW.ordinal() || biome == (byte) Biome.TUNDRA.ordinal());
        boolean isOpenGrassland = (biome == (byte) Biome.PLAINS.ordinal());
        if (!isCryosphericSteppe && !isOpenGrassland) return;
        double safeDt = Math.max(1e-4, dtYears);
        double humanPop = buffer.getBiomassHuman()[i] / 0.06;
        double naturalBiomass = buffer.getBiomassNatural()[i];
        double domesticLivestock = buffer.getBiomassLivestock()[i];
        double wildGameReliance = 1.0 / (1.0 + (domesticLivestock / Math.max(1.0, humanPop * 0.5)));
        double huntingPressureRatio = (humanPop * wildGameReliance) / Math.max(10.0, naturalBiomass);
        if (huntingPressureRatio > 0.10) {
            double stepDepletion = Math.min(0.20, Math.min(0.20, huntingPressureRatio * 0.02) * safeDt);
            double newBio = Math.max(10.0, naturalBiomass * (1.0 - stepDepletion));
            acc.dBiomassNatural += (float) (newBio - naturalBiomass);
            double soc = buffer.getSoilOrganicCarbon()[i];
            acc.dSoilOrganicCarbon += (float) (Math.max(2.0, soc - stepDepletion * 0.5) - soc);
        }
    }
}
