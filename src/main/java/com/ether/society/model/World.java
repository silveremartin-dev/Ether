/*
 * MIT License
 *
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
package com.ether.society.model;

import com.ether.society.config.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the simulation world grid containing cells and agents.
 * Manages the spatial layout and provides access to cells and agents.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class World {
    private final int width;
    private final int height;
    private Cell[][] grid;
    private final List<Agent> agents = new CopyOnWriteArrayList<>();

    private double globalTemperatureOffset = 0.0;
    private double globalHarvestModifier = 1.0;

    /**
     * Creates a world with the specified dimensions.
     *
     * @param width  World width in cells
     * @param height World height in cells
     */
    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
    }

    /**
     * Sets the entire grid (used by terrain generator).
     *
     * @param grid 2D array of cells
     */
    public void setGrid(Cell[][] grid) {
        this.grid = grid;
    }

    /**
     * Gets a cell at the specified coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return Cell at position, or null if out of bounds
     */
    public Cell getCell(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return null;
    }

    /**
     * Adds an agent to the world.
     *
     * @param agent Agent to add
     */
    public void addAgent(Agent agent) {
        agents.add(agent);
    }

    /**
     * Removes an agent from the world.
     *
     * @param agent Agent to remove
     */
    public void removeAgent(Agent agent) {
        agents.remove(agent);
    }

    /**
     * Gets all agents in the world.
     *
     * @return Thread-safe list of agents
     */
    public List<Agent> getAgents() {
        return agents;
    }

    // Getters and setters

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getGlobalTemperatureOffset() {
        return globalTemperatureOffset;
    }

    public void setGlobalTemperatureOffset(double globalTemperatureOffset) {
        this.globalTemperatureOffset = globalTemperatureOffset;
    }

    public double getGlobalHarvestModifier() {
        return globalHarvestModifier;
    }

    public void setGlobalHarvestModifier(double globalHarvestModifier) {
        this.globalHarvestModifier = globalHarvestModifier;
    }

    /**
     * Resets global modifiers to default values.
     */
    public void resetGlobalModifiers() {
        this.globalTemperatureOffset = 0.0;
        this.globalHarvestModifier = 1.0;
    }
}
