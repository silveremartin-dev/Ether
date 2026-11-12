/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Max Weber Protestant Work Ethic &amp; Human Capital Engine.
 *
 * <p>Formalizes Max Weber (1905) and Becker &amp; Woessmann (2009, <i>QJE</i> 124:531): religious
 * and institutional reforms promoting literacy, thrift, and ascetic reinvestment of surpluses
 * accelerate capital formation (\(\Delta K\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ProtestantWorkEthicEngine extends AbstractSimulationEngine {
    public ProtestantWorkEthicEngine() {
        super("ProtestantWorkEthicEngine", "ProtestantWorkEthicEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static double capitalSavingsMultiplier = 1.30;
    private static double techAccelerationMultiplier = 1.20;

    @Override
    public void reset() {
        resetState();
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0 || dtYears <= 0.0) return;
        double capital = buffer.getResourceCapital()[i];
        double literacyWeight = SmoothMath.smoothClamp(capital / (pop * 2000.0), 0.10, 1.0, 10.0);
        double capitalGain = (pop * 0.10 * (1.0 + (capitalSavingsMultiplier - 1.0) * literacyWeight)) * dtYears;
        acc.dResourceCapital += (float) capitalGain;
    }

    public static double getCapitalSavingsMultiplier() { return capitalSavingsMultiplier; }
    public static void setCapitalSavingsMultiplier(double mult) { capitalSavingsMultiplier = mult; }

    public static double getTechAccelerationMultiplier() { return techAccelerationMultiplier; }
    public static void setTechAccelerationMultiplier(double mult) { techAccelerationMultiplier = mult; }

    public static void resetState() {
        capitalSavingsMultiplier = 1.30;
        techAccelerationMultiplier = 1.20;
    }
}
