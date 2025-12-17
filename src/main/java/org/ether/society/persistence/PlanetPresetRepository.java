/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.persistence;

import org.ether.society.procedural.PlanetPreset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for managing Planet Configurations (Presets).
 * Persists PlanetPreset objects which wrap PlanetConfig logic.
 */
public class PlanetPresetRepository extends JsonRepository<PlanetPreset> {

    public PlanetPresetRepository() {
        super("planet_presets.json", PlanetPreset.class);
    }

    /**
     * seed defaults if empty.
     */
    public void ensureDefaults() {
        if (findAll().isEmpty()) {
            saveAll(List.of(
                    PlanetPreset.EARTH_LIKE,
                    PlanetPreset.DESERT_WORLD,
                    PlanetPreset.ARCHIPELAGO,
                    PlanetPreset.ICE_WORLD));
        }
    }
}
