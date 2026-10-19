*
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
 * Pre-Columbian Amerindian Ecosystem & Virgin-Soil Epidemics Engine Variant B32.1 (Pure) & B32.2 (Hybrid).
 * Models pre-Columbian Amerindian dynamics (Mesoamerica, Andes, North America):
 * 1. Lack of domesticated draft animals (due to Pleistocene megafauna overkill) limits mechanical work EROEI.
 * 2. Virgin-Soil Epidemic Shock: Old World pathogen contact causes catastrophic 85-95% demographic collapse.
 * 3. Agricultural intensification & drought vulnerability (e.g., Maya classic collapse).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class AmerindianEcosystemEngine {
    private static final Logger logger = LoggerFactory.getLogger(AmerindianEcosystemEngine.class);

    /** Internal state: old world contact triggered (boolean). */
    private static boolean oldWorldContactTriggered = false;
    private static double epidemicMortalityRate = 0.90; // 90% virgin soil epidemic mortality upon contact

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        oldWorldContactTriggered = false;
        epidemicMortalityRate = 0.90;
    }

    /**
     * Process hybrid.
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
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            // Universal Draft Megafauna Absence & Zoonotic Naivety Law (no hardcoded GPS longitude box):
            // Applies to any society lacking domesticated draft livestock (biomassLivestock < 1.0)
            // before industrial mechanization (tech < 3.0).
            double livestock = cell.getBiomassLivestock() != null ? cell.getBiomassLivestock() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double pop = cell.getPopulation();

            if (livestock < 1.0 && tech < 3.0) {
                // Draft animal constraint: Mechanical work density capped when relying purely on human muscle
                double work = cell.getResourceWork() != null ? cell.getResourceWork() : 20.0;
                cell.setResourceWork(Math.min(work, 40.0));
            }

            // Virgin-Soil Zoonotic Epidemic Shock upon contact with high-pathogen-load populations
            if (oldWorldContactTriggered && livestock < 5.0) {
                double newPop = pop * (1.0 - epidemicMortalityRate * Math.min(1.0, 0.10 * deltaYears));
                cell.setPopulation((int) Math.max(10, newPop));
            }
        }
    }

    /**
     * Is old world contact triggered.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isOldWorldContactTriggered() { return oldWorldContactTriggered; }
    /**
     * Set old world contact triggered operation.
     * <p>
     * Executes operational logic for {@code AmerindianEcosystemEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param contact the contact argument (boolean)
     */
    public static void setOldWorldContactTriggered(boolean contact) { oldWorldContactTriggered = contact; }

    /**
     * Get epidemic mortality rate.
     *
     * @return the resulting computation or state reference
     */
    public static double getEpidemicMortalityRate() { return epidemicMortalityRate; }
    /**
     * Set epidemic mortality rate operation.
     * <p>
     * Executes operational logic for {@code AmerindianEcosystemEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (double)
     */
    public static void setEpidemicMortalityRate(double rate) { epidemicMortalityRate = Math.max(0.1, Math.min(0.99, rate)); }
}


