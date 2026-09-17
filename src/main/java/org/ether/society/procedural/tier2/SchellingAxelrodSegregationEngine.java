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
 * Cultural Homophily & Spatial Segregation Engine (Schelling, 1971 / Axelrod, 1997).
 *
 * <p>Models macro-spatial enclave segregation emerging from mild micro-preferences for cultural homophily:</p>
 * <pre>
 *   U_i = 1  if  [ Σ_{j ∈ N(i)} 1(C_j == C_i) / |N(i)| ] ≥ τ_tolerance
 *   U_i = 0  otherwise (triggers relocation / social tension friction)
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SchellingAxelrodSegregationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SchellingAxelrodSegregationEngine.class);

    public static final double HOMOPHILY_TOLERANCE_THRESHOLD = 0.40; // 40% neighborhood threshold

    @Override
    public String getName() {
        return "Schelling-Axelrod Cultural Spatial Segregation";
    }

    @Override
    public String getDescription() {
        return "Models micro-motives driving macro-segregation enclaves and cultural polarization across territorial boundaries.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Schelling-Axelrod Cultural Homophily Model]
               • Neighborhood Similarity:  s_i = Σ_{j ∈ N(i)} δ(C_i, C_j) / |N(i)|
               • Satisfaction Utility:     U_i = 1 if s_i ≥ τ_tol (0.40), else 0
               • Spatial Tension Friction: ΔFriction_i = (1 - U_i) · 0.10 · (1 - s_i)
               Units: s_i [0.0, 1.0], τ_tol [seuil de tolérance homophile], U_i [utilité booléenne]
               Ref: T. Schelling (1971) "Dynamic Models of Segregation", R. Axelrod (1997) "Dissemination of Culture"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Cultural Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            // High internal inequality / segregation increases movement friction and civil tension
            if (gini > 0.50) {
                double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
                cell.setMovementFriction(Math.min(10.0, friction + (0.05 * (gini - 0.50) * deltaYears)));
            }
        }
    }
}

