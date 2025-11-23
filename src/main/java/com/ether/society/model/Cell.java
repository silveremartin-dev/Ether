package com.ether.society.model;

import java.util.HashMap;
import java.util.Map;

public class Cell {
    private final int x;
    private final int y;
    
    private double elevation;
    private double temperature;
    private double rainfall;
    private Biome biome;
    
    private final Map<String, Double> resources = new HashMap<>();
    
    // Population tracking
    private int humanCount = 0;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public double getElevation() { return elevation; }
    public void setElevation(double elevation) { this.elevation = elevation; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getRainfall() { return rainfall; }
    public void setRainfall(double rainfall) { this.rainfall = rainfall; }

    public Biome getBiome() { return biome; }
    public void setBiome(Biome biome) { this.biome = biome; }
    
    public void addResource(String name, double amount) {
        resources.merge(name, amount, Double::sum);
    }
    
    public double getResource(String name) {
        return resources.getOrDefault(name, 0.0);
    }
    
    public void consumeResource(String name, double amount) {
        double current = getResource(name);
        resources.put(name, Math.max(0, current - amount));
    }

    public int getHumanCount() { return humanCount; }
    public void setHumanCount(int humanCount) { this.humanCount = humanCount; }
}
