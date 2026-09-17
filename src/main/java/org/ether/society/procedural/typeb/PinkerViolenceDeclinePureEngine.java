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
 * Steven Pinker Model Variant B16.1 (Pure) & B16.2 (Hybrid).
 * Pinker Law (Better Angels / Enlightenment): State monopolization of violence (Leviathan), commerce,
 * and literacy drive long-term secular decline in interpersonal and inter-state violent mortality per capita.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class PinkerViolenceDeclinePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(PinkerViolenceDeclinePureEngine.class);

    public static double calculateViolentDeathRatePerCapita(double literacyRate, double stateStrength, double tradeOpenness) {
        // Pinker decline formula: baseline violence drops exponentially with Leviathan + Trade + Enlightenment
        double pacificationFactor = (stateStrength * 0.40) + (tradeOpenness * 0.30) + (literacyRate * 0.30);
        return Math.max(0.00001, 0.005 * Math.exp(-3.0 * pacificationFactor));
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double literacy = Math.min(1.0, tech / 5.0);
            double stateStrength = Math.min(1.0, tech / 4.0);
            double tradeOpenness = 0.50;

            double violentMortalityRate = calculateViolentDeathRatePerCapita(literacy, stateStrength, tradeOpenness);

            // Reduce population by violent mortality rate
            double violentDeaths = cell.getPopulation() * violentMortalityRate * deltaYears;
            cell.setPopulation(Math.max(1, (int)(cell.getPopulation() - violentDeaths)));
        }
    }
}

