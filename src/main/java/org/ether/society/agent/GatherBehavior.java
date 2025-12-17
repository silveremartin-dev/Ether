/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.agent;

import org.ether.society.model.Cell;
import org.ether.society.model.Human;
import org.ether.society.model.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gather behavior - collect resources from the current cell.
 */
public class GatherBehavior implements Behavior {
    private static final Logger logger = LoggerFactory.getLogger(GatherBehavior.class);

    @Override
    public String getName() {
        return "Gather";
    }

    @Override
    public double calculateUtility(Human human, World world) {
        // High utility when low on food or other resources
        double foodInventory = human.getInventoryAmount("Food");
        double waterInventory = human.getInventoryAmount("Water");

        // Urgency increases as resources deplete
        double foodUrgency = Math.max(0, 1.0 - (foodInventory / 20.0));
        double waterUrgency = Math.max(0, 1.0 - (waterInventory / 20.0));

        // Check if current cell has resources
        Cell cell = world.getCell(human.getX(), human.getY());
        if (cell == null)
            return 0.0;

        double cellFood = cell.getResource("Food");
        double cellWater = cell.getResource("Water");

        // Can't gather if nothing here
        if (cellFood <= 0 && cellWater <= 0)
            return 0.0;

        // Combine urgency with availability
        double utility = (foodUrgency * 0.6 + waterUrgency * 0.4);

        // Boost if resources are plentiful
        if (cellFood > 10 || cellWater > 10) {
            utility += 0.2;
        }

        return Math.min(1.0, utility);
    }

    @Override
    public boolean execute(Human human, World world) {
        Cell cell = world.getCell(human.getX(), human.getY());
        if (cell == null)
            return false;

        double gathered = 0;

        // Gather food
        double cellFood = cell.getResource("Food");
        if (cellFood > 0) {
            double toGather = Math.min(5.0, cellFood);
            cell.consumeResource("Food", toGather);
            human.addToInventory("Food", toGather);
            gathered += toGather;
        }

        // Gather water
        double cellWater = cell.getResource("Water");
        if (cellWater > 0) {
            double toGather = Math.min(3.0, cellWater);
            cell.consumeResource("Water", toGather);
            human.addToInventory("Water", toGather);
            gathered += toGather;
        }

        // Costs energy
        human.setEnergy(Math.max(0, human.getEnergy() - 5));

        logger.trace("Human {} gathered {:.1f} resources", human.getId(), gathered);
        return gathered > 0;
    }

    @Override
    public boolean canExecute(Human human, World world) {
        Cell cell = world.getCell(human.getX(), human.getY());
        return cell != null && (cell.getResource("Food") > 0 || cell.getResource("Water") > 0);
    }
}

