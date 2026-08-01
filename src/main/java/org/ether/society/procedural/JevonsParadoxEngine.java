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
 * Jevons Paradox & Rebound Effect Engine (Jancovici & Bihouix Rebound Law).
 * Models how technological efficiency gains increase total aggregate resource consumption.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class JevonsParadoxEngine {
    private static final Logger logger = LoggerFactory.getLogger(JevonsParadoxEngine.class);

    private static double reboundCoefficient = 0.80; // 80% rebound effect

    public static void processJevonsRebound(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getTechnologyLevel() == null) continue;

            double tech = cell.getTechnologyLevel();
            if (tech > 2.0) {
                // High technology efficiency drives expanded energy demand
                double reboundDemand = tech * reboundCoefficient * 0.005 * deltaYears;
                if (cell.getEnergyFoodConsumed() != null) {
                    cell.setEnergyFoodConsumed(cell.getEnergyFoodConsumed() + reboundDemand);
                }
            }
        }
    }

    public static double getReboundCoefficient() { return reboundCoefficient; }
    public static void setReboundCoefficient(double coeff) { reboundCoefficient = Math.max(0.0, Math.min(2.0, coeff)); }
}
