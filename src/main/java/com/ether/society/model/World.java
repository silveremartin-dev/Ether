package com.ether.society.model;

public class World {
    private final int width;
    private final int height;
    private Cell[][] grid;
    private final java.util.List<Agent> agents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private double globalTemperatureOffset = 0.0;
    private double globalHarvestModifier = 1.0;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
    }

    public void addAgent(Agent agent) {
        agents.add(agent);
    }

    public void removeAgent(Agent agent) {
        agents.remove(agent);
    }

    public java.util.List<Agent> getAgents() {
        return agents;
    }

    public void setGrid(Cell[][] grid) {
        this.grid = grid;
    }
    
    public Cell getCell(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return null;
    }

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
    
    public void resetGlobalModifiers() {
        this.globalTemperatureOffset = 0.0;
        this.globalHarvestModifier = 1.0;
    }
}
