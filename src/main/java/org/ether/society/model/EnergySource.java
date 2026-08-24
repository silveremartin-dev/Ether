/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Energy sources available to civilizations.
 * Ordered by technological progression (techLevel).
 * 
 * Each source has a multiplier that affects carrying capacity and productivity.
 */
public enum EnergySource {
    FIRE(0, "Fire", 1.0, 1.2), // Basic: cooking, warmth
    ANIMAL_POWER(1, "Draft Animals", 2.0, 1.5), // Oxen, horses for farming
    SLAVE_LABOR(2, "Slave Labor", 3.0, 1.8), // Human labor exploitation
    WATER_MILL(3, "Water Mill", 4.0, 2.0), // Mechanical power from rivers
    WIND_POWER(4, "Wind Power", 5.0, 2.2), // Windmills, sailing
    COAL(5, "Coal", 10.0, 3.0), // Industrial revolution
    OIL(6, "Oil", 20.0, 4.0), // Modern era
    NATURAL_GAS(6, "Natural Gas", 22.0, 4.2), // Natural gas (CH4)
    SOLAR(7, "Solar", 25.0, 5.0), // Renewable future
    NUCLEAR(8, "Nuclear Fission", 50.0, 6.0), // Uranium/Thorium fission
    FUSION_HE3(9, "Helium-3 Fusion", 150.0, 10.0); // High-tech Aneutronic Fusion

    private final int techLevel;
    private final String displayName;
    private final double energyMultiplier; // How much energy is available
    private final double carryingCapacityBoost; // Multiplier for population capacity

    EnergySource(int techLevel, String displayName, double energyMultiplier, double carryingCapacityBoost) {
        this.techLevel = techLevel;
        this.displayName = displayName;
        this.energyMultiplier = energyMultiplier;
        this.carryingCapacityBoost = carryingCapacityBoost;
    }

    public int getTechLevel() {
        return techLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getEnergyMultiplier() {
        return energyMultiplier;
    }

    public double getCarryingCapacityBoost() {
        return carryingCapacityBoost;
    }

    /**
     * Get energy source by tech level.
     */
    public static EnergySource byTechLevel(int level) {
        for (EnergySource source : values()) {
            if (source.techLevel == level) {
                return source;
            }
        }
        return FIRE; // Default
    }

    /**
     * Check if this source requires a specific resource.
     */
    public Resource getRequiredResource() {
        return switch (this) {
            case FIRE -> Resource.WOOD;
            case COAL -> Resource.COAL;
            case OIL -> Resource.OIL;
            case NATURAL_GAS -> Resource.NATURAL_GAS;
            case NUCLEAR -> Resource.URANIUM_ORE;
            case FUSION_HE3 -> Resource.HELIUM_3;
            default -> null; // No resource required
        };
    }
}
