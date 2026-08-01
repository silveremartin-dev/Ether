/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

/**
 * Display mode for H3 map visualization content.
 * Controls what data is visualized on the map cells.
 */
public enum DisplayMode {
    /** Show terrain/biome colors */
    BIOME("Biome"),

    /** Show population density as heat map */
    POPULATION("Population"),

    /** Show food resource density */
    FOOD("Food"),

    /** Show temperature as gradient */
    TEMPERATURE("Temperature"),

    /** Show technology level */
    TECHNOLOGY("Technology"),

    /** Show water resources */
    WATER("Water"),

    /** Show wood resources */
    WOOD("Wood"),

    /** Show Gini inequality index */
    INEQUALITY("Inequality"),

    /** Show carrying capacity */
    CAPACITY("Capacity"),

    /** Show migration pressure */
    MIGRATION("Migration"),

    /** Show resource flux (trade routes) */
    FLUX("Flux (Trade)"),

    /** Show cultural identity vector */
    CULTURE("Culture"),

    /** Show political borders */
    POLITICAL("Political"),

    /** Show Asabiyyah social cohesion & political instability index */
    ASABIYYAH("Asabiyyah & Instability"),

    /** Show age pyramid demographic distribution (Elderly ratio) */
    AGE_PYRAMID("Age Pyramid (Seniors)"),

    /** Show dynamic surface albedo */
    ALBEDO("Surface Albedo"),

    /** Show active epidemiological outbreaks */
    EPIDEMIC("Epidemic Outbreaks"),

    /** Show terrain movement friction matrix */
    FRICTION("Movement Friction");

    private final String displayName;

    DisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
