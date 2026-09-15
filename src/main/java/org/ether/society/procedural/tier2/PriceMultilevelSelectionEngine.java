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
 * Multilevel Cultural Selection & Price Equation Engine (George R. Price, 1970).
 *
 * <p>Models the evolution of altruistic cooperation, prosocial norms, and military solidarity
 * through between-group vs within-group cultural selection pressures:</p>
 * <pre>
 *   Δz̄ = Cov(w_i, z_i) / w̄  +  E[w_i · Δz_i] / w̄
 * </pre>
 * where:
 * <ul>
 *   <li><b>Cov(w_i, z_i) / w̄</b>: Between-group selection term (prosocial/altruistic groups win inter-polity wars).</li>
 *   <li><b>E[w_i · Δz_i] / w̄</b>: Within-group individual selection term (free-riders / selfish agents outcompete altruists internally).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.4.0
 */
public class PriceMultilevelSelectionEngine implements ProceduralEnginePlugin {
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
                .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0)
                .average().orElse(10.0);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;

            // Group-level prosociality / cohesion index z = (1.0 - gini)
            double prosociality = Math.clamp(1.0 - gini, 0.0, 1.0);

            // Between-group selective advantage: high cooperation increases collective labor output
            double betweenGroupAdvantage = prosociality * 0.03 * (capital / Math.max(1.0, meanWealth));
            // Within-group free-rider erosion: high luxury/inequality erodes civic solidarity
            double withinGroupErosion = (gini > 0.45) ? (gini - 0.45) * 0.02 : 0.0;

            double netAltruismShift = (betweenGroupAdvantage - withinGroupErosion) * deltaYears;
            double currentWork = cell.getResourceWork() != null ? cell.getResourceWork() : 50.0;
            cell.setResourceWork(Math.max(10.0, currentWork * (1.0 + netAltruismShift)));
        }
    }
}
