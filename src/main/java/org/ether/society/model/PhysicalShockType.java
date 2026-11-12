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
 * @version 1.0.0-beta.2
 */
public enum PhysicalShockType {

    VOLCANO(true, false),
    NUCLEAR_STRIKE(false, false),
    NUCLEAR_WINTER(false, false),
    METEOR(false, false),
    GEOENGINEERING(false, false),
    EARTHQUAKE(true, false),
    TSUNAMI(false, true),
    PANDEMIC(true, false),
    FLOOD(true, false),
    DROUGHT(true, false),
    FAMINE(true, false),
    SOLAR_STORM(false, false),
    WAR(true, false),
    CLIMATE_ANOMALY(false, false),
    ECONOMIC_CRASH(true, false),
    CYBER_ATTACK(true, false),
    TECH_SINGULARITY(true, false),
    RENAISSANCE_BOOM(true, false),
    BIODIVERSITY_COLLAPSE(true, false),
    UNKNOWN(false, false);

    private final boolean terrestrialOnly;
    private final boolean oceanicOnly;

    PhysicalShockType(boolean terrestrialOnly, boolean oceanicOnly) {
        this.terrestrialOnly = terrestrialOnly;
        this.oceanicOnly = oceanicOnly;
    }

    public boolean isTerrestrialOnly() {
        return terrestrialOnly;
    }

    public boolean isOceanicOnly() {
        return oceanicOnly;
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
        try {
            return PhysicalShockType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
