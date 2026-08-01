/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Currency, Financial Banking & Economic Institutions Simulation Engine.
 * Models:
 * 1. <b>Evolution of Monetary Systems</b>: Barter (Prehistory) -> Metallic Coinage (Antiquity/Medieval) -> Paper Currency & Credit Banking (Modern).
 * 2. <b>Capital Accumulation & Credit Supply</b>: Financial banking expands infrastructure investment (resourceCapital) and trade velocity.
 * 3. <b>Inflation & Financial Crises</b>: Over-issuance of credit or debasement of currency increases Gini inequality and triggers financial panics.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.9.0
 */
public class FinancialBankingEngine {
    private static final Logger logger = LoggerFactory.getLogger(FinancialBankingEngine.class);

    /**
     * Executes one financial and banking simulation tick across cells.
     */
    public static void processFinancialSystem(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int bankingHubs = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // 1. Emergence of Banking Institutions (Tech >= 5.0, High Capital)
            if (tech >= 5.0 && capital > 2000.0) {
                bankingHubs++;
                // Financial credit multiplier expands capital investment
                double creditMultiplier = 1.05 + (tech / 100.0);
                cell.setResourceCapital(capital * creditMultiplier);

                // Inflationary pressure under excessive credit growth
                if (capital > 15000.0) {
                    double inflationRisk = (capital - 15000.0) / 100000.0;
                    cell.setGiniIndex(Math.min(1.0, cell.getGiniIndex() + inflationRisk));
                }
            } else if (tech >= 2.5) {
                // Metallic Coinage Era: Capital growth tied to precious metal availability
                double preciousMetals = cell.getResourcePreciousMetal() != null ? cell.getResourcePreciousMetal() : 0.0;
                if (preciousMetals > 50.0) {
                    cell.setResourceCapital(capital + preciousMetals * 0.1);
                }
            }
        }

        if (bankingHubs > 0) {
            logger.info("Financial Engine: Credit banking multiplier active across {} financial hub cells.", bankingHubs);
        }
    }
}
