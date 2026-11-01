/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * E.O. Wilson &amp; W.D. Hamilton Kin Selection Model Variant B28.1 (Pure) &amp; B28.2 (Hybrid).
 * Hamilton Law (r * B &gt; C): Altruistic cooperation is bounded by genetic/cultural relatedness r.
 * Under food scarcity, inter-group hostility increases toward culturally distant outgroups.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KinSelectionHamiltonEngine {
    private static final Logger logger = LoggerFactory.getLogger(KinSelectionHamiltonEngine.class);

    /**
     * Check hamilton rule.
     *
     * @param relatednessR the relatedness r parameter (double)
     * @param benefitB the benefit b parameter (double)
     * @param costC the cost c parameter (double)
     * @return the resulting computation or state reference
     */
    public static boolean checkHamiltonRule(double relatednessR, double benefitB, double costC) {
        return (relatednessR * benefitB) > costC;
    }

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getFoodResource() == null) continue;

            double food = cell.getFoodResource();
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double foodPerCapita = food / pop;
            // Continuous outgroup hostility response under resource scarcity (Hamilton 1964; Bowles 2006)
            double scarcityStress = 1.0 / (1.0 + Math.exp(20.0 * (foodPerCapita - 0.05)));
            if (scarcityStress > 0.01) {
                // Outgroup hostility increases defensive/military work expenditure
                double work = cell.getResourceWorkOr(50.0);
                cell.setResourceWork(work * (1.0 + 0.08 * scarcityStress * deltaYears));
            }
        }
    }
}
