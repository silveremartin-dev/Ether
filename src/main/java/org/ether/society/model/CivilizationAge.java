/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Represents the broad eras of civilization development.
 * Each age unlocks new potentials and multipliers for society.
 */
public enum CivilizationAge {
    STONE_AGE("Stone Age", 0.0, 1.0, 1.0, 1.0),
    BRONZE_AGE("Bronze Age", 2.0, 1.2, 1.05, 1.1),
    IRON_AGE("Iron Age", 5.0, 1.5, 1.1, 1.2),
    MEDIEVAL("Medieval Age", 10.0, 2.0, 1.15, 1.3),
    RENAISSANCE("Renaissance", 15.0, 2.5, 1.2, 1.5),
    INDUSTRIAL("Industrial Age", 25.0, 5.0, 1.5, 3.0),
    MODERN("Modern Age", 40.0, 10.0, 1.1, 4.0), // Lower growth multiplier due to demographic transition
    FUTURE("Future Age", 60.0, 20.0, 1.0, 2.0); // Stable population, high efficiency

    private final String displayName;
    private final double minTechLevel;
    private final double capacityMultiplier;
    private final double growthRateMultiplier;
    private final double resourceConsumptionMultiplier;

    CivilizationAge(String displayName, double minTechLevel, double capacityMultiplier, double growthRateMultiplier,
            double resourceConsumptionMultiplier) {
        this.displayName = displayName;
        this.minTechLevel = minTechLevel;
        this.capacityMultiplier = capacityMultiplier;
        this.growthRateMultiplier = growthRateMultiplier;
        this.resourceConsumptionMultiplier = resourceConsumptionMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CivilizationAge fromTechLevel(double level) {
        CivilizationAge best = STONE_AGE;
        for (CivilizationAge age : values()) {
            if (level >= age.minTechLevel) {
                best = age;
            }
        }
        return best;
    }

    public double getMinTechLevel() {
        return minTechLevel;
    }

    public double getCapacityMultiplier() {
        return capacityMultiplier;
    }

    public double getGrowthRateMultiplier() {
        return growthRateMultiplier;
    }

    public double getResourceConsumptionMultiplier() {
        return resourceConsumptionMultiplier;
    }
}
