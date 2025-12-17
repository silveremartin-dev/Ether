/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.model;

import org.ether.society.agent.BehaviorSelector;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a human agent with genetics, culture, and resource management.
 * Humans are the primary agents in the simulation with complex behaviors.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class Human extends Agent {
    private final String genetics;
    private final String cultureId;
    private final Map<String, Double> inventory = new HashMap<>();
    private final BehaviorSelector behaviorSelector;

    private double happiness;

    /**
     * Creates a human at the specified position with genetic and cultural identity.
     *
     * @param x         X coordinate
     * @param y         Y coordinate
     * @param genetics  Genetic identifier (encodes genetic proximity)
     * @param cultureId Cultural group identifier
     */
    public Human(int x, int y, String genetics, String cultureId) {
        super(x, y);
        this.genetics = genetics;
        this.cultureId = cultureId;
        this.happiness = 100.0;
        this.behaviorSelector = new BehaviorSelector();
    }

    @Override
    public void tick(World world) {
        if (!isAlive())
            return;

        incrementAge();
        consumeFood();

        // Check for death
        if (getHealth() <= 0 || getAge() > 960) { // 80 years max
            setAlive(false);
            return;
        }

        // Execute AI behavior decision
        behaviorSelector.executeDecision(this, world);

        // Update happiness based on conditions
        updateHappiness();
    }

    /**
     * Consumes food from inventory to maintain energy.
     */
    private void consumeFood() {
        double foodNeeded = 1.0;
        if (inventory.getOrDefault("Food", 0.0) >= foodNeeded) {
            inventory.put("Food", inventory.get("Food") - foodNeeded);
            setEnergy(Math.min(100.0, getEnergy() + 10.0));
        } else {
            setEnergy(Math.max(0.0, getEnergy() - 10.0));
            if (getEnergy() <= 0) {
                setHealth(getHealth() - 10.0);
            }
        }
    }

    /**
     * Update happiness based on needs satisfaction.
     */
    private void updateHappiness() {
        double foodLevel = Math.min(1.0, getInventoryAmount("Food") / 20.0);
        double energyLevel = getEnergy() / 100.0;
        double healthLevel = getHealth() / 100.0;

        // Weighted average
        happiness = (foodLevel * 0.4 + energyLevel * 0.3 + healthLevel * 0.3) * 100.0;
    }

    // Getters

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

    /**
     * Adds an item to the human's inventory.
     *
     * @param item   Item name
     * @param amount Amount to add
     */
    public void addToInventory(String item, double amount) {
        inventory.merge(item, amount, Double::sum);
    }

    /**
     * Gets the amount of an item in inventory.
     *
     * @param item Item name
     * @return Amount in inventory
     */
    public double getInventoryAmount(String item) {
        return inventory.getOrDefault(item, 0.0);
    }
}

