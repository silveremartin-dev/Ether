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
 * @version 1.0.0-beta.1
 */
public class ArthurCombinatorialTechnologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(ArthurCombinatorialTechnologyEngine.class);

    @Override
    public String getName() {
        return "Arthur Combinatorial Technological Evolution";
    }

    @Override
    public String getDescription() {
        return "Models technological evolution through recursive combinatorial recombinant assembly of existing modular primitives.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [W. Brian Arthur Combinatorial Evolution (The Nature of Technology 2009)]
               • Combinatorial Space: |Combinations| = C(N_primitives, 2) ∝ N^2
               • Emergence Rate:      dTech/dt = μ_comb · (Tech_level)^1.25 · (Capital_R&D / Pop)^0.5
               • Autocatalysis:       Every new invention becomes a candidate building block for future inventions.
               Units: Tech [niveau technologique sans dimension], Capital_R&D [Joules / $]
               Ref: W. B. Arthur (2009) "The Nature of Technology: What It Is and How It Evolves"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Socio-Technical Evolution";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 100) continue;

            double tech = cell.getTechnologyLevel() != null ? Math.max(1.0, cell.getTechnologyLevel()) : 1.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 10.0;

            // Combinatorial innovation rate scales super-linearly with existing tech primitives (Arthur 2009)
            double rAndDIntensity = Math.min(5.0, capital / Math.max(10.0, pop * 0.05));
            double innovationRate = 0.008 * Math.pow(tech, 1.25) * Math.sqrt(rAndDIntensity) * deltaYears;
            cell.setTechnologyLevel(tech + innovationRate);
        }
    }
}

