/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

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
 * @version 4.0.0
 */
public class PsychohistoryPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(PsychohistoryPureEngine.class);

    public static double calculateSeldonCrisisProbability(double globalPopulation, double inequalityGini) {
        if (globalPopulation < 1e6) return 0.01;
        // Psychohistorical law: probability of structural bifurcation scales with mass N and inequality entropy
        return Math.min(0.99, (Math.log10(globalPopulation) / 10.0) * inequalityGini);
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double totalPop = 0.0;
        for (H3Cell cell : cells) {
            if (cell != null) totalPop += cell.getPopulation();
        }

        double crisisProb = calculateSeldonCrisisProbability(totalPop, 0.45);
        if (crisisProb > 0.75) {
            logger.info("🔮 Psychohistory Engine: Seldon Crisis Impending! Probability = {}%", String.format("%.1f", crisisProb * 100));
        }
    }
}
