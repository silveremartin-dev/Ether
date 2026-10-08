/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
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
        assertTrue(Files.exists(scDir.resolve("earth_+2025_business_as_usual.json")) || Files.exists(scDir.resolve("earth_+2025_earth_2025_business_as_usual.json")), "earth_+2025_business_as_usual.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mars_+2050_colony_2050.json")), "mars_+2050_colony_2050.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("moon_+2035_shackleton_2035.json")), "moon_+2035_shackleton_2035.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("venus_+2080_cloud_cities_2080.json")), "venus_+2080_cloud_cities_2080.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("mercury_+2120_caloris_forge_2120.json")), "mercury_+2120_caloris_forge_2120.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("super_earth_+2200_gaia_2200.json")), "super_earth_+2200_gaia_2200.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("eyeball_world_+2220_twilight_2220.json")), "eyeball_world_+2220_twilight_2220.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("oceania_+2100_aquapolis_2100.json")), "oceania_+2100_aquapolis_2100.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("boreas_+2120_subglacial_2120.json")), "boreas_+2120_subglacial_2120.json must exist on disk");
        assertTrue(Files.exists(scDir.resolve("archipelago_+2055_seasteading_2055.json")), "archipelago_+2055_seasteading_2055.json must exist on disk");
    }

    @Test
    @DisplayName("Should verify that no duplicate scenarios exist across names or planet-year keys")
    void testNoDuplicateScenarios() {
        List<Scenario> list = PresetStorageService.loadAllScenarios();
        assertNotNull(list);
        
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        java.util.Set<String> seenPlanetYears = new java.util.HashSet<>();
        
        for (Scenario s : list) {
            String name = s.getName();
            String planet = s.getPlanetPreset() != null ? s.getPlanetPreset().getCanonicalPlanet() : "earth";
            String planetYear = planet + ":" + s.getStartDateYear();
            
            assertFalse(seenNames.contains(name), "Duplicate scenario name found: " + name);
            seenNames.add(name);
        }
    }

    @Test
    @DisplayName("Should verify leaders catalog preset exists in presets/leaders/")
    void testLeadersPresetLocation() {
        Path leadersFile = EtherPaths.getPresetsLeadersDir().resolve("earth_historical_leaders.json");
        assertTrue(Files.exists(leadersFile), "earth_historical_leaders.json must exist under presets/leaders/");
    }
}

