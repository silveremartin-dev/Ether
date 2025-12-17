/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Population distribution patterns for initial simulation setup.
 * 
 * Controls how the initial human population is distributed across the world.
 */
public enum PopulationDistribution {
    /**
     * All population starts on a single continent (Africa-like origin).
     */
    ONE_CONTINENT("Single Origin",
            "Population concentrated on one continent, simulating Out of Africa migration"),

    /**
     * Population clustered along major river systems.
     */
    RIVER_VALLEYS("River Valleys",
            "Settlement along rivers with high water resources (Nile, Tigris, Indus pattern)"),

    /**
     * Population distributed along coastlines.
     */
    COASTAL("Coastal Settlement",
            "Population concentrated near ocean/beach biomes for fishing"),

    /**
     * Low-density scattered population across all habitable land.
     */
    SPARSE("Sparse Nomadic",
            "Low-density hunter-gatherer distribution across all habitable terrain"),

    /**
     * High-density concentrated population in optimal areas.
     */
    DENSE("Dense Settlement",
            "Higher population density in the most fertile/temperate regions"),

    /**
     * Random distribution proportional to biome habitability.
     */
    RANDOM("Random",
            "Randomized distribution weighted by biome carrying capacity"),

    /**
     * Population evenly spread across all habitable cells.
     */
    UNIFORM("Uniform",
            "Equal population in all habitable cells"),

    /**
     * No initial population (for custom scenarios).
     */
    EMPTY("Empty World",
            "No initial population - add manually or via events");

    private final String displayName;
    private final String description;

    PopulationDistribution(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
