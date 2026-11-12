/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    public void process(List<H3Cell> cells, double deltaYears) {
        processInstitutionalDynamics(cells, deltaYears);
    }

    public static void processInstitutionalDynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double gini = cell.getGiniIndexOr(0.35);
            double capital = cell.getResourceCapitalOr(10.0);
            double kPerCapita = capital / pop;

            // Inclusiveness score: low Gini + broad-based distribution of economic opportunities
            double inclusiveness = SmoothMath.smoothClamp(1.0 - gini, 0.05, 0.95, 20.0);

            // Sedentary property rights activation: dormant for nomadic / low-capital bands (AGENTS.md §8)
            double propertyRightsActivation = SmoothMath.sigmoid(kPerCapita, 100.0, 30.0);

            // Inclusive institutions protect property rights and encourage capital reinvestment;
            // extractive institutions impose expropriation risk and elite rent-seeking (Acemoglu & Robinson 2012).
            double netInstitutionalGrowthRate = 0.02 * (inclusiveness - 0.35) * propertyRightsActivation * deltaYears;
            cell.setResourceCapital(Math.max(0.0, capital * (1.0 + netInstitutionalGrowthRate)));
        }
    }
}
