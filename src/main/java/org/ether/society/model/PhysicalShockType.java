/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.util.Locale;

/**
 * <h1>Physical Shock Type</h1>
 *
 * <p>Strongly-typed enumeration of physical, climatic, astronomical, and socio-ecological shocks
 * applied to planetary mesh cells and atmospheric buffers.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public enum PhysicalShockType {

    VOLCANO(EventCategory.NATURAL),
    EARTHQUAKE(EventCategory.NATURAL),
    TSUNAMI(EventCategory.NATURAL),
    FLOOD(EventCategory.NATURAL),
    DROUGHT(EventCategory.NATURAL),
    HEATWAVE(EventCategory.NATURAL),
    ICE_AGE(EventCategory.NATURAL),
    METEOR(EventCategory.NATURAL),
    SOLAR_STORM(EventCategory.NATURAL),
    SOLAR_EMP(EventCategory.NATURAL),
    CLIMATE_ANOMALY(EventCategory.NATURAL),
    FAMINE(EventCategory.NATURAL),
    PANDEMIC(EventCategory.NATURAL),

    NUCLEAR_STRIKE(EventCategory.ANTHROPIC),
    NUCLEAR_WINTER(EventCategory.ANTHROPIC),
    WAR(EventCategory.ANTHROPIC),
    ECONOMIC_CRASH(EventCategory.ANTHROPIC),
    CYBER_ATTACK(EventCategory.ANTHROPIC),
    GEOENGINEERING(EventCategory.ANTHROPIC),
    BIODIVERSITY_COLLAPSE(EventCategory.ANTHROPIC),
    INDUSTRIAL_ACCIDENT(EventCategory.ANTHROPIC),
    URBAN_FIRE(EventCategory.ANTHROPIC),
    DAM_FAILURE(EventCategory.ANTHROPIC),

    TECH_SINGULARITY(EventCategory.CHRONICLE_MILESTONE),
    RENAISSANCE_BOOM(EventCategory.CHRONICLE_MILESTONE),

    ALIEN_CONTACT(EventCategory.ANTHROPIC),
    DOME_BREACH(EventCategory.ANTHROPIC),
    ECLSS_FAILURE(EventCategory.ANTHROPIC),
    AEROSTAT_PUNCTURE(EventCategory.ANTHROPIC),
    LAVA_TUBE_COLLAPSE(EventCategory.NATURAL),

    UNKNOWN(EventCategory.NATURAL);

    private final EventCategory defaultCategory;

    PhysicalShockType(EventCategory defaultCategory) {
        this.defaultCategory = defaultCategory != null ? defaultCategory : EventCategory.NATURAL;
    }

    public EventCategory getDefaultCategory() {
        return defaultCategory;
    }

    public EventCategory getCategory() {
        return defaultCategory;
    }

    public boolean isNatural() {
        return defaultCategory == EventCategory.NATURAL;
    }

    public boolean isAnthropic() {
        return defaultCategory == EventCategory.ANTHROPIC;
    }

    /**
     * Resolves a raw string event type into a strongly-typed {@link PhysicalShockType}.
     *
     * @param rawType raw event type name
     * @return resolved shock type, or {@link #UNKNOWN}
     */
    public static PhysicalShockType fromString(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return UNKNOWN;
        }
        String clean = rawType.trim().toUpperCase(Locale.ROOT);
        if (clean.startsWith("DISASTER_")) {
            clean = clean.substring("DISASTER_".length());
        } else if (clean.startsWith("MILESTONE_")) {
            clean = clean.substring("MILESTONE_".length());
        }
        try {
            return PhysicalShockType.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    /**
     * Infers the event category (NATURAL vs ANTHROPIC) from a raw type string.
     *
     * @param rawType raw type string identifier
     * @return corresponding EventCategory
     */
    public static EventCategory inferCategory(String rawType) {
        return fromString(rawType).getCategory();
    }
}
