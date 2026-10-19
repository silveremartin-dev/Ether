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
 * Combinatorial Technological Evolution Engine (W. Brian Arthur, 2009).
 *
 * <p>Models technological innovation not as a linear scalar progress, but as recombinant
 * synthesis of modular technological building blocks (autocatalytic combinatorial sets):</p>
 * <pre>
 *   T_new = T_i ⊗ T_j  if  Research_Capital ≥ Cost(T_i, T_j)
 *   Innovation_Rate = k_comb · Tech_Primitives^(1.5)
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ArthurCombinatorialTechnologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(ArthurCombinatorialTechnologyEngine.class);

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Arthur Combinatorial Technological Evolution";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Models technological evolution through recursive combinatorial recombinant assembly of existing modular primitives.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [W. Brian Arthur Combinatorial Evolution (The Nature of Technology 2009)]
               • Combinatorial Space: |Combinations| = C(N_primitives, 2) âˆ N^2
               • Emergence Rate:      dTech/dt = μ_comb · (Tech_level)^1.25 · (Capital_R&D / Pop)^0.5
               • Autocatalysis:       Every new invention becomes a candidate building block for future inventions.
               Units: Tech [niveau technologique sans dimension], Capital_R&D [Joules / $]
               Ref: W. B. Arthur (2009) "The Nature of Technology: What It Is and How It Evolves"
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Socio-Technical Evolution";
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
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop < 100) continue;

            double tech = cell.getTechnologyLevel() != null ? Math.max(1.0, cell.getTechnologyLevel()) : 1.0;
            double capital = cell.getResourceCapitalOr(10.0);

            // Combinatorial innovation rate scales super-linearly with existing tech primitives (Arthur 2009)
            double rAndDIntensity = Math.clamp(capital / Math.max(10.0, pop * 0.05), 0.0, 5.0);
            double innovationRate = 0.008 * Math.pow(tech, 1.25) * Math.sqrt(rAndDIntensity) * deltaYears;
            cell.setTechnologyLevel(tech + innovationRate);
        }
    }
}



