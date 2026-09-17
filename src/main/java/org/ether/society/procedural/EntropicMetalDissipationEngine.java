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
 * Entropic Metal Dissipation Engine (Bihouix Recycling Limits).
 * Models irreversible physical dissipation of metals into environment due to entropy generation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EntropicMetalDissipationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EntropicMetalDissipationEngine.class);

    private static double annualDissipationRate = 0.015; // 1.5% annual thermodynamic loss rate

    public static void processEntropicDissipation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceMetal() == null) continue;

            double metal = cell.getResourceMetal();
            // Irreversible entropic loss
            double dissipated = metal * annualDissipationRate * deltaYears;
            cell.setResourceMetal(Math.max(0.0, metal - dissipated));

            // Dissipated metals add to micro-pollution level
            if (cell.getPollutionLevel() != null) {
                cell.setPollutionLevel(cell.getPollutionLevel() + dissipated * 0.1);
            }
        }
    }

    public static double getAnnualDissipationRate() { return annualDissipationRate; }
    public static void setAnnualDissipationRate(double rate) { annualDissipationRate = Math.max(0.0, Math.min(0.20, rate)); }
}

