/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.generation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying {@link ProceduralGenerator} planetary synthesis.
 */
class ProceduralGeneratorTest {

    private ProceduralGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ProceduralGenerator();
    }

    @Test
    @DisplayName("getPlanetPoint returns valid terrain point for Earth-like preset")
    void testGetPlanetPointEarthLike() {
        ProceduralGenerator.PlanetPoint point = generator.getPlanetPoint(48.85, 2.35, PlanetPreset.EARTH_LIKE);

        assertNotNull(point);
        assertNotNull(point.biome());
        assertTrue(point.elevation() >= -1.0 && point.elevation() <= 1.0);
        assertTrue(point.rainfall() >= 0.0 && point.rainfall() <= 1.0);
    }

    @Test
    @DisplayName("Polar latitudes have colder temperatures than equator")
    void testTemperatureGradient() {
        ProceduralGenerator.PlanetPoint equator = generator.getPlanetPoint(0.0, 0.0, PlanetPreset.EARTH_LIKE);
        ProceduralGenerator.PlanetPoint arctic = generator.getPlanetPoint(85.0, 0.0, PlanetPreset.EARTH_LIKE);

        assertTrue(equator.temperature() > arctic.temperature(), "Equator should be warmer than Arctic pole");
    }
}
