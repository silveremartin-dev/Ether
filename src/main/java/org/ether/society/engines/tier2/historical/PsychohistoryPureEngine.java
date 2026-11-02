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
 * Asimov Psychohistory Model Variant B12.1 (Pure) & B12.2 (Hybrid).
 * Computes statistical socio-historical inertia and predicts Seldon Crises (bifurcation points)
 * based on mass population N and entropy.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PsychohistoryPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(PsychohistoryPureEngine.class);

    /**
     * Calculate seldon crisis probability.
     *
     * @param globalPopulation the global population parameter (double)
     * @param inequalityGini the inequality gini parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateSeldonCrisisProbability(double globalPopulation, double inequalityGini) {
        if (globalPopulation <= 10.0) return 0.001;
        // Continuous logistic socio-historical inertia scaling across mass scale N
        // eliminates the knife-edge discontinuity at N = 1e6
        double logN = Math.log10(globalPopulation);
        double massInertia = 1.0 / (1.0 + Math.exp(-1.5 * (logN - 6.0)));
        return Math.clamp(massInertia * inequalityGini, 0.001, 0.99);
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

        double totalPop = 0.0;
        for (H3Cell cell : cells) {
            if (cell != null && cell.getPopulation() != null) totalPop += cell.getPopulation();
        }

        double totalGini = 0.0;
        int count = 0;
        for (H3Cell cell : cells) {
            if (cell != null && cell.getGiniIndex() != null) {
                totalGini += cell.getGiniIndex();
                count++;
            }
        }
        double meanGini = count > 0 ? (totalGini / count) : 0.35;
        double crisisProb = calculateSeldonCrisisProbability(totalPop, meanGini);
        if (crisisProb > 0.75) {
            logger.info("🔮 Psychohistory Engine: Seldon Crisis Impending! Probability = {}%", String.format(java.util.Locale.ROOT, "%.1f", crisisProb * 100));
        }
    }
}


