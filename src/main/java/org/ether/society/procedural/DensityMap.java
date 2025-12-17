/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores resource density values for H3 cells.
 */
public class DensityMap {
    // H3 index -> (ResourceType -> density 0.0-1.0)
    private final Map<Long, Map<ResourceType, Double>> densities;

    // City locations (H3 index -> city tier 1-3)
    private final Map<Long, Integer> cities;

    public DensityMap() {
        this.densities = new HashMap<>();
        this.cities = new HashMap<>();
    }

    /**
     * Set density for a specific resource at a cell.
     */
    public void setDensity(long h3Index, ResourceType type, double value) {
        densities.computeIfAbsent(h3Index, k -> new HashMap<>())
                .put(type, Math.max(0.0, Math.min(1.0, value)));
    }

    /**
     * Get density for a specific resource at a cell.
     */
    public double getDensity(long h3Index, ResourceType type) {
        return densities.getOrDefault(h3Index, new HashMap<>())
                .getOrDefault(type, 0.0);
    }

    /**
     * Add density (useful for brush painting).
     */
    public void addDensity(long h3Index, ResourceType type, double delta) {
        double current = getDensity(h3Index, type);
        setDensity(h3Index, type, current + delta);
    }

    /**
     * Clear all density for a cell.
     */
    public void clearCell(long h3Index) {
        densities.remove(h3Index);
        cities.remove(h3Index);
    }

    /**
     * Clear all data.
     */
    public void clear() {
        densities.clear();
        cities.clear();
    }

    /**
     * Add a city at location.
     */
    public void addCity(long h3Index, int tier) {
        cities.put(h3Index, tier);
    }

    /**
     * Get city tier at location (0 if no city).
     */
    public int getCityTier(long h3Index) {
        return cities.getOrDefault(h3Index, 0);
    }

    /**
     * Check if cell has a city.
     */
    public boolean hasCity(long h3Index) {
        return cities.containsKey(h3Index);
    }

    /**
     * Get all cities.
     */
    public Map<Long, Integer> getCities() {
        return new HashMap<>(cities);
    }

    /**
     * Get number of cells with density data.
     */
    public int getCellCount() {
        return densities.size();
    }
}

