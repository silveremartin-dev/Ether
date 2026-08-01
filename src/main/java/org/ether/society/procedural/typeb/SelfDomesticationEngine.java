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
 * Human Self-Domestication Model Variant B22.1 (Pure) & B22.2 (Hybrid).
 * Lahire (2023) Law: Group living auto-domesticates humans by penalizing impulsive violence,
 * favoring language skills, prolonged childhood learning, and grandmother effects.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class SelfDomesticationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SelfDomesticationEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double popDensity = cell.getPopulation() / 1000.0;
            // High social density accelerates self-domestication, reducing internal violence
            if (popDensity > 1.0) {
                double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
                // Boost technological/linguistic learning rate via grandmother effect & prolonged childhood
                cell.setTechnologyLevel(tech + 0.002 * Math.log10(1.0 + popDensity) * deltaYears);
            }
        }
    }
}
