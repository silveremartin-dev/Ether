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
import org.ether.society.core.math.SmoothMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acemoglu-Robinson Institutional Economics Engine.
 *
 * <p>Models the divergence of nations driven by institutional architecture (Acemoglu &amp; Robinson 2012,
 * <i>Why Nations Fail</i>): inclusive institutions secure property rights and promote creative destruction
 * and capital deepening, whereas extractive institutions suffer elite expropriation and capital flight.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AcemogluRobinsonInstitutionsEngine extends AbstractSimulationEngine {
    public AcemogluRobinsonInstitutionsEngine() {
        super("AcemogluRobinsonInstitutionsEngine", "AcemogluRobinsonInstitutionsEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(AcemogluRobinsonInstitutionsEngine.class);

    @Override
    public String getName() {
        return "AcemogluRobinsonInstitutionsEngine";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 1.0) return;

        double gini = buffer.getGiniIndex() != null ? buffer.getGiniIndex()[i] : 0.35;
        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double kPerCapita = capital / pop;

        double inclusiveness = SmoothMath.smoothClamp(1.0 - gini, 0.05, 0.95, 20.0);
        double propertyRightsActivation = SmoothMath.sigmoid(kPerCapita, 100.0, 30.0);
        double netInstitutionalGrowthRate = 0.02 * (inclusiveness - 0.35) * propertyRightsActivation * dtYears;
        acc.dResourceCapital += (float) (capital * netInstitutionalGrowthRate);
    }
}
