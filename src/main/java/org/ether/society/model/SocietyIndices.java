/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for global socio-economic indices.
 */
public class SocietyIndices implements Serializable {
    private long totalPopulation;
    private double globalBiomass;
    private double giniCoefficient;
    private double averageLifespan;
    private double fertilityRate;
    private double averageTechnologyLevel;

    // Additional tracking metrics
    private final Map<String, Double> customIndices = new HashMap<>();

    public SocietyIndices() {
        this.totalPopulation = 0;
        this.globalBiomass = 0.0;
        this.giniCoefficient = 0.0;
        this.averageLifespan = 40.0; // Default historic start
        this.fertilityRate = 6.0; // High historic fertility
        this.averageTechnologyLevel = 0.0;
    }

    public long getTotalPopulation() {
        return totalPopulation;
    }

    public void setTotalPopulation(long totalPopulation) {
        this.totalPopulation = totalPopulation;
    }

    public double getGlobalBiomass() {
        return globalBiomass;
    }

    public void setGlobalBiomass(double globalBiomass) {
        this.globalBiomass = globalBiomass;
    }

    public double getGiniCoefficient() {
        return giniCoefficient;
    }

    public void setGiniCoefficient(double giniCoefficient) {
        this.giniCoefficient = giniCoefficient;
    }

    public double getAverageLifespan() {
        return averageLifespan;
    }

    public void setAverageLifespan(double averageLifespan) {
        this.averageLifespan = averageLifespan;
    }

    public double getFertilityRate() {
        return fertilityRate;
    }

    public void setFertilityRate(double fertilityRate) {
        this.fertilityRate = fertilityRate;
    }

    public double getAverageTechnologyLevel() {
        return averageTechnologyLevel;
    }

    public void setAverageTechnologyLevel(double averageTechnologyLevel) {
        this.averageTechnologyLevel = averageTechnologyLevel;
    }

    public void setCustomIndex(String key, double value) {
        customIndices.put(key, value);
    }

    public double getCustomIndex(String key) {
        return customIndices.getOrDefault(key, 0.0);
    }
}
