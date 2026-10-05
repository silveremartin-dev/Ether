/*
 * MIT License
 *
 * Copyright (c) 2024-2026 SilvÃ¨re Martin-Michiellot
 */
package org.ether.society.persistence;

import org.ether.society.config.EtherPaths;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetStorageServiceTest {

    @BeforeAll
    /*
     * Setup operation.
     * <p>
     * Executes operational logic for {@code PresetStorageServiceTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    static void setup() {
        EtherPaths.initPaths();
        PresetStorageService.exportAllFactoryPresets();
    }

    @Test
    @DisplayName("Should export and load all planet presets")
    void testPlanetPresetsExportAndLoad() {
        List<PlanetPreset> list = PresetStorageService.loadAllPlanetPresets();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.size() >= 20, "Should contain at least 20 planet presets");

        // Verify disk files exist
        Path planetsDir = EtherPaths.getPresetsPlanetsDir();
        assertTrue(Files.exists(planetsDir.resolve("earth_modern.json")), "earth_modern.json must exist on disk");
        assertTrue(Files.exists(planetsDir.resolve("mars.json")), "mars.json must exist on disk");
        assertTrue(Files.exists(planetsDir.resolve("super_earth.json")), "super_earth.json must exist on disk");
    }

    @Test
    @DisplayName("Should export and load all ecology presets")
    void testEcologyPresetsExportAndLoad() {
        List<EcologyPreset> list = PresetStorageService.loadAllEcologyPresets();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.size() >= 20, "Should contain at least 20 ecology presets");

        Path ecoDir = EtherPaths.getPresetsEcologyDir();
        assertTrue(Files.exists(ecoDir.resolve("earth_modern.json")), "earth_modern.json must exist on disk");
        assertTrue(Files.exists(ecoDir.resolve("mars.json")), "mars.json must exist on disk");
    }

    @Test
    @DisplayName("Should export and load all scenario presets including exoplanetary colonies")
    void testScenarioPresetsExportAndLoad() {
        List<Scenario> list = PresetStorageService.loadAllScenarios();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.size() >= 40, "Should contain at least 40 scenario presets");

        Path scDir = EtherPaths.getPresetsScenariosDir();
        assertTrue(Files.exists(scDir.resolve("earth_+0_roman_empire.json")), "earth_+0_roman_empire.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("earth_+1347_black_death_1347.json")), "earth_+1347_black_death_1347.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mars_+2050_colony_2050.json")), "mars_+2050_colony_2050.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("moon_+2050_shackleton_2050.json")), "moon_+2050_shackleton_2050.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("venus_+2060_cloud_cities_2060.json")), "venus_+2060_cloud_cities_2060.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mercury_+2070_caloris_forge_2070.json")), "mercury_+2070_caloris_forge_2070.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("super_earth_+2100_gaia_2100.json")), "super_earth_+2100_gaia_2100.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("eyeball_world_+2120_twilight_2120.json")), "eyeball_world_+2120_twilight_2120.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("oceania_+2090_aquapolis_2090.json")), "oceania_+2090_aquapolis_2090.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("boreas_+2075_subglacial_2075.json")), "boreas_+2075_subglacial_2075.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("archipelago_+2055_seasteading_2055.json")), "archipelago_+2055_seasteading_2055.json must exist on disk");
    }

    @Test
    @DisplayName("Should verify leaders catalog preset exists in presets/leaders/")
    void testLeadersPresetLocation() {
        Path leadersFile = EtherPaths.getPresetsLeadersDir().resolve("earth_historical_leaders.json");
        assertTrue(Files.exists(leadersFile), "earth_historical_leaders.json must exist under presets/leaders/");
    }
}

