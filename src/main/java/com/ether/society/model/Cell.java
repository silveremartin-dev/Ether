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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single cell in the world grid.
 * Contains terrain properties, climate data, and resource availability.
 * 
 * <p>
 * Note: This class is designed for GPU compatibility and may be refactored
 * to use flat arrays instead of HashMap for better GPU performance.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class Cell {
    private final int x;
    private final int y;

    private double elevation;
    private double temperature;
    private double rainfall;
    private Biome biome;

    private final Map<String, Double> resources = new HashMap<>();

    private int humanCount = 0;

    /**
     * Creates a cell at the specified coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getters and setters

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getRainfall() {
        return rainfall;
    }

    public void setRainfall(double rainfall) {
        this.rainfall = rainfall;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    /**
     * Adds resources to this cell.
     *
     * @param name   Resource name
     * @param amount Amount to add
     */
    public void addResource(String name, double amount) {
        resources.merge(name, amount, Double::sum);
    }

    /**
     * Gets the amount of a specific resource.
     *
     * @param name Resource name
     * @return Amount available
     */
    public double getResource(String name) {
        return resources.getOrDefault(name, 0.0);
    }

    /**
     * Consumes resources from this cell.
     *
     * @param name   Resource name
     * @param amount Amount to consume
     */
    public void consumeResource(String name, double amount) {
        double current = getResource(name);
        resources.put(name, Math.max(0, current - amount));
    }

    public int getHumanCount() {
        return humanCount;
    }

    public void setHumanCount(int humanCount) {
        this.humanCount = humanCount;
    }
}
