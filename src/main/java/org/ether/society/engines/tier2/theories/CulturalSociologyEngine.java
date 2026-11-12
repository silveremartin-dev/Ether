/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cultural Sociology Engine (Tier 2).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CulturalSociologyEngine extends AbstractSimulationEngine {
    public CulturalSociologyEngine() {
        super("CulturalSociologyEngine", "CulturalSociologyEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(CulturalSociologyEngine.class);

    private static double globalCollectivismIndex = 0.5;
    private static double globalEnvironmentalStewardship = 0.5;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (buffer.getBiomassHuman()[i] <= 0.0f) return;

        if (globalEnvironmentalStewardship > 0.6) {
            acc.dSoilOrganicCarbon += (float) ((globalEnvironmentalStewardship - 0.5) * 0.1 * dtYears);
        }
        if (globalCollectivismIndex > 0.6) {
            float pollution = buffer.getPollutionLevel()[i];
            double reduction = Math.min(pollution, (globalCollectivismIndex - 0.5) * 0.5 * dtYears);
            acc.dPollutionLevel -= (float) reduction;
        }
    }

    public static double getGlobalCollectivismIndex() { return globalCollectivismIndex; }
    public static void setGlobalCollectivismIndex(double idx) { globalCollectivismIndex = Math.max(0.0, Math.min(1.0, idx)); }

    public static double getGlobalEnvironmentalStewardship() { return globalEnvironmentalStewardship; }
    public static void setGlobalEnvironmentalStewardship(double stw) { globalEnvironmentalStewardship = Math.max(0.0, Math.min(1.0, stw)); }

    public static void resetState() {
        globalCollectivismIndex = 0.5;
        globalEnvironmentalStewardship = 0.5;
    }
}
