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
 * Human Self-Domestication Model Variant B22.1 (Pure) & B22.2 (Hybrid).
 * Lahire (2023) Law: Group living auto-domesticates humans by penalizing impulsive violence,
 * favoring language skills, prolonged childhood learning, and grandmother effects.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SelfDomesticationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SelfDomesticationEngine.class);

    /*
     * Process hybrid.
     * Enforces physical invariants and updates associated state variables within {@code SelfDomesticationEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
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


