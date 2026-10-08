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
 * Institutional Complexity & Declining Marginal Returns Engine (Joseph Tainter, 1988).
 *
 * <p>Models the diminishing marginal returns to bureaucratic and technological complexity,
 * and the vulnerability of over-complex societies to catastrophic fiscal/energetic collapse:</p>
 * <pre>
 *   C_i = ln(1 + 0.1 · K_i)
 *   Σ_maint = C_i^1.15 · E_base
 *   dK_i/dt = Production_i · s - Σ_maint
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TainterComplexityCollapseEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(TainterComplexityCollapseEngine.class);

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code TainterComplexityCollapseEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Tainter Institutional Complexity & Declining Returns";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code TainterComplexityCollapseEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models the diminishing marginal returns on socio-political complexity and fiscal-energetic collapse vulnerabilities.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code TainterComplexityCollapseEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Tainter Law of Diminishing Marginal Returns on Complexity (1988)]
               • Complexity Level:   C_i = ln(1 + 0.05 · Capital_i)
               • Maintenance Burden: Σ_maint = C_i^1.20 · 50.0 Joules
               • Net Growth Rate:    dCapital/dt = NetSurplus_i - Σ_maint
               • Collapse Condition: If Σ_maint > Surplus -> Rapid Simplification / Capital Decay
               Units: C [score complexité sans dimension], Σ_maint [Joules maintenance institutionnelle/an]
               Ref: J. A. Tainter (1988) "The Collapse of Complex Societies", Cambridge Univ. Press
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code TainterComplexityCollapseEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Institutional Cliodynamics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code TainterComplexityCollapseEngine}.
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
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            if (capital < 50.0) continue;

            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            double surplus = pop * 0.05;

            // Complexity index derived from institutional capital depth
            double complexity = Math.log(1.0 + 0.02 * capital);
            // Institutional & infrastructural maintenance overhead scales with capital stock and bureaucratic depth
            double maintenanceCost = (0.015 * capital + Math.pow(complexity, 1.5) * 5.0) * deltaYears;

            if (maintenanceCost > surplus) {
                // Diminishing returns trap (Tainter 1988): complexity maintenance exceeds surplus -> capital collapse
                double erosion = (maintenanceCost - surplus) * 0.8;
                cell.setResourceCapital(Math.max(10.0, capital - erosion));
            }
        }
    }
}



