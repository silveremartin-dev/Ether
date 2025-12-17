/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks resource stockpiles and ecological properties for a simulation cell.
 * This is the primary container for all resource-related data within a cell.
 */
public class CellResources {

    /** Resource stockpiles (current amounts) */
    private final Map<Resource, Double> stockpiles;

    /** Maximum capacity for each resource (based on biome, geography) */
    private final Map<Resource, Double> maxCapacity;

    /** Fresh water access (0-1 scale: 0=desert, 1=riverside) */
    private double freshWaterAccess;

    /** Carrying capacity for human population (pre-technology) */
    private double baseCarryingCapacity;

    /** Current biomass in this cell */
    private Biomass biomass;

    /** Current energy source available to inhabitants */
    private EnergySource energySource;

    public CellResources() {
        this.stockpiles = new EnumMap<>(Resource.class);
        this.maxCapacity = new EnumMap<>(Resource.class);
        this.freshWaterAccess = 0.0;
        this.baseCarryingCapacity = 0.0;
        this.biomass = Biomass.empty();
        this.energySource = EnergySource.FIRE;

        // Initialize all resources to zero
        for (Resource r : Resource.values()) {
            stockpiles.put(r, 0.0);
            maxCapacity.put(r, 0.0);
        }
    }

    /**
     * Get current stock of a resource.
     */
    public double getStock(Resource resource) {
        return stockpiles.getOrDefault(resource, 0.0);
    }

    /**
     * Set stock of a resource.
     */
    public void setStock(Resource resource, double amount) {
        stockpiles.put(resource, Math.max(0, amount));
    }

    /**
     * Add to stock (e.g., regeneration).
     */
    public void addStock(Resource resource, double amount) {
        double current = getStock(resource);
        double max = getMaxCapacity(resource);
        stockpiles.put(resource, Math.min(max, current + amount));
    }

    /**
     * Consume from stock (e.g., harvesting).
     * 
     * @return Amount actually consumed (may be less than requested).
     */
    public double consume(Resource resource, double amount) {
        double current = getStock(resource);
        double consumed = Math.min(current, amount);
        stockpiles.put(resource, current - consumed);
        return consumed;
    }

    /**
     * Get maximum capacity for a resource.
     */
    public double getMaxCapacity(Resource resource) {
        return maxCapacity.getOrDefault(resource, 0.0);
    }

    /**
     * Set maximum capacity for a resource.
     */
    public void setMaxCapacity(Resource resource, double capacity) {
        maxCapacity.put(resource, Math.max(0, capacity));
    }

    /**
     * Regenerate all renewable resources based on their rates.
     */
    public void regenerateResources() {
        for (Resource r : Resource.values()) {
            if (r.isRenewable()) {
                double current = getStock(r);
                double max = getMaxCapacity(r);
                double regen = r.getRegenerationRate() * max;
                stockpiles.put(r, Math.min(max, current + regen));
            }
        }
    }

    /**
     * Calculate effective carrying capacity (base * energy boost * water factor).
     */
    public double getEffectiveCarryingCapacity() {
        double waterFactor = 0.5 + (freshWaterAccess * 0.5); // 0.5 min, 1.0 max
        return baseCarryingCapacity * energySource.getCarryingCapacityBoost() * waterFactor;
    }

    /**
     * Check if cell has sufficient resources to sustain population.
     */
    public boolean canSustainPopulation(long population) {
        double foodNeeded = population * 2.5; // ~2.5 kg food per person per day per tick
        return biomass.totalFoodMass() >= foodNeeded;
    }

    // Getters and Setters

    public double getFreshWaterAccess() {
        return freshWaterAccess;
    }

    public void setFreshWaterAccess(double freshWaterAccess) {
        this.freshWaterAccess = Math.max(0, Math.min(1, freshWaterAccess));
    }

    public double getBaseCarryingCapacity() {
        return baseCarryingCapacity;
    }

    public void setBaseCarryingCapacity(double baseCarryingCapacity) {
        this.baseCarryingCapacity = Math.max(0, baseCarryingCapacity);
    }

    public Biomass getBiomass() {
        return biomass;
    }

    public void setBiomass(Biomass biomass) {
        this.biomass = biomass != null ? biomass : Biomass.empty();
    }

    public EnergySource getEnergySource() {
        return energySource;
    }

    public void setEnergySource(EnergySource energySource) {
        this.energySource = energySource != null ? energySource : EnergySource.FIRE;
    }

    public Map<Resource, Double> getAllStockpiles() {
        return new EnumMap<>(stockpiles);
    }

    /**
     * Total value of all resources (simple sum, no weighting).
     */
    public double getTotalResourceValue() {
        return stockpiles.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
