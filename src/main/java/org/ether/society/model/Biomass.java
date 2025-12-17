/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Tracks biomass (living matter) within a simulation cell.
 * All values in kilograms.
 * 
 * Used for:
 * - Population tracking (human mass correlates with population)
 * - Food chain modeling
 * - Ecological carrying capacity
 */
public record Biomass(
        double humanMass, // Total human biomass (kg) - avg 70kg per person
        double livestockMass, // Domesticated animals (cattle, sheep, pigs)
        double fishMass, // Fish stock in coastal/river cells
        double cropMass, // Agricultural yield (grains, vegetables)
        double wildFoodMass // Wild plants, game, fruits
) {
    /**
     * Creates an empty biomass record.
     */
    public static Biomass empty() {
        return new Biomass(0, 0, 0, 0, 0);
    }

    /**
     * Estimate human population from biomass.
     * Assumes average human weight of 70kg.
     */
    public long estimatedHumanPopulation() {
        return Math.round(humanMass / 70.0);
    }

    /**
     * Total food mass available (all non-human biomass).
     */
    public double totalFoodMass() {
        return livestockMass + fishMass + cropMass + wildFoodMass;
    }

    /**
     * Total biomass in the cell.
     */
    public double totalMass() {
        return humanMass + livestockMass + fishMass + cropMass + wildFoodMass;
    }

    /**
     * Add biomass from another record.
     */
    public Biomass add(Biomass other) {
        return new Biomass(
                this.humanMass + other.humanMass,
                this.livestockMass + other.livestockMass,
                this.fishMass + other.fishMass,
                this.cropMass + other.cropMass,
                this.wildFoodMass + other.wildFoodMass);
    }

    /**
     * Scale all biomass by a factor.
     */
    public Biomass scale(double factor) {
        return new Biomass(
                this.humanMass * factor,
                this.livestockMass * factor,
                this.fishMass * factor,
                this.cropMass * factor,
                this.wildFoodMass * factor);
    }

    /**
     * Create biomass with updated human mass.
     */
    public Biomass withHumanMass(double newHumanMass) {
        return new Biomass(newHumanMass, livestockMass, fishMass, cropMass, wildFoodMass);
    }

    /**
     * Create biomass with updated crop mass.
     */
    public Biomass withCropMass(double newCropMass) {
        return new Biomass(humanMass, livestockMass, fishMass, newCropMass, wildFoodMass);
    }
}
