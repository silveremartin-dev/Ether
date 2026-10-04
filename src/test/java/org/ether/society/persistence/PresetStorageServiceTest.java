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
        assertTrue(Files.exists(scDir.resolve("out_of_africa.json")), "out_of_africa.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mars_colony_2050.json")), "mars_colony_2050.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("moon_shackleton_2050.json")), "moon_shackleton_2050.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("venus_cloud_cities_2060.json")), "venus_cloud_cities_2060.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mercury_caloris_forge_2070.json")), "mercury_caloris_forge_2070.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("titan_cryo_methane_2080.json")), "titan_cryo_methane_2080.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("super_earth_gaia_2100.json")), "super_earth_gaia_2100.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("eyeball_world_twilight_2120.json")), "eyeball_world_twilight_2120.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("oceania_aquapolis_2090.json")), "oceania_aquapolis_2090.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("boreas_subglacial_2075.json")), "boreas_subglacial_2075.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("archipelago_seasteading_2055.json")), "archipelago_seasteading_2055.json must exist on disk");
    }

    @Test
    @DisplayName("Should verify leaders catalog preset exists in presets/leaders/")
    void testLeadersPresetLocation() {
        Path leadersFile = EtherPaths.getPresetsLeadersDir().resolve("earth_historical_leaders.json");
        assertTrue(Files.exists(leadersFile), "earth_historical_leaders.json must exist under presets/leaders/");
    }
}

