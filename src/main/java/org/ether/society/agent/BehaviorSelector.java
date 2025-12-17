/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.agent;

import org.ether.society.model.Human;
import org.ether.society.model.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AI brain that selects and executes behaviors based on utility scores.
 * Uses utility-based AI: evaluates all behaviors and picks the highest utility.
 */
public class BehaviorSelector {
    private static final Logger logger = LoggerFactory.getLogger(BehaviorSelector.class);

    private final List<Behavior> behaviors;

    public BehaviorSelector() {
        this.behaviors = new ArrayList<>();
        registerDefaultBehaviors();
    }

    /**
     * Register default human behaviors.
     */
    private void registerDefaultBehaviors() {
        behaviors.add(new GatherBehavior());
        behaviors.add(new HuntBehavior());
        behaviors.add(new MigrateBehavior());
        behaviors.add(new ReproduceBehavior());
        behaviors.add(new RestBehavior());
    }

    /**
     * Add a custom behavior.
     */
    public void addBehavior(Behavior behavior) {
        behaviors.add(behavior);
    }

    /**
     * Select the best behavior based on utility scores.
     */
    public Behavior selectBehavior(Human human, World world) {
        return behaviors.stream()
                .filter(b -> b.canExecute(human, world))
                .max(Comparator.comparingDouble(b -> b.calculateUtility(human, world)))
                .orElse(null);
    }

    /**
     * Execute a full decision cycle for the agent.
     * Returns the behavior that was executed, or null if none.
     */
    public Behavior executeDecision(Human human, World world) {
        Behavior best = selectBehavior(human, world);

        if (best != null) {
            double utility = best.calculateUtility(human, world);

            if (utility > 0.1) { // Minimum threshold
                boolean success = best.execute(human, world);

                if (success) {
                    logger.trace("Human {} executed {} (utility: {:.2f})",
                            human.getId().substring(0, 8), best.getName(), utility);
                    return best;
                }
            }
        }

        return null;
    }

    /**
     * Get all behavior utilities for debugging.
     */
    public List<BehaviorScore> getAllUtilities(Human human, World world) {
        List<BehaviorScore> scores = new ArrayList<>();

        for (Behavior b : behaviors) {
            double utility = b.canExecute(human, world) ? b.calculateUtility(human, world) : 0.0;
            scores.add(new BehaviorScore(b.getName(), utility, b.canExecute(human, world)));
        }

        scores.sort((a, b) -> Double.compare(b.utility(), a.utility()));
        return scores;
    }

    /**
     * Record for behavior score debugging.
     */
    public record BehaviorScore(String name, double utility, boolean canExecute) {
    }
}
