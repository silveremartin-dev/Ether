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
 * Migrate behavior - move to a better location.
 */
public class MigrateBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(MigrateBehavior.class);
    private static final Random random = new Random();

    @Override
    public String getName() {
        return "Migrate";
    }

    @Override
    public double calculateUtility(Human human, World world) {
        Cell currentCell = world.getCell(human.getX(), human.getY());
        if (currentCell == null)
            return 0.5; // Should move if in null cell

        // Low resources = high migration utility
        double currentFood = currentCell.getResource("Food");
        double currentWater = currentCell.getResource("Water");

        double resourceScore = (currentFood + currentWater) / 40.0;

        // Inverse - migrate when current location is poor
        double migrationNeed = Math.max(0, 0.8 - resourceScore);

        // Temperature considerations
        double temp = currentCell.getTemperature();
        if (temp < -10 || temp > 40) {
            migrationNeed += 0.3; // Extreme temps drive migration
        }

        // Low energy = less likely to migrate (costly)
        if (human.getEnergy() < 30) {
            migrationNeed *= 0.5;
        }

        return Math.min(1.0, migrationNeed);
    }

    @Override
    public boolean execute(Human human, World world) {
        // Find best adjacent cell
        int bestX = human.getX();
        int bestY = human.getY();
        double bestScore = -1;

        // Check 8 directions
        int[][] directions = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };

        for (int[] dir : directions) {
            int newX = human.getX() + dir[0];
            int newY = human.getY() + dir[1];

            if (newX < 0 || newX >= world.getWidth() || newY < 0 || newY >= world.getHeight()) {
                continue;
            }

            Cell cell = world.getCell(newX, newY);
            if (cell == null)
                continue;

            // Skip water cells
            if (cell.getBiome() == Biome.OCEAN) {
                continue;
            }

            // Score based on resources and temperature
            double score = cell.getResource("Food") + cell.getResource("Water");
            double temp = cell.getTemperature();
            if (temp > 5 && temp < 30)
                score += 10; // Comfortable temp bonus

            // Add some randomness for exploration
            score += random.nextDouble() * 5;

            if (score > bestScore) {
                bestScore = score;
                bestX = newX;
                bestY = newY;
            }
        }

        // Move if found better location
        if (bestX != human.getX() || bestY != human.getY()) {
            human.setX(bestX);
            human.setY(bestY);
            human.setEnergy(Math.max(0, human.getEnergy() - 10)); // Movement costs energy

            logger.trace("Human {} migrated to ({}, {})", human.getId(), bestX, bestY);
            return true;
        }

        return false;
    }
}

