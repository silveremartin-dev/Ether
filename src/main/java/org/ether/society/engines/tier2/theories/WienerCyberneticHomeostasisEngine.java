/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.math.SmoothMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * @version 1.0.0-beta.3
 * @see <a href="https://mitpress.mit.edu">Wiener, N. (1948), Cybernetics</a>
 */
public class WienerCyberneticHomeostasisEngine extends AbstractSimulationEngine {
    public WienerCyberneticHomeostasisEngine() {
        super("WienerCyberneticHomeostasisEngine", "WienerCyberneticHomeostasisEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double food = buffer.getFoodResource()[i];
        double foodPerCapita = food / pop;
        double error = (foodPerCapita - TARGET_FOOD_PER_CAPITA) / TARGET_FOOD_PER_CAPITA;

        // Negative feedback damping: when food deficit occurs, mobilize emergency subsistence effort;
        // when surplus is high, stabilize institutional/work pressure
        double dampingAdjustment = -DAMPING_FEEDBACK_GAIN * error * dtYears;
        if (error < 0.0) {
            acc.dFoodResource += Math.min(pop * 0.1, -error * pop * 0.02 * dtYears);
        }
        acc.dFluxPressure += Math.clamp(dampingAdjustment, -0.05, 0.05);
    }
}
