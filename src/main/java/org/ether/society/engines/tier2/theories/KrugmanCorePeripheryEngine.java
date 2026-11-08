/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Krugman Core-Periphery &amp; New Economic Geography Engine (Paul Krugman, Nobel 1991).
 *
 * <p>Models endogenous spatial agglomeration, trade costs, and forward-backward linkages:</p>
 * <pre>
 *   Ω_i = Σ_j [ Y_j · exp(-τ · d_ij) ]
 *   dK_i/dt = Centripetal_Force(Ω_i) - Centrifugal_Congestion(K_i / Area_i)
 * </pre>
 * where:
 * <ul>
 *   <li><b>Centripetal Forces</b>: Increasing returns in manufacturing, labor market pooling, low transport costs.</li>
 *   <li><b>Centrifugal Forces</b>: Agricultural dispersion, land rents, transport congestion.</li>
 *   <li><b>Bifurcation Point</b>: Below critical transport friction $\tau &lt; \tau^*$, symmetrical distribution collapses into core-periphery clustering.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KrugmanCorePeripheryEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(KrugmanCorePeripheryEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Krugman Core-Periphery Spatial Agglomeration (NEG)";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models endogenous industrial clustering, forward/backward linkages, and bifurcation from dispersed to core-periphery geography.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Krugman New Economic Geography (JPE 1991)]
               • Market Potential:      Ω_i = Σ_j [ Income_j · exp(-τ · dist_ij) ]
               • Agglomeration Factor:  A_i = σ(2.0 · (K_i / K_mean - 1)) · (Access_i / (1 + Access_i))
               • Friction Reduction:    Δμ = -2% · A_i · Δt
               • Bifurcation Threshold: Symmetrical distribution destabilizes when τ falls below critical value.
               Units: Ω [potentiel de marché sans dimension], τ [coefficient de friction transport km^-1]
               Ref: P. Krugman (1991) "Increasing Returns and Economic Geography", Journal of Political Economy
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Spatial Economics";
    }

    /**
     * Process.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        double meanCapital = cells.stream()
                .filter(java.util.Objects::nonNull)
                .mapToDouble(c -> c.getResourceCapitalOr(0.0))
                .average().orElse(10.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(10.0);
            double friction = cell.getMovementFrictionOr(1.0);

            // Core hub advantage: high local market access + low transport friction -> capital inflow
            double accessibilityIndex = (pop * 0.001) / Math.max(0.2, friction);

            // Continuous centripetal agglomeration potential (Krugman 1991, Fujita et al. 1999)
            double relCapital = capital / Math.max(1.0, meanCapital);
            double capitalAdvantage = 1.0 / (1.0 + Math.exp(-2.0 * (relCapital - 1.0)));
            double accessAdvantage = accessibilityIndex / (1.0 + accessibilityIndex);
            double centripetalFactor = 2.0 * capitalAdvantage * accessAdvantage;

            // Agglomeration feedback (centripetal force): enhances local market potential, reduces transport friction,
            // and shifts capital from high-friction periphery toward low-friction core hubs (Krugman 1991, eq. dK_i/dt)
            cell.setMovementFriction(Math.max(0.1, friction * (1.0 - 0.02 * centripetalFactor * deltaYears)));
            double centrifugalCongestion = 0.01 * (relCapital / Math.max(1.0, accessibilityIndex));
            double netAgglomerationRate = (0.015 * centripetalFactor - centrifugalCongestion) * deltaYears;
            cell.setResourceCapital(Math.max(1.0, capital * (1.0 + Math.clamp(netAgglomerationRate, -0.05, 0.08))));
        }
    }
}
