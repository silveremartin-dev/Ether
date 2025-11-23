package com.ether.society.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Human extends Agent {
    private static final Random random = new Random();
    private final String genetics; // Simplified genetic code
    private final String cultureId; // Cultural attachment
    private final Map<String, Double> inventory = new HashMap<>();

    private double happiness;

    public Human(int x, int y, String genetics, String cultureId) {
        super(x, y);
        this.genetics = genetics;
        this.cultureId = cultureId;
        this.happiness = 100.0;
    }

    @Override
    public void tick(World world) {
        if (!isAlive())
            return;

        incrementAge();
        consumeFood();

        // Movement: Move toward resources if hungry
        if (getEnergy() < 50.0 || inventory.getOrDefault("Food", 0.0) < 5.0) {
            moveTowardResources(world);
        } else {
            // Random walk
            randomMove(world);
        }

        // Gather resources from current cell
        gatherResources(world);

        // Reproduction
        attemptReproduction(world);

        // Natural death from old age
        if (getAge() > 960) { // 80 years * 12 months
            if (random.nextDouble() < 0.01) { // 1% chance per month
                setAlive(false);
            }
        }
    }

    private void consumeFood() {
        // Simple consumption logic
        double foodNeeded = 1.0;
        double waterNeeded = 0.5;

        if (inventory.getOrDefault("Food", 0.0) >= foodNeeded) {
            inventory.put("Food", inventory.get("Food") - foodNeeded);
            setEnergy(Math.min(100.0, getEnergy() + 10.0));
            happiness = Math.min(100.0, happiness + 2.0);
        } else {
            setEnergy(Math.max(0.0, getEnergy() - 10.0));
            happiness = Math.max(0.0, happiness - 5.0);
            if (getEnergy() <= 0) {
                setHealth(getHealth() - 10.0);
                if (getHealth() <= 0) {
                    setAlive(false);
                }
            }
        }

        if (inventory.getOrDefault("Water", 0.0) >= waterNeeded) {
            inventory.put("Water", inventory.get("Water") - waterNeeded);
        } else {
            setHealth(Math.max(0.0, getHealth() - 5.0));
            if (getHealth() <= 0) {
                setAlive(false);
            }
        }
    }

    private void moveTowardResources(World world) {
        // Check adjacent cells for resources
        int bestDx = 0, bestDy = 0;
        double bestScore = -1.0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0)
                    continue;

                int checkX = getX() + dx;
                int checkY = getY() + dy;

                if (checkX >= 0 && checkX < world.getWidth() && checkY >= 0 && checkY < world.getHeight()) {
                    Cell cell = world.getCell(checkX, checkY);
                    double score = cell.getResource("Food") + cell.getResource("Water") * 0.5;

                    if (score > bestScore) {
                        bestScore = score;
                        bestDx = dx;
                        bestDy = dy;
                    }
                }
            }
        }

        if (bestScore > 0) {
            move(bestDx, bestDy, world);
        } else {
            randomMove(world);
        }
    }

    private void randomMove(World world) {
        int dx = random.nextInt(3) - 1; // -1, 0, 1
        int dy = random.nextInt(3) - 1;
        move(dx, dy, world);
    }

    private void gatherResources(World world) {
        Cell cell = world.getCell(getX(), getY());

        // Gather food
        double foodAvailable = cell.getResource("Food");
        double foodToGather = Math.min(5.0, foodAvailable);
        if (foodToGather > 0) {
            cell.consumeResource("Food", foodToGather);
            addToInventory("Food", foodToGather);
        }

        // Gather water
        double waterAvailable = cell.getResource("Water");
        double waterToGather = Math.min(3.0, waterAvailable);
        if (waterToGather > 0) {
            cell.consumeResource("Water", waterToGather);
            addToInventory("Water", waterToGather);
        }
    }

    private void attemptReproduction(World world) {
        // Only reproduce if healthy, happy, and adult
        if (getAge() < 192)
            return; // 16 years * 12 months
        if (getAge() > 600)
            return; // 50 years * 12 months
        if (happiness < 60.0)
            return;
        if (getEnergy() < 70.0)
            return;
        if (inventory.getOrDefault("Food", 0.0) < 10.0)
            return;

        // Low chance per month
        if (random.nextDouble() < 0.02) { // 2% chance
            // Create offspring
            Human child = new Human(getX(), getY(), genetics, cultureId);
            world.addAgent(child);

            // Cost of reproduction
            setEnergy(getEnergy() - 20.0);
            happiness -= 10.0;
            inventory.put("Food", inventory.get("Food") - 5.0);
        }
    }

    public String getGenetics() {
        return genetics;
    }

    public String getCultureId() {
        return cultureId;
    }

    public double getHappiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = happiness;
    }

    public void addToInventory(String item, double amount) {
        inventory.merge(item, amount, Double::sum);
    }

    public double getInventoryAmount(String item) {
        return inventory.getOrDefault(item, 0.0);
    }
}
