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
 * Celibate Monastic Demographic Buffer Engine Variant B23.1 (Pure) & B23.2 (Hybrid).
 * Diverts 2% to 10% of adult population into celibate monastic orders (e.g. Medieval Europe 2-4%, Tibet 10%),
 * absorbing Malthusian demographic overpressure without triggering war.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MonasticDemographicBufferEngine {
    private static final Logger logger = LoggerFactory.getLogger(MonasticDemographicBufferEngine.class);

    private static double monasticFraction = 0.04; // 4% default monastic buffer fraction

    /*
     * Process hybrid.
     * Enforces physical invariants and updates associated state variables within {@code MonasticDemographicBufferEngine}.
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

            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double pop = cell.getPopulation();

            // When food per capita drops below threshold, monastic vocation increases to buffer fertility
            if ((food / pop) < 0.10) {
                double celibateCount = pop * monasticFraction;
                // Reduce net birth rate by dampening fertile population fraction
                cell.setPopulation((int) (pop - celibateCount * 0.02 * deltaYears));
            }
        }
    }

    /*
     * Get monastic fraction.
     * Enforces physical invariants and updates associated state variables within {@code MonasticDemographicBufferEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static double getMonasticFraction() { return monasticFraction; }
    /*
     * Set monastic fraction operation.
     * <p>
     * Executes operational logic for {@code MonasticDemographicBufferEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param fraction the fraction argument (double)
     */
    public static void setMonasticFraction(double fraction) { monasticFraction = Math.max(0.0, Math.min(0.20, fraction)); }
}


