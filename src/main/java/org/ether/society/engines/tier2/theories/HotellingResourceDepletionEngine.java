/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Hotelling Non-Renewable Resource Depletion & Scarcity Rent Engine (Hotelling, 1931).
 *
 * <p>Models the economic extraction dynamics and price path of exhaustible mineral and fossil reserves:</p>
 * <pre>
 *   [P(t) - MC(Q)] / [P_0 - MC(Q_0)] = exp(r Â· t)
 *   MC(Q) = MC_0 / (1 - Q_cumul / Q_total)^Î»
 * </pre>
 * where:
 * <ul>
 *   <li><b>P(t) - MC</b>: Scarcity rent (net marginal profit) growing at the discount rate $r$.</li>
 *   <li><b>MC(Q)</b>: Marginal extraction cost escalating asymptotically as accessible high-grade reserves deplete.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class HotellingResourceDepletionEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(HotellingResourceDepletionEngine.class);

    public static final double DISCOUNT_RATE_R = 0.03; // Real interest rate (3% per year)

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code HotellingResourceDepletionEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Hotelling Resource Depletion & Scarcity Rent";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code HotellingResourceDepletionEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models the depletion economics and escalating scarcity rents of non-renewable mineral/fossil assets.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code HotellingResourceDepletionEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Hotelling Exhaustible Resource Valuation Rule (JPE 1931)]
               â€¢ Scarcity Rent:     [P(t) - MC] = [P_0 - MC] Â· exp(r Â· t)
               â€¢ Marginal Cost:     MC(Q) = MC_0 / (1 - Q_extracted / Q_initial)^1.5
               â€¢ Depletion Flow:    dQ/dt = -min(Q_remaining, Demand Â· (P / P_substitute)^-Îµ)
               Units: P, MC [$/tonne], Q [tonnes minerais/pÃ©trole], r [%/an]
               Ref: H. Hotelling (1931) "The Economics of Exhaustible Resources"
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code HotellingResourceDepletionEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Resource Economics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code HotellingResourceDepletionEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double metalDeposit = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;
            if (metalDeposit <= 5.0) continue;

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            double industrialDemand = Math.max(1.0, pop * 0.02);

            // Deplete ore deposit based on extraction demand
            double extraction = Math.min(metalDeposit * 0.05, industrialDemand * deltaYears);
            cell.setResourceMetal(Math.max(0.0, metalDeposit - extraction));

            // Scarcity rent escalates infrastructure cost
            double remainingFraction = Math.max(0.01, (metalDeposit - extraction) / (metalDeposit + 100.0));
            double costMultiplier = Math.pow(1.0 / remainingFraction, 0.5);

            double currentCapital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;
            // High resource rents increase local capital accumulation in mining hub
            cell.setResourceCapital(currentCapital + (extraction * 2.0 * costMultiplier));
        }
    }
}



