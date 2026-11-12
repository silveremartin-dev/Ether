/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Co-Governance, Atmospheric Commons &amp; Network Trade Engine.
 *
 * <p>Models bilateral resource treaties, global carbon quota agreements,
 * and thermodynamic exchange networks between planetary regions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CoGovernanceTradeEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(CoGovernanceTradeEngine.class);

    /** Internal state: global carbon quota treaty active (boolean). */
    private static boolean globalCarbonQuotaTreatyActive = false;
    /** Internal state: international resource trade volume (double). */
    private static double internationalResourceTradeVolume = 1.0;

    public CoGovernanceTradeEngine() {
        super("CoGovernanceTradeEngine", "Co-Governance & Atmospheric Commons", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (globalCarbonQuotaTreatyActive && buffer != null && buffer.getPollutionLevel() != null) {
            float pol = buffer.getPollutionLevel()[i];
            if (pol > 200.0f) {
                double decayed = pol * Math.exp(-0.02 * dtYears);
                acc.dPollutionLevel += (float) (decayed - pol);
            }
        }
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processTradeAndGovernance(cells, deltaYears);
    }

    public static void resetState() {
        globalCarbonQuotaTreatyActive = false;
        internationalResourceTradeVolume = 1.0;
    }

    @Override
    public void reset() {
        resetState();
    }

    public static void processTradeAndGovernance(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        if (globalCarbonQuotaTreatyActive) {
            for (H3Cell cell : cells) {
                if (cell == null || cell.getPollutionLevel() == null) continue;
                if (cell.getPollutionLevel() > 200.0) {
                    cell.setPollutionLevel(cell.getPollutionLevel() * Math.exp(-0.02 * deltaYears));
                }
            }
        }
    }

    public static boolean isGlobalCarbonQuotaTreatyActive() {
        return globalCarbonQuotaTreatyActive;
    }

    public static void setGlobalCarbonQuotaTreatyActive(boolean active) {
        globalCarbonQuotaTreatyActive = active;
    }

    public static double getInternationalResourceTradeVolume() {
        return internationalResourceTradeVolume;
    }

    public static void setInternationalResourceTradeVolume(double volume) {
        internationalResourceTradeVolume = Math.max(0.0, volume);
    }
}
