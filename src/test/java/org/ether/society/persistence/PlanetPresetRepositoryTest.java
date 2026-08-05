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
        PlanetPreset custom = new PlanetPreset(
                "TestPlanet", 5, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 0.5, 1.0, 0.4, 30.0, 21.0, 0.30, 1.0,
                false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
                false, "none", false, "", 12445L, false, "", 13345L, false, "", 14345L);
        int initialSize = repository.findAll().size();

        repository.save(custom);
        List<PlanetPreset> updated = repository.findAll();

        assertEquals(initialSize + 1, updated.size());
        assertTrue(updated.stream().anyMatch(p -> "TestPlanet".equals(p.name())));
    }
}
