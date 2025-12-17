/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.agent;

import org.ether.society.model.Human;
import org.ether.society.model.World;

/**
 * Interface for agent behaviors using utility-based AI.
 * Each behavior calculates a utility score and can be executed.
 */
public interface Behavior {

    /**
     * Get the name of this behavior.
     */
    String getName();

    /**
     * Calculate utility score (0.0 - 1.0) for this behavior.
     * Higher scores mean the behavior is more desirable.
     * 
     * @param human The human agent
     * @param world The simulation world
     * @return Utility score between 0.0 and 1.0
     */
    double calculateUtility(Human human, World world);

    /**
     * Execute this behavior.
     * 
     * @param human The human agent
     * @param world The simulation world
     * @return true if behavior was successfully executed
     */
    boolean execute(Human human, World world);

    /**
     * Check if this behavior can be executed.
     * 
     * @param human The human agent
     * @param world The simulation world
     * @return true if preconditions are met
     */
    default boolean canExecute(Human human, World world) {
        return true;
    }
}

