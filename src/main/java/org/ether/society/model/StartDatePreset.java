/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Predefined start dates for historical simulation scenarios.
 * Each preset represents a significant era in human history.
 */
public enum StartDatePreset {
    OUT_OF_AFRICA(-100000, "Out of Africa",
            "Early human migration from Africa. Population ~10,000. Hunter-gatherer societies."),

    UPPER_PALEOLITHIC(-40000, "Upper Paleolithic",
            "Art, religion, and complex tools emerge. Homo sapiens dominant."),

    MESOLITHIC(-15000, "Mesolithic",
            "Post-glacial period. Domestication of dogs. Semi-sedentary communities."),

    NEOLITHIZATION(-10000, "Neolithic Revolution",
            "Agriculture begins. First permanent settlements. Population ~5 million."),

    CHALCOLITHIC(-5000, "Copper Age",
            "Early metallurgy. Proto-cities like Çatalhöyük. Trade networks."),

    BRONZE_AGE(-3000, "First Empires",
            "Egypt, Sumer, Indus Valley. Writing invented. Population ~50 million."),

    IRON_AGE(-1200, "Iron Age",
            "Iron tools spread. Greek city-states. Persian Empire."),

    CLASSICAL(-500, "Classical Era",
            "Greek philosophy. Roman Republic. Buddha, Confucius."),

    MEDIEVAL(500, "Medieval Period",
            "Feudalism. Islam expansion. Tang Dynasty. Population ~200 million."),

    EARLY_MODERN(1500, "Age of Exploration",
            "Columbian exchange. Renaissance. Printing press."),

    INDUSTRIAL(1800, "Industrial Revolution",
            "Steam power. Factories. Population ~1 billion."),

    MODERN(1945, "Modern Era",
            "Nuclear age. Information technology. Population ~2.5 billion.");

    private final long year;
    private final String displayName;
    private final String description;

    StartDatePreset(long year, String displayName, String description) {
        this.year = year;
        this.displayName = displayName;
        this.description = description;
    }

    public long getYear() {
        return year;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get estimated world population for this era.
     */
    public long getEstimatedPopulation() {
        return switch (this) {
            case OUT_OF_AFRICA -> 10_000;
            case UPPER_PALEOLITHIC -> 500_000;
            case MESOLITHIC -> 3_000_000;
            case NEOLITHIZATION -> 5_000_000;
            case CHALCOLITHIC -> 15_000_000;
            case BRONZE_AGE -> 50_000_000;
            case IRON_AGE -> 100_000_000;
            case CLASSICAL -> 200_000_000;
            case MEDIEVAL -> 300_000_000;
            case EARLY_MODERN -> 500_000_000;
            case INDUSTRIAL -> 1_000_000_000;
            case MODERN -> 2_500_000_000L;
        };
    }

    /**
     * Get estimated technology level (maps to EnergySource).
     */
    public int getEstimatedTechLevel() {
        return switch (this) {
            case OUT_OF_AFRICA, UPPER_PALEOLITHIC -> 0; // Fire
            case MESOLITHIC, NEOLITHIZATION -> 1; // Animal power
            case CHALCOLITHIC, BRONZE_AGE, IRON_AGE -> 2; // Slave labor
            case CLASSICAL, MEDIEVAL -> 3; // Water mill
            case EARLY_MODERN -> 4; // Wind
            case INDUSTRIAL -> 5; // Coal
            case MODERN -> 6; // Oil
        };
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", displayName, year);
    }
}
