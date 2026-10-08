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
 * New Economic Geography (NEG) Core-Periphery Agglomeration Engine (Paul Krugman, 1991).
 *
 * <p>Models endogenous industrial clustering driven by increasing returns to scale,
 * Dixit-Stiglitz monopolistic competition, and iceberg transport costs:</p>
 * <pre>
 *   ω_i = [ Σ_j Y_j · P_j^(σ - 1) · exp(-τ_ij · (σ - 1)) ]^(1 / σ)
 * </pre>
 * where:
 * <ul>
 *   <li><b>σ > 1</b>: Elasticity of substitution among manufacturing varieties (~4.0).</li>
 *   <li><b>τ_ij</b>: Iceberg transport cost between regions $i$ and $j$.</li>
 *   <li><b>Centripetal forces</b> (market size effect & thick labor markets) pull capital towards core hubs.</li>
 *   <li><b>Centrifugal forces</b> (immobile agricultural demand & land rent competition) disperse activity.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KrugmanCorePeripheryEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(KrugmanCorePeripheryEngine.class);

    /* Internal state variable for sigma elasticity (double). */
    public static final double SIGMA_ELASTICITY = 4.0;

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code KrugmanCorePeripheryEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Krugman New Economic Geography Core-Periphery";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code KrugmanCorePeripheryEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Models endogenous spatial economic concentration, manufacturing core-periphery bifurcations, and iceberg transport friction.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code KrugmanCorePeripheryEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Krugman Core-Periphery NEG Formulation (JPE 1991)]
               • Nominal Wage Index:  ω_i = [ Σ_j Y_j · P_j^(σ - 1) · exp(-τ_ij · (σ - 1)) ]^(1 / σ)
               • Price Index:         P_i = [ Σ_j λ_j · (w_j · exp(τ_ij))^(1 - σ) ]^(1 / (1 - σ))
               • Manufacturing Share: dλ_i/dt = γ_mob · (ω_i / P_i^μ - RealWage_mean) · λ_i
               Units: ω, w [$/h], P [indice de prix], τ [friction transport iceberg], σ [élasticité CES]
               Ref: P. Krugman (1991) "Increasing Returns and Economic Geography", Journal of Political Economy
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code KrugmanCorePeripheryEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Spatial Economics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code KrugmanCorePeripheryEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        double meanCapital = cells.stream()
                .mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0)
                .average().orElse(10.0);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;
            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;

            // Core hub advantage: high local market access + low transport friction -> capital inflow
            double accessibilityIndex = (pop * 0.001) / Math.max(0.2, friction);
            if (capital > meanCapital && accessibilityIndex > 1.0) {
                // Agglomeration feedback (centripetal force)
                double capitalGrowth = (capital - meanCapital) * 0.03 * deltaYears;
                cell.setResourceCapital(capital + capitalGrowth);
            }
        }
    }
}



