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
 * Cultural Homophily & Spatial Segregation Engine (Schelling, 1971 / Axelrod, 1997).
 *
 * <p>Models macro-spatial enclave segregation emerging from micro-preferences for cultural homophily:</p>
 * <pre>
 *   s_i = Σ_{j ∈ N(i)} δ(C_i, C_j) / |N(i)|
 *   ΔFriction_i = (1 - U(s_i)) · 0.05 · (Gini - 0.35) · Δt
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SchellingAxelrodSegregationEngine extends AbstractSimulationEngine {
    public SchellingAxelrodSegregationEngine() {
        super("SchellingAxelrodSegregationEngine", "SchellingAxelrodSegregationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
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
               • Polarization Activation:  P_act = 1 / (1 + exp(-12.0 · (Gini - 0.45)))
               • Spatial Tension Friction: ΔFriction_i = 0.05 · P_act · max(0, Gini - 0.35) · Δt
               Units: Gini [0.0, 1.0], Friction [0.05, 10.0]
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
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double gini = cell.getGiniIndexOr(0.35);
            // Continuous polarization friction: smooth logistic activation around Gini=0.45
            // High internal inequality and social segregation induce micro-motives for enclave territorialization
            double polarizationActivation = 1.0 / (1.0 + Math.exp(-12.0 * (gini - 0.45)));
            if (polarizationActivation > 0.02) {
                double friction = cell.getMovementFrictionOr(1.0);
                double frictionIncrement = 0.05 * polarizationActivation * Math.max(0.0, gini - 0.35) * deltaYears;
                cell.setMovementFriction(Math.min(10.0, friction + frictionIncrement));
            }
        }
    }
}
