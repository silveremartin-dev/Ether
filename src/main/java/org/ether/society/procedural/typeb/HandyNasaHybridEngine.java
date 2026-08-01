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
 * NASA HANDY Hybrid Model Variant B7.2 (NASA HANDY + Ether H3 Grid Integration).
 * Injects wealth inequality Gini penalization and ecological carrying capacity depletion
 * directly into Ether H3 cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class HandyNasaHybridEngine {
    private static final Logger logger = LoggerFactory.getLogger(HandyNasaHybridEngine.class);

    public static void processPlugin(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double pop = cell.getPopulation();
            double biomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 100.0;

            // HANDY hypothesis: high population depletes natural biomass carrying capacity
            double depletionRate = 0.0001 * pop * deltaYears;
            cell.setBiomassNatural(Math.max(0.0, biomass - depletionRate));
        }
    }
}
