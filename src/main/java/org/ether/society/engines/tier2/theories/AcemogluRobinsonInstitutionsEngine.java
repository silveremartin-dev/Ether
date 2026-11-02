/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Inclusive vs Extractive Economic Institutions Engine (Daron Acemoglu & James A. Robinson, 2012).
 *
 * <p>Models the long-run divergence of wealth, property rights security, and state capacity:</p>
 * <pre>
 *   dI_inclusive/dt = μ_inst · (BalanceOfPower - MonopolyRents)
 *   dK/dt = s · Y - δ·K + [Inclusive_Reinvestment_Bonus - Extractive_Expropriation_Drag]
 *   Innovation_Incentive = I_inclusive · Tech_level
 * </pre>
 * where:
 * <ul>
 *   <li><b>Inclusive Institutions</b>: Secure property rights, rule of law, and level playing field empower broad-based investment and creative destruction.</li>
 *   <li><b>Extractive Institutions</b>: Power concentrated in the hands of a narrow elite that extracts wealth from the rest of society, stifling innovation and inducing capital flight.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AcemogluRobinsonInstitutionsEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(AcemogluRobinsonInstitutionsEngine.class);

    @Override
    public String getName() {
        return "Acemoglu-Robinson Inclusive vs Extractive Institutions";
    }

    @Override
    public String getDescription() {
        return "Models the institutional divergence between inclusive property rights (innovation/growth) and extractive elite monopolies (stagnation).";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Acemoglu & Robinson Institutional Divergence Model (2012)]
               • Inclusiveness Index:      I_inc = clamp(1.0 - Gini, 0.05, 0.95)
               • Property Rights Bonus:    dK_inc/dt = +0.02 · (I_inc - 0.35) · K  (for I_inc > 0.35)
               • Extractive Capital Drag:  dK_ext/dt = -0.015 · (0.35 - I_inc) · K (for I_inc <= 0.35)
               • Creative Destruction:     dTech/dt = I_inc · 0.005 · (K / (K + 100.0)) · Tech^0.2
               Units: I_inc [0, 1], K [capital units], Tech [scalar 0-10]
               Ref: D. Acemoglu & J. A. Robinson (2012) "Why Nations Fail: The Origins of Power, Prosperity, and Poverty"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Institutional Economics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double gini = cell.getGiniIndexOr(0.35);
            double tech = cell.getTechnologyLevelOr(1.0);
            double capital = cell.getResourceCapitalOr(10.0);

            // Inclusiveness score: low Gini + broad-based distribution of economic opportunities
            double inclusiveness = Math.clamp(1.0 - gini, 0.05, 0.95);

            // Deep-time baseline invariant (AGENTS.md §8): before sedentary agriculture / legal states (T < 1.0),
            // formal institutional property rights and sovereign extraction indices are dormant
            if (tech < 1.0) continue;

            // Inclusive institutions protect property rights and encourage capital reinvestment;
            // extractive institutions impose expropriation risk and elite rent-seeking (Acemoglu & Robinson 2012).
            // Smooth continuous differential formulation: dK/dt = K · 0.02 · (I_inc - 0.35) · Δt
            double netInstitutionalGrowthRate = 0.02 * (inclusiveness - 0.35) * deltaYears;
            cell.setResourceCapital(Math.max(0.0, capital * (1.0 + netInstitutionalGrowthRate)));

            // Creative destruction and technological innovation fostered by inclusive property rights
            double capitalFactor = capital / (capital + 100.0);
            double techIncrement = inclusiveness * 0.005 * (0.5 + 0.5 * capitalFactor) * deltaYears;
            cell.setTechnologyLevel(Math.min(10.0, tech + techIncrement));
        }
    }
}
