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
 * E.O. Wilson & W.D. Hamilton Kin Selection Model Variant B28.1 (Pure) & B28.2 (Hybrid).
 * Hamilton Law (r * B > C): Altruistic cooperation is bounded by genetic/cultural relatedness r.
 * Under food scarcity, inter-group hostility increases toward culturally distant outgroups.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class KinSelectionHamiltonEngine {
    private static final Logger logger = LoggerFactory.getLogger(KinSelectionHamiltonEngine.class);

    public static boolean checkHamiltonRule(double relatednessR, double benefitB, double costC) {
        return (relatednessR * benefitB) > costC;
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getFoodResource() == null) continue;

            double food = cell.getFoodResource();
            double pop = cell.getPopulation();

            // High food scarcity triggers Hamilton outgroup hostility filter
            if (pop > 0 && (food / pop) < 0.05) {
                // Outgroup hostility increases military work expenditure
                if (cell.getResourceWork() != null) {
                    cell.setResourceWork(cell.getResourceWork() * (1.0 + 0.08 * deltaYears));
                }
            }
        }
    }
}
