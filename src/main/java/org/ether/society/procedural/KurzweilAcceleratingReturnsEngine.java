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
 * Kurzweil Law of Accelerating Returns (LOAR) & Paradigm Shift S-Curve Engine.
 * Formally models:
 * 1. Double Exponential Knowledge Growth: W(t) = exp(e^(c * t))
 * 2. Nested S-Curve Paradigm Shifts: Logistic envelope transition when physical silicon/lithography limits are reached.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class KurzweilAcceleratingReturnsEngine {
    private static final Logger logger = LoggerFactory.getLogger(KurzweilAcceleratingReturnsEngine.class);

    private static double globalKnowledgeStock = 1.0;
    private static double accelerationRate = 0.05;

    public static void processAcceleratingReturns(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        // Double-exponential growth rate dW/dt = c * W * log(W + 1)
        double dW = accelerationRate * globalKnowledgeStock * Math.log(globalKnowledgeStock + 1.0) * deltaYears;
        globalKnowledgeStock += dW;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double currentTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            // S-Curve Paradigm Shift Envelope: P(t) = K / (1 + exp(-beta * t))
            double paradigmCapacity = 10.0 / (1.0 + Math.exp(-0.1 * globalKnowledgeStock));

            if (currentTech < paradigmCapacity) {
                cell.setTechnologyLevel(Math.min(10.0, currentTech + dW * 0.1));
            }
        }
    }

    public static double getGlobalKnowledgeStock() { return globalKnowledgeStock; }
    public static void setGlobalKnowledgeStock(double k) { globalKnowledgeStock = Math.max(1.0, k); }
}
