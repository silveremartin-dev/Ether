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

import java.util.List;
import java.util.Random;

/**
 * Reproduce behavior - create offspring when conditions are favorable.
 */
public class ReproduceBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(ReproduceBehavior.class);
    private static final Random random = new Random();

    private static final int MIN_REPRODUCE_AGE = 180; // 15 years = 180 months
    private static final int MAX_REPRODUCE_AGE = 540; // 45 years = 540 months
    private static final double MIN_HEALTH = 60.0;
    private static final double MIN_ENERGY = 50.0;
    private static final double MIN_FOOD = 10.0;

    @Override
    public String getName() {
        return "Reproduce";
    }

    @Override
    public double calculateUtility(Human human, World world) {
        // Check basic requirements
        if (!canExecute(human, world))
            return 0.0;

        // Find a potential partner nearby
        Human partner = findPartner(human, world);
        if (partner == null)
            return 0.0;

        // High resources = higher reproduction utility
        double foodSecurity = Math.min(1.0, human.getInventoryAmount("Food") / 30.0);
        double healthScore = human.getHealth() / 100.0;
        double happiness = human.getHappiness() / 100.0;

        // Combine factors
        double utility = (foodSecurity * 0.4 + healthScore * 0.3 + happiness * 0.3) * 0.7;

        return Math.min(1.0, utility);
    }

    @Override
    public boolean execute(Human human, World world) {
        Human partner = findPartner(human, world);
        if (partner == null)
            return false;

        // Create offspring
        String childGenetics = combineGenetics(human.getGenetics(), partner.getGenetics());
        String childCulture = random.nextBoolean() ? human.getCultureId() : partner.getCultureId();

        Human child = new Human(human.getX(), human.getY(), childGenetics, childCulture);
        child.setAge(0);
        child.setHealth(100.0);
        child.setEnergy(50.0);

        // Add to world
        world.addAgent(child);

        // Reproduction costs
        human.setEnergy(Math.max(0, human.getEnergy() - 30));
        human.addToInventory("Food", -5.0);
        partner.setEnergy(Math.max(0, partner.getEnergy() - 30));

        logger.debug("New human born at ({}, {}) - parents: {} + {}",
                child.getX(), child.getY(), human.getId().substring(0, 8), partner.getId().substring(0, 8));

        return true;
    }

    @Override
    public boolean canExecute(Human human, World world) {
        // Age requirements
        if (human.getAge() < MIN_REPRODUCE_AGE || human.getAge() > MAX_REPRODUCE_AGE) {
            return false;
        }

        // Health and energy requirements
        if (human.getHealth() < MIN_HEALTH || human.getEnergy() < MIN_ENERGY) {
            return false;
        }

        // Resource requirements
        if (human.getInventoryAmount("Food") < MIN_FOOD) {
            return false;
        }

        return true;
    }

    /**
     * Find a partner in the same cell.
     */
    private Human findPartner(Human human, World world) {
        List<? extends org.ether.society.model.Agent> agents = world.getAgents();

        for (var agent : agents) {
            if (!(agent instanceof Human other))
                continue;
            if (other.getId().equals(human.getId()))
                continue;

            // Same location
            if (other.getX() != human.getX() || other.getY() != human.getY())
                continue;

            // Partner must also meet requirements
            if (!canExecute(other, world))
                continue;

            // Prefer same culture (but not required)
            return other;
        }

        return null;
    }

    /**
     * Combine parent genetics (simplified - just concatenate and hash).
     */
    private String combineGenetics(String g1, String g2) {
        return Integer.toHexString((g1 + g2).hashCode() + random.nextInt(1000));
    }
}

