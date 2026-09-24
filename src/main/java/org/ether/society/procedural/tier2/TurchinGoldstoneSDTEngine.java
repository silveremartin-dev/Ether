/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Structural-Demographic Theory (SDT) & Political Stress Index Engine (Jack Goldstone & Peter Turchin).
 *
 * <p>Models the secular cycles of socio-political instability, elite overproduction, and state breakdown:</p>
 * <pre>
 *   PSI = MMP · EMP · SF
 * </pre>
 * where:
 * <ul>
 *   <li><b>MMP (Mass Mobilization Potential)</b>: Driven by falling real wages, youth bulges, and urban immiseration.
 *       $$\text{MMP} = \frac{w_0}{w_{\text{real}}} \cdot \left(\frac{N_{\text{youth}}}{N_{\text{total}}}\right) \cdot U_{\text{urban}}$$
 *   </li>
 *   <li><b>EMP (Elite Mobilization Potential)</b>: Driven by elite overproduction and intra-elite competition for fixed state offices.
 *       $$\text{EMP} = \left(\frac{N_{\text{elites}}}{N_{\text{aspirants}}}\right) \cdot \text{Gini}_{\text{elite}} \cdot \text{Factionalism}$$
 *   </li>
 *   <li><b>SF (State Fiscal Distress)</b>: Driven by escalating sovereign debt and shrinking tax revenues.
 *       $$\text{SF} = \frac{\text{Debt}}{\text{Annual Revenue}} \cdot \left(1 - \text{Legitimacy}\right)$$
 *   </li>
 *   <li><b>Crisis Threshold</b>: When $\text{PSI} > \text{PSI}_{\text{critical}}$, state collapses into civil war, fiscal bankruptcy, or peasant revolts.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class TurchinGoldstoneSDTEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(TurchinGoldstoneSDTEngine.class);

    @Override
    public String getName() {
        return "Turchin-Goldstone Structural-Demographic Theory (SDT)";
    }

    @Override
    public String getDescription() {
        return "Models secular political instability cycles, elite overproduction, mass immiseration, and state fiscal breakdown.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Goldstone-Turchin Political Stress Index (SDT / PSI)]
               • Political Stress Index:  PSI = MMP · EMP · SF
               • Mass Immiseration (MMP): MMP = (w_ref / RealWage) · UrbanFraction
               • Elite Overproduction:    EMP = (N_elites / Office_capacity) · Gini_index
               • State Fiscal Distress:   SF  = SovereignDebt / AnnualTaxRevenue
               • Secular Breakdown Trap:  If PSI > 10.0 -> State Fragmentation & Civil Conflict
               Units: PSI [indice sans dimension [0, +inf]], MMP, EMP, SF [multiplicateurs adimensionnels]
               Ref: P. Turchin (2016) "Ages of Discord", J. A. Goldstone (1991) "Revolution and Rebellion"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Structural Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 100) continue;

            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 500.0;

            // 1. Mass Mobilization Potential: food scarcity + urban density
            double mmp = Math.max(0.5, (1000.0 / Math.max(100.0, food)) * (pop / 2000.0));

            // 2. Elite Mobilization Potential: high capital inequality (Gini > 0.45)
            double emp = Math.max(0.5, Math.pow(gini / 0.35, 2.0));

            // 3. State Fiscal Distress (ratio of administrative overhead to resource capacity)
            double sf = Math.max(0.5, (capital / Math.max(10.0, food * 0.5)));

            double psi = mmp * emp * sf;

            // When PSI exceeds critical threshold (PSI >= 5.0), political unrest causes capital destruction
            if (psi > 5.0) {
                double unrestDamage = Math.min(capital * 0.15, (psi - 5.0) * 5.0 * deltaYears);
                cell.setResourceCapital(Math.max(5.0, capital - unrestDamage));

                // Increases local friction due to riots and civil breakdown
                double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.min(10.0, friction + (0.1 * deltaYears)));

                // Post-crisis elite purge: structural breakdown purges excessive elite fortunes, resetting Gini
                if (psi > 8.0) {
                    cell.setGiniIndex(Math.max(0.30, gini - (0.02 * deltaYears)));
                }
            }
        }
    }
}

