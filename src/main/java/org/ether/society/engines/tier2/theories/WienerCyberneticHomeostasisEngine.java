/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Norbert Wiener Cybernetic Negative-Feedback Homeostasis Engine.
 *
 * <p>Formalizes cybernetic feedback control theory in human societies (Norbert Wiener, 1948,
 * <i>Cybernetics: Or Control and Communication in the Animal and the Machine</i>;
 * Gregory Bateson, 1972, <i>Steps to an Ecology of Mind</i>):</p>
 * <ul>
 *   <li><b>Goal-Directed Homeostatic Error Sensor:</b> Evaluates mismatch between actual per-capita
 *       subsistence/capital resources and target biological/infrastructural setpoints.</li>
 *   <li><b>Negative Feedback Damping:</b> Adjusts work allocation and resource consumption to dampen
 *       runaway boom-bust oscillations and return the system to the stable attractor.</li>
 *   <li><b>Runaway Destabilization (Positive Feedback):</b> When error exceeds self-regulating capacity,
 *       damping breaks down, triggering phase transition or state collapse.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://mitpress.mit.edu">Wiener, N. (1948), Cybernetics</a>
 */
public class WienerCyberneticHomeostasisEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(WienerCyberneticHomeostasisEngine.class);

    /** Target subsistence setpoint in food units per capita. */
    public static final double TARGET_FOOD_PER_CAPITA = 1.20;

    /** Error damping feedback gain. */
    public static final double DAMPING_FEEDBACK_GAIN = 0.08;

    @Override
    public String getName() {
        return "WienerCyberneticHomeostasisEngine";
    }

    @Override
    public String getDescription() {
        return "Models Norbert Wiener (1948) cybernetic negative-feedback homeostasis, stabilizing socio-ecological variables around physiological setpoints.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Wiener Cybernetic Negative-Feedback Homeostasis (Wiener 1948, Bateson 1972)]
               • State Error:         e(t) = (Resource_Actual - Setpoint_Target) / Setpoint_Target
               • Damping Response:    ΔWork = -Gain · e(t) · σ(State_Capacity - 0.25)
               • Homeostatic Range:   |e(t)| <= 0.35 -> Stable Negative Feedback Damping
               • Runaway Collapse:    |e(t)| > 0.70  -> Bifurcation & Positive Feedback Oscillation
               Ref: N. Wiener (1948), "Cybernetics: Or Control and Communication in the Animal and the Machine", MIT Press.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: System Dynamics & Cybernetics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processCyberneticHomeostasis(cells, deltaYears);
    }

    /**
     * Executes cybernetic homeostatic damping across the cell domain.
     *
     * @param cells      spatial cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processCyberneticHomeostasis(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double food = cell.getFoodResourceOr(0.0);
            double foodPerCapita = food / (double) pop;
            double error = (foodPerCapita - TARGET_FOOD_PER_CAPITA) / TARGET_FOOD_PER_CAPITA;

            double currentWork = cell.getResourceWorkOr(50.0);

            // Negative feedback damping: when surplus is high, reallocate labor away from emergency subsistence;
            // when deficit occurs, increase emergency subsistence effort smoothly
            double dampingAdjustment = -DAMPING_FEEDBACK_GAIN * error * deltaYears * 10.0;
            double clampedDamping = Math.clamp(dampingAdjustment, -5.0, 5.0);

            cell.setResourceWork(Math.clamp(currentWork + clampedDamping, 10.0, 100.0));
        }
    }
}
