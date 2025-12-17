/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Technology tree managing civilization progression.
 * 
 * Tech levels unlock:
 * - New energy sources
 * - Better resource extraction
 * - Higher carrying capacity
 * - New abilities (irrigation, mining, etc.)
 */
public class TechnologyTree {

    /**
     * Technology categories that can be researched.
     */
    public enum TechCategory {
        AGRICULTURE("Agriculture", "Farming and food production"),
        METALLURGY("Metallurgy", "Metal extraction and tools"),
        ENERGY("Energy", "Power sources and machines"),
        CONSTRUCTION("Construction", "Buildings and infrastructure"),
        MEDICINE("Medicine", "Health and lifespan"),
        NAVIGATION("Navigation", "Exploration and trade");

        private final String name;
        private final String description;

        TechCategory(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Specific technologies that can be unlocked.
     */
    public enum Technology {
        // Agriculture tree (levels 0-5)
        FORAGING(TechCategory.AGRICULTURE, 0, "Foraging", "Gathering wild food", 1.0),
        BASIC_FARMING(TechCategory.AGRICULTURE, 1, "Basic Farming", "Simple crop cultivation", 1.5),
        IRRIGATION(TechCategory.AGRICULTURE, 2, "Irrigation", "Water management for crops", 2.0),
        CROP_ROTATION(TechCategory.AGRICULTURE, 3, "Crop Rotation", "Improved soil management", 2.5),
        SELECTIVE_BREEDING(TechCategory.AGRICULTURE, 4, "Selective Breeding", "Better crop yields", 3.0),
        MECHANIZED_FARMING(TechCategory.AGRICULTURE, 5, "Mechanized Farming", "Tractors and machinery", 5.0),

        // Metallurgy tree (levels 1-6)
        STONE_TOOLS(TechCategory.METALLURGY, 0, "Stone Tools", "Basic stone implements", 1.0),
        COPPER_WORKING(TechCategory.METALLURGY, 1, "Copper Working", "Soft metal tools", 1.3),
        BRONZE_WORKING(TechCategory.METALLURGY, 2, "Bronze Working", "Alloy weapons and tools", 1.6),
        IRON_WORKING(TechCategory.METALLURGY, 3, "Iron Working", "Strong iron implements", 2.0),
        STEEL_PRODUCTION(TechCategory.METALLURGY, 4, "Steel Production", "High-quality metal", 3.0),
        ADVANCED_ALLOYS(TechCategory.METALLURGY, 5, "Advanced Alloys", "Specialized metals", 4.0),

        // Energy tree (levels 0-8)
        FIRE_MASTERY(TechCategory.ENERGY, 0, "Fire Mastery", "Controlled fire use", 1.2),
        DRAFT_ANIMALS(TechCategory.ENERGY, 1, "Draft Animals", "Animal labor power", 1.5),
        WATER_POWER(TechCategory.ENERGY, 3, "Water Power", "Mills and water wheels", 2.0),
        WIND_POWER(TechCategory.ENERGY, 4, "Wind Power", "Windmills and sailing", 2.2),
        STEAM_POWER(TechCategory.ENERGY, 5, "Steam Power", "Industrial machinery", 3.0),
        ELECTRICITY(TechCategory.ENERGY, 6, "Electricity", "Electric power grid", 4.0),
        SOLAR_POWER(TechCategory.ENERGY, 7, "Solar Power", "Renewable energy", 5.0),
        NUCLEAR_POWER(TechCategory.ENERGY, 8, "Nuclear Power", "Atomic energy", 6.0),

        // Construction tree (levels 0-5)
        SHELTERS(TechCategory.CONSTRUCTION, 0, "Shelters", "Basic dwellings", 1.1),
        STONE_BUILDINGS(TechCategory.CONSTRUCTION, 2, "Stone Buildings", "Permanent structures", 1.5),
        FORTIFICATIONS(TechCategory.CONSTRUCTION, 3, "Fortifications", "Defensive walls", 1.8),
        ARCHITECTURE(TechCategory.CONSTRUCTION, 4, "Architecture", "Advanced buildings", 2.2),
        SKYSCRAPERS(TechCategory.CONSTRUCTION, 6, "Skyscrapers", "High-density housing", 4.0),

        // Medicine tree (levels 0-6)
        HERBAL_MEDICINE(TechCategory.MEDICINE, 0, "Herbal Medicine", "Basic treatments", 1.1),
        SURGERY(TechCategory.MEDICINE, 2, "Surgery", "Basic operations", 1.3),
        SANITATION(TechCategory.MEDICINE, 3, "Sanitation", "Clean water and sewage", 1.8),
        GERM_THEORY(TechCategory.MEDICINE, 4, "Germ Theory", "Understanding disease", 2.5),
        ANTIBIOTICS(TechCategory.MEDICINE, 5, "Antibiotics", "Modern medicine", 3.5),
        GENETIC_MEDICINE(TechCategory.MEDICINE, 7, "Genetic Medicine", "Gene therapy", 5.0),

        // Navigation tree (levels 1-5)
        RAFTS(TechCategory.NAVIGATION, 1, "Rafts", "Basic water travel", 1.1),
        SAILING(TechCategory.NAVIGATION, 2, "Sailing", "Wind-powered ships", 1.5),
        COMPASS(TechCategory.NAVIGATION, 3, "Compass", "Improved navigation", 1.8),
        OCEAN_VESSELS(TechCategory.NAVIGATION, 4, "Ocean Vessels", "Long-distance travel", 2.2),
        STEAM_SHIPS(TechCategory.NAVIGATION, 5, "Steam Ships", "Powered vessels", 3.0);

        private final TechCategory category;
        private final int requiredLevel;
        private final String name;
        private final String description;
        private final double multiplier;

        Technology(TechCategory category, int requiredLevel, String name, String description, double multiplier) {
            this.category = category;
            this.requiredLevel = requiredLevel;
            this.name = name;
            this.description = description;
            this.multiplier = multiplier;
        }

        public TechCategory getCategory() {
            return category;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    // Instance state
    private double overallTechLevel = 0.0;
    private CivilizationAge currentAge = CivilizationAge.STONE_AGE;
    private final Set<Technology> unlockedTechs = EnumSet.noneOf(Technology.class);

    public TechnologyTree() {
        // Start with basic technologies
        unlockedTechs.add(Technology.FORAGING);
        unlockedTechs.add(Technology.STONE_TOOLS);
        unlockedTechs.add(Technology.FIRE_MASTERY);
        unlockedTechs.add(Technology.SHELTERS);
        unlockedTechs.add(Technology.HERBAL_MEDICINE);
    }

    /**
     * Get current overall tech level (0-10 scale).
     */
    public double getOverallLevel() {
        return overallTechLevel;
    }

    /**
     * Set tech level and unlock appropriate technologies.
     */
    public void setOverallLevel(double level) {
        this.overallTechLevel = Math.max(0, Math.min(100, level)); // Increased max level for future
        updateUnlockedTechs();
        updateAge();
    }

    private void updateAge() {
        CivilizationAge newAge = CivilizationAge.STONE_AGE;
        for (CivilizationAge age : CivilizationAge.values()) {
            if (this.overallTechLevel >= age.getMinTechLevel()) {
                newAge = age;
            }
        }
        this.currentAge = newAge;
    }

    public CivilizationAge getCurrentAge() {
        return currentAge;
    }

    /**
     * Increment tech level by a small amount.
     */
    public void addProgress(double increment) {
        setOverallLevel(overallTechLevel + increment);
    }

    /**
     * Update unlocked technologies based on current level.
     */
    private void updateUnlockedTechs() {
        for (Technology tech : Technology.values()) {
            if (tech.getRequiredLevel() <= overallTechLevel) {
                unlockedTechs.add(tech);
            }
        }
    }

    /**
     * Check if a specific technology is unlocked.
     */
    public boolean isUnlocked(Technology tech) {
        return unlockedTechs.contains(tech);
    }

    /**
     * Get all unlocked technologies.
     */
    public Set<Technology> getUnlockedTechs() {
        return EnumSet.copyOf(unlockedTechs);
    }

    /**
     * Get the best unlocked energy source.
     */
    public EnergySource getBestEnergySource() {
        EnergySource best = EnergySource.FIRE;
        for (EnergySource source : EnergySource.values()) {
            if (source.getTechLevel() <= overallTechLevel && source.ordinal() > best.ordinal()) {
                best = source;
            }
        }
        return best;
    }

    /**
     * Calculate total carrying capacity multiplier from all unlocked techs.
     */
    public double getCarryingCapacityMultiplier() {
        double mult = 1.0;

        // Sum agriculture multipliers
        for (Technology tech : unlockedTechs) {
            if (tech.getCategory() == TechCategory.AGRICULTURE) {
                mult = Math.max(mult, tech.getMultiplier());
            }
        }

        // Add energy source boost
        mult *= getBestEnergySource().getCarryingCapacityBoost();

        // Add Age Multiplier
        mult *= currentAge.getCapacityMultiplier();

        return mult;
    }

    /**
     * Calculate lifespan bonus from medicine techs.
     */
    public double getLifespanBonus() {
        double bonus = 0;
        for (Technology tech : unlockedTechs) {
            if (tech.getCategory() == TechCategory.MEDICINE) {
                bonus += (tech.getMultiplier() - 1.0) * 10; // Convert to years
            }
        }
        return bonus;
    }

    /**
     * Check if irrigation is unlocked (affects desert reclamation).
     */
    public boolean hasIrrigation() {
        return isUnlocked(Technology.IRRIGATION);
    }

    /**
     * Check if ocean navigation is possible.
     */
    public boolean canCrossOceans() {
        return isUnlocked(Technology.OCEAN_VESSELS);
    }
}
