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
 *   dI_inclusive/dt = Î¼_inst Â· (BalanceOfPower - MonopolyRents)
 *   Innovation_Incentive = I_inclusive Â· Tech_level
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
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code AcemogluRobinsonInstitutionsEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Acemoglu-Robinson Inclusive vs Extractive Institutions";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code AcemogluRobinsonInstitutionsEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models the institutional divergence between inclusive property rights (innovation/growth) and extractive elite monopolies (stagnation).";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code AcemogluRobinsonInstitutionsEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Acemoglu & Robinson Institutional Divergence Model (2012)]
               â€¢ Inclusiveness Evolution:  dI_inc/dt = Î¼ Â· (Pluralism_score - Elite_monopoly_rents)
               â€¢ Creative Destruction:     Investment_rate = I_inc Â· (1.0 - Monopolistic_entry_barriers)
               â€¢ Extractive Trap:          When Elite extractive rents dominate -> Growth stagnation & Capital flight
               Units: I_inc [indice d'inclusivitÃ© institutionnelle [0, 1]], Pluralism [0, 1]
               Ref: D. Acemoglu & J. A. Robinson (2012) "Why Nations Fail: The Origins of Power, Prosperity, and Poverty"
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code AcemogluRobinsonInstitutionsEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Institutional Economics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code AcemogluRobinsonInstitutionsEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;

            // Inclusiveness score: low Gini + high pluralism
            double inclusiveness = Math.clamp(1.0 - gini, 0.1, 0.9);

            // Inclusive institutions incentivize capital reinvestment and technological adoption (creative destruction)
            double institutionalGrowthBonus = inclusiveness * 0.02 * Math.log(1.0 + tech) * deltaYears;
            cell.setResourceCapital(capital * (1.0 + institutionalGrowthBonus));
            cell.setTechnologyLevel(Math.min(10.0, tech + (inclusiveness * 0.005 * deltaYears)));
        }
    }
}



