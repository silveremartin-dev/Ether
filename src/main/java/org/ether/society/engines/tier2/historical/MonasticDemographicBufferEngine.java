/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Celibate Monastic Demographic Buffer Engine Variant B23.1 (Pure) &amp; B23.2 (Hybrid).
 * Diverts 2% to 10% of adult population into celibate monastic orders (e.g. Medieval Europe 2-4%, Tibet 10%),
 * absorbing Malthusian demographic overpressure by dampening fertility without triggering war.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MonasticDemographicBufferEngine extends AbstractSimulationEngine {
    public MonasticDemographicBufferEngine() {
        super("MonasticDemographicBufferEngine", "MonasticDemographicBufferEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(MonasticDemographicBufferEngine.class);

    private static double monasticFraction = 0.04; // 4% default monastic buffer fraction

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
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double food = cell.getFoodResourceOr(0.0);
            double pop = cell.getPopulation();
            double foodPerCapita = food / Math.max(1.0, pop);

            // Continuous Malthusian pressure on monastic vocation (Voigtländer & Voth 2013):
            // Dampens fertility rate according to demographic conservation rather than unphysical population deletion
            double malthusianDeficit = Math.max(0.0, 1.0 - foodPerCapita / 0.10);
            if (malthusianDeficit > 0.0) {
                double monasticDamping = monasticFraction * malthusianDeficit * 0.20 * deltaYears;
                cell.setFertility(Math.max(1.0, (cell.getFertilityOr(2.1)) - monasticDamping));
            }
        }
    }

    /**
     * Get monastic fraction.
     *
     * @return the resulting computation or state reference
     */
    public static double getMonasticFraction() { return monasticFraction; }

    /**
     * Set monastic fraction operation.
     * <p>
     * Executes operational logic for {@code MonasticDemographicBufferEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param fraction the fraction argument (double)
     */
    public static void setMonasticFraction(double fraction) { monasticFraction = Math.max(0.0, Math.min(0.20, fraction)); }

    /**
     * Resets monastic fraction for scenario isolation.
     */
    public static void resetState() {
        monasticFraction = 0.04;
    }
}
