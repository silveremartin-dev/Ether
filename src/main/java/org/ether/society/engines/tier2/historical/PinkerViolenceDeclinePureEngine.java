/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Steven Pinker Model Variant B16.1 (Pure) & B16.2 (Hybrid).
 * Pinker Law (Better Angels / Enlightenment): State monopolization of violence (Leviathan), commerce,
 * and literacy drive long-term secular decline in interpersonal and inter-state violent mortality per capita.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PinkerViolenceDeclinePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(PinkerViolenceDeclinePureEngine.class);

    /**
     * Calculate violent death rate per capita.
     *
     * @param literacyRate the literacy rate parameter (double)
     * @param stateStrength the state strength parameter (double)
     * @param tradeOpenness the trade openness parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateViolentDeathRatePerCapita(double literacyRate, double stateStrength, double tradeOpenness) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        // Pinker decline formula: baseline violence drops exponentially with Leviathan + Trade + Enlightenment
        double pacificationFactor = (stateStrength * 0.40) + (tradeOpenness * 0.30) + (literacyRate * 0.30);
        return Math.max(0.00001, 0.005 * Math.exp(-3.0 * pacificationFactor));
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
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double tech = cell.getTechnologyLevelOr(1.0);
            // Empirical literacy rate; invariant deep-time baseline is 0.0% (AGENTS.md §8)
            double literacy = tech < 1.0 ? 0.0 : Math.clamp((tech - 1.0) / 5.0, 0.0, 1.0);
            double stateStrength = Math.min(1.0, Math.max(0.0, (tech - 1.0) / 4.0));
            double tradeOpenness = 0.50;

            double violentMortalityRate = calculateViolentDeathRatePerCapita(literacy, stateStrength, tradeOpenness);

            // Calculate violent deaths scaling with deltaYears
            int pop = cell.getPopulation();
            int violentDeaths = (int) Math.round(pop * violentMortalityRate * deltaYears);
            if (violentDeaths <= 0) continue;

            // Historical violent mortality targeting (Eisner 2003): interpersonal violence & raids
            // fall overwhelmingly on young adult cohorts (pop15to24: 60%, pop25to49: 35%)
            int pop15 = cell.getPop15to24Or(0);
            int pop25 = cell.getPop25to49Or(0);
            int cas15 = Math.min(pop15, (int) Math.round(violentDeaths * 0.60));
            int cas25 = Math.min(pop25, (int) Math.round(violentDeaths * 0.35));
            int remainingCas = Math.max(0, violentDeaths - cas15 - cas25);

            cell.setPop15to24(pop15 - cas15);
            cell.setPop25to49(pop25 - cas25);

            if (remainingCas > 0) {
                int pop50 = cell.getPop50to64Or(0);
                int cas50 = Math.min(pop50, remainingCas);
                cell.setPop50to64(pop50 - cas50);
            }

            // Enforce exact demographic mass closure across all 7 cohorts
            int newTotal = cell.getPop0to4Or(0) + cell.getPop5to14Or(0)
                    + cell.getPop15to24Or(0) + cell.getPop25to49Or(0)
                    + cell.getPop50to64Or(0) + cell.getPop65to79Or(0)
                    + cell.getPop80PlusOr(0);
            cell.setPopulation(Math.max(1, newTotal));
        }
    }
}


