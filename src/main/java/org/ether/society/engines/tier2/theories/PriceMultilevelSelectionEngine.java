/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Multilevel Cultural Selection & Price Equation Engine (George R. Price, 1970).
 *
 * <p>Models the evolution of altruistic cooperation, prosocial norms, and military solidarity
 * through between-group vs within-group cultural selection pressures:</p>
 * <pre>
 *   Δz̄ = Cov(w_i, z_i) / w̄  +  E[w_i · Δz_i] / w̄
 * </pre>
 * where:
 * <ul>
 *   <li><b>Cov(w_i, z_i) / w̄</b>: Between-group selection term (prosocial/altruistic groups win inter-polity competition).</li>
 *   <li><b>E[w_i · Δz_i] / w̄</b>: Within-group individual selection term (free-riders / selfish rent-seekers erode civic solidarity).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PriceMultilevelSelectionEngine extends AbstractSimulationEngine {
    public PriceMultilevelSelectionEngine() {
        super("PriceMultilevelSelectionEngine", "PriceMultilevelSelectionEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(PriceMultilevelSelectionEngine.class);

    @Override
    public String getName() {
        return "Price Equation Multilevel Cultural Selection";
    }

    @Override
    public String getDescription() {
        return "Models the evolution of prosocial cooperation, military solidarity, and altruism via Price's multilevel cultural selection theorem.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Price Equation Multilevel Selection (Nature 1970)]
               • Full Equation:       Δz̄ = [ Cov(w_g, z_g) + E(w_g · Δz_g) ] / w̄
               • Between-Group Term:  Cov(w_g, z_g) > 0 (Cooperative tribes triumph in inter-polity warfare)
               • Within-Group Term:   E(w_g · Δz_g) < 0 (Free-riders exploit altruists internally)
               • Evolutionary Stable: Altruism spreads when Between-Group Covariance > Within-Group Defection
               Units: z [degré d'altruisme / civisme [0, 1]], w [fitness culturelle / survie démographique]
               Ref: G. R. Price (1970) "Selection and Covariance", Nature; Bowles & Gintis (2011) "A Cooperative Species"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Evolutionary Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double meanWealth = cells.stream()
                .filter(java.util.Objects::nonNull)
                .mapToDouble(c -> c.getResourceCapitalOr(0.0))
                .average().orElse(10.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double gini = cell.getGiniIndexOr(0.35);
            double capital = cell.getResourceCapitalOr(10.0);

            // Group-level prosociality / civic solidarity index z = (1.0 - Gini)
            double prosociality = Math.clamp(1.0 - gini, 0.0, 1.0);

            // Between-group selective advantage: high cooperation increases collective labor and capital output
            // Normalized saturation ratio ensures stable bounds [0.0, 2.0]
            double relativeCapital = (2.0 * capital) / (capital + Math.max(1.0, meanWealth));
            double betweenGroupAdvantage = prosociality * 0.03 * relativeCapital;

            // Within-group free-rider erosion: smooth continuous softplus penalty for elite inequality / rent extraction
            double withinGroupErosion = 0.02 * (1.0 / (1.0 + Math.exp(-12.0 * (gini - 0.45)))) * Math.max(0.0, gini - 0.35);

            double netAltruismShift = Math.clamp((betweenGroupAdvantage - withinGroupErosion) * deltaYears, -0.50, 0.50);

            double currentWork = cell.getResourceWorkOr(50.0);
            cell.setResourceWork(Math.max(10.0, currentWork * (1.0 + netAltruismShift)));

            // Civic prosociality compounds into productive capital accumulation
            if (netAltruismShift > 0.0) {
                cell.setResourceCapital(capital + (capital * netAltruismShift * 0.10));
            }
        }
    }
}
