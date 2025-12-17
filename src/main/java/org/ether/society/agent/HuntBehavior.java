/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.agent;

import org.ether.society.model.Biome;
import org.ether.society.model.Cell;
import org.ether.society.model.Human;
import org.ether.society.model.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Hunt behavior - hunt animals for food (high yield but risky).
 */
public class HuntBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(HuntBehavior.class);
    private static final Random random = new Random();

    @Override
    public String getName() {
        return "Hunt";
    }

    @Override
    public double calculateUtility(Human human, World world) {
        // Need good energy to hunt
        if (human.getEnergy() < 40)
            return 0.0;

        // Check if biome supports hunting
        Cell cell = world.getCell(human.getX(), human.getY());
        if (cell == null)
            return 0.0;

        // Good hunting biomes
        Biome biome = cell.getBiome();
        double huntingPotential = switch (biome) {
            case FOREST -> 0.9;
            case PLAINS -> 0.8;
            case JUNGLE -> 0.7;
            case HILLS -> 0.6;
            case TUNDRA -> 0.5;
            case MOUNTAINS -> 0.4;
            default -> 0.1;
        };

        // Low on food = higher hunting utility
        double foodNeed = Math.max(0, 1.0 - (human.getInventoryAmount("Food") / 15.0));

        // Combine factors
        return Math.min(1.0, huntingPotential * foodNeed * 0.7);
    }

    @Override
    public boolean execute(Human human, World world) {
        // Hunt attempt - success depends on skill and luck
        double successChance = 0.3 + (human.getHealth() / 200.0); // 30-80% base chance

        if (random.nextDouble() < successChance) {
            // Successful hunt - good food yield
            double meat = 8.0 + random.nextDouble() * 7.0; // 8-15 food
            human.addToInventory("Food", meat);
            human.setEnergy(Math.max(0, human.getEnergy() - 20));

            logger.debug("Human {} successful hunt - gained {} meat",
                    human.getId().substring(0, 8), meat);
            return true;
        } else {
            // Failed hunt - wasted energy, possible injury
            human.setEnergy(Math.max(0, human.getEnergy() - 15));

            // Small chance of injury
            if (random.nextDouble() < 0.1) {
                human.setHealth(Math.max(0, human.getHealth() - 10));
                logger.debug("Human {} injured during hunt", human.getId().substring(0, 8));
            }

            return false;
        }
    }

    @Override
    public boolean canExecute(Human human, World world) {
        // Need minimum energy to hunt
        if (human.getEnergy() < 40)
            return false;

        // Check biome
        Cell cell = world.getCell(human.getX(), human.getY());
        if (cell == null)
            return false;

        // Can't hunt in water or desert
        Biome biome = cell.getBiome();
        return biome != Biome.OCEAN && biome != Biome.DESERT;
    }
}

