/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Represents a natural resource in the simulation.
 * Resources can be renewable (regenerate over time) or non-renewable (finite).
 */
public enum Resource {
    // Renewable Resources
    WOOD("Wood", ResourceCategory.RENEWABLE, 0.02), // ~50 years to full regen
    FISH("Fish", ResourceCategory.RENEWABLE, 0.1), // Fast regeneration
    WILD_FAUNA("Wild Fauna", ResourceCategory.RENEWABLE, 0.05), // Wild animals for foraging
    FRESHWATER("Fresh Water", ResourceCategory.RENEWABLE, 1.0), // Daily replenishment
    CROPS("Crops", ResourceCategory.RENEWABLE, 0.25), // Seasonal
    HYDROGEN("Hydrogen", ResourceCategory.RENEWABLE, 0.5), // Electrolysis & atmospheric

    // Non-Renewable Resources
    IRON_ORE("Iron Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    COPPER_ORE("Copper Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    TIN_ORE("Tin Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    GOLD("Gold", ResourceCategory.NON_RENEWABLE, 0.0),
    COAL("Coal", ResourceCategory.NON_RENEWABLE, 0.0),
    OIL("Oil", ResourceCategory.NON_RENEWABLE, 0.0),
    CRUDE_OIL("Crude Oil", ResourceCategory.NON_RENEWABLE, 0.0),
    NATURAL_GAS("Natural Gas", ResourceCategory.NON_RENEWABLE, 0.0),
    URANIUM_ORE("Uranium Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    THORIUM_ORE("Thorium Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    HELIUM_3("Helium-3", ResourceCategory.NON_RENEWABLE, 0.0),
    RARE_EARTH_ORE("Rare Earth Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    LITHIUM_ORE("Lithium Ore / Brine", ResourceCategory.NON_RENEWABLE, 0.0),
    BAUXITE_ORE("Bauxite Ore", ResourceCategory.NON_RENEWABLE, 0.0),
    CLAY("Clay", ResourceCategory.NON_RENEWABLE, 0.0),
    STONE("Stone", ResourceCategory.NON_RENEWABLE, 0.0),
    SALT("Salt", ResourceCategory.NON_RENEWABLE, 0.0);

    private final String displayName;
    private final ResourceCategory category;
    private final double regenerationRate; // Per tick (0-1 scale, 1 = instant)

    Resource(String displayName, ResourceCategory category, double regenerationRate) {
        this.displayName = displayName;
        this.category = category;
        this.regenerationRate = regenerationRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public double getRegenerationRate() {
        return regenerationRate;
    }

    public boolean isRenewable() {
        return category == ResourceCategory.RENEWABLE;
    }

    /**
     * Resource category classification.
     */
    public enum ResourceCategory {
        RENEWABLE,
        NON_RENEWABLE
    }
}
