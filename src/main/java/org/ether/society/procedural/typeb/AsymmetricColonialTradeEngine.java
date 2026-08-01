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
 * Asymmetric Colonial Trade & Demographic Dilution Engine Variant B25.1 (Pure) & B25.2 (Hybrid).
 * Portuguese Empire Case (16th-17th C.): Trading non-renewable bullion (silver) for renewable perishables (spices)
 * coupled with a small home population leads to colonial fragmentation, local elite trade diversion, and monopoly decay.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class AsymmetricColonialTradeEngine {
    private static final Logger logger = LoggerFactory.getLogger(AsymmetricColonialTradeEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceMetal() == null) continue;

            double metal = cell.getResourceMetal(); // Bullion stock
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // Asymmetric trade bullion drain: silver exchanged for perishable spices
            if (metal > 10.0) {
                cell.setResourceMetal(Math.max(0.0, metal - 0.5 * deltaYears)); // Bullion outflow
                cell.setResourceCapital(capital + 0.2 * deltaYears); // Temporary capital gain before monopoly decay
            }
        }
    }
}
