package org.ether.society.persistence;

import org.ether.society.procedural.PlanetPreset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanetPresetRepositoryTest {

    private PlanetPresetRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PlanetPresetRepository();
    }

    @Test
    @DisplayName("Default planet presets can be initialized and read")
    void testEnsureDefaults() {
        repository.ensureDefaults();
        List<PlanetPreset> presets = repository.findAll();

        assertNotNull(presets);
        assertFalse(presets.isEmpty(), "Should contain default planet presets");
    }

    @Test
    @DisplayName("Saving custom preset appends to repository")
    void testSaveCustomPreset() {
        PlanetPreset custom = new PlanetPreset("TestPlanet", 5, 12345L, 0.5, 1.0, 0.4, 30.0);
        int initialSize = repository.findAll().size();

        repository.save(custom);
        List<PlanetPreset> updated = repository.findAll();

        assertEquals(initialSize + 1, updated.size());
        assertTrue(updated.stream().anyMatch(p -> "TestPlanet".equals(p.name())));
    }
}
