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
 * Pre-Columbian Amerindian Ecosystem & Virgin-Soil Epidemics Engine Variant B32.1 (Pure) & B32.2 (Hybrid).
 * Models pre-Columbian Amerindian dynamics (Mesoamerica, Andes, North America):
 * 1. Lack of domesticated draft animals (due to Pleistocene megafauna overkill) limits mechanical work EROEI.
 * 2. Virgin-Soil Epidemic Shock: Old World pathogen contact causes catastrophic 85-95% demographic collapse.
 * 3. Agricultural intensification & drought vulnerability (e.g., Maya classic collapse).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class AmerindianEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(AmerindianEcosystemEngine.class);

    private static boolean oldWorldContactTriggered = false;
    private static double epidemicMortalityRate = 0.90; // 90% virgin soil epidemic mortality upon contact

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() <= 0) continue;

            double pop = cell.getPopulation();

            // Draft animal constraint: Mechanical work density capped without horses/cattle
            double work = cell.getResourceWork() != null ? cell.getResourceWork() : 20.0;
            cell.setResourceWork(Math.min(work, 40.0)); // Cap work output from lack of draft animals

            // Virgin Soil Epidemic Shock upon European/Old World contact
            if (oldWorldContactTriggered) {
                double newPop = pop * (1.0 - epidemicMortalityRate * Math.min(1.0, 0.10 * deltaYears));
                cell.setPopulation((int) Math.max(10, newPop));
            }
        }
    }

    public static boolean isOldWorldContactTriggered() { return oldWorldContactTriggered; }
    public static void setOldWorldContactTriggered(boolean contact) { oldWorldContactTriggered = contact; }

    public static double getEpidemicMortalityRate() { return epidemicMortalityRate; }
    public static void setEpidemicMortalityRate(double rate) { epidemicMortalityRate = Math.max(0.1, Math.min(0.99, rate)); }
}
