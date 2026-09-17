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
 * Marvin Harris Cultural Materialism Model Variant B13.1 (Pure) & B13.2 (Hybrid).
 * Postulates deterministic causal cascade: Infrastructure (Energy/Environment) -> Structure (Economy/Polity) -> Superstructure (Ideology).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class CulturalMaterialismPureEngine {
    private static final Logger logger = LoggerFactory.getLogger(CulturalMaterialismPureEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            // Infrastructure constraint: food and water determine political structure stability
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double pop = cell.getPopulation();

            if (pop > 0 && (food / pop) < 0.05) {
                // Infrastructural deficit forces structural collapse
                cell.setResourceCapital(Math.max(0.0, (cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0) * 0.90));
            }
        }
    }
}

