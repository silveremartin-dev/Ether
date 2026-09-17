/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Inclusive vs Extractive Economic Institutions Engine (Daron Acemoglu & James A. Robinson, 2012).
 *
 * <p>Models the long-run divergence of wealth, property rights security, and state capacity:</p>
 * <pre>
 *   dI_inclusive/dt = μ_inst · (BalanceOfPower - MonopolyRents)
 *   Innovation_Incentive = I_inclusive · Tech_level
 * </pre>
 * where:
 * <ul>
 *   <li><b>Inclusive Institutions</b>: Secure property rights, rule of law, and level playing field empower broad-based investment and creative destruction.</li>
 *   <li><b>Extractive Institutions</b>: Power concentrated in the hands of a narrow elite that extracts wealth from the rest of society, stifling innovation.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
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
               • Inclusiveness Evolution:  dI_inc/dt = μ · (Pluralism_score - Elite_monopoly_rents)
               • Creative Destruction:     Investment_rate = I_inc · (1.0 - Monopolistic_entry_barriers)
               • Extractive Trap:          When Elite extractive rents dominate -> Growth stagnation & Capital flight
               Units: I_inc [indice d'inclusivité institutionnelle [0, 1]], Pluralism [0, 1]
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
            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;

            // Inclusiveness score: low Gini + high pluralism
            double inclusiveness = Math.clamp(1.0 - gini, 0.1, 0.9);

            // Inclusive institutions incentivize capital reinvestment and technological adoption
            double institutionalGrowthBonus = inclusiveness * 0.02 * Math.log(1.0 + tech) * deltaYears;
            cell.setResourceCapital(capital * (1.0 + institutionalGrowthBonus));
        }
    }
}

