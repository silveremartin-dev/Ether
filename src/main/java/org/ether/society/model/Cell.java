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

    // New Tracking Maps
    private final Map<String, Double> biomass = new HashMap<>();
    private final Map<String, Double> energy = new HashMap<>();
    // Note: 'resources' map already exists but will be used for specific resources

    // Socio-economic indicators at cellular level
    private double cellLifespan = 40.0;
    private double cellFertility = 6.0;
    private double localPopulation = 0.0; // Floating point for fractional growth tracking

    /**
     * Creates a cell at the specified coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     */
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        initializeTracking();
    }

    private void initializeTracking() {
        // Initialize default biomass types
        biomass.put("HUMAN", 0.0);
        biomass.put("LIVESTOCK", 0.0);
        biomass.put("FISH", 0.0); // Will be populated based on water/biome
        biomass.put("AGRICULTURE", 0.0);
        biomass.put("NATURAL", 100.0); // Baseline natural food source

        // Initialize energy types
        energy.put("WIND", 0.0);
        energy.put("SOLAR", 0.0);
        energy.put("FIRE", 0.0);
        energy.put("SLAVES", 0.0);
        energy.put("FOOD_CONSUMED", 0.0);
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

    public void setResource(String name, double amount) {
        resources.put(name, amount);
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
        this.biomass.put("HUMAN", (double) humanCount * 50.0); // approx 50kg per human avg
        this.localPopulation = humanCount;
    }

    public double getBiomass(String type) {
        return biomass.getOrDefault(type, 0.0);
    }

    public void setBiomass(String type, double amount) {
        biomass.put(type, amount);
    }

    public void addBiomass(String type, double amount) {
        biomass.merge(type, amount, Double::sum);
    }

    public double getEnergy(String type) {
        return energy.getOrDefault(type, 0.0);
    }

    public void setEnergy(String type, double amount) {
        energy.put(type, amount);
    }

    public double getCellLifespan() {
        return cellLifespan;
    }

    public void setCellLifespan(double cellLifespan) {
        this.cellLifespan = cellLifespan;
    }

    public double getCellFertility() {
        return cellFertility;
    }

    public void setCellFertility(double cellFertility) {
        this.cellFertility = cellFertility;
    }

    public double getLocalPopulation() {
        return localPopulation;
    }

    public void setLocalPopulation(double pop) {
        this.localPopulation = pop;
        this.humanCount = (int) pop;
        this.biomass.put("HUMAN", pop * 50.0);
    }
}
