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

/**
 * Rest behavior - recover energy when tired.
 */
public class RestBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(RestBehavior.class);

    @Override
    public String getName() {
        return "Rest";
    }

    @Override
    public double calculateUtility(Human human, World world) {
        // High utility when low on energy
        double energyLevel = human.getEnergy() / 100.0;

        // Inverse relationship - lower energy = higher rest utility
        double restNeed = 1.0 - energyLevel;

        // Boost if health is also low
        if (human.getHealth() < 50) {
            restNeed += 0.2;
        }

        return Math.min(1.0, restNeed * 0.8);
    }

    @Override
    public boolean execute(Human human, World world) {
        // Recover energy
        double energyGain = 15.0;

        // Food improves recovery
        if (human.getInventoryAmount("Food") > 0) {
            energyGain += 5.0;
            human.addToInventory("Food", -0.5);
        }

        human.setEnergy(Math.min(100.0, human.getEnergy() + energyGain));

        // Slight health recovery
        if (human.getEnergy() > 50) {
            human.setHealth(Math.min(100.0, human.getHealth() + 2.0));
        }

        logger.trace("Human {} rested, energy now {:.1f}", human.getId(), human.getEnergy());
        return true;
    }
}

