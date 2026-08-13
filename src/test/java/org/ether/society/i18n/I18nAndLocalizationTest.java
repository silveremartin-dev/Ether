/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Unit Test Suite verifying internationalization completeness, bundle keys integrity,
 * and lack of hardcoded fallback errors across all 5 supported locales (EN, FR, ES, DE, ZH).
 */
public class I18nAndLocalizationTest {

    private static ResourceBundle bundleEn;
    private static ResourceBundle bundleFr;
    private static ResourceBundle bundleEs;
    private static ResourceBundle bundleDe;
    private static ResourceBundle bundleZh;

    @BeforeAll
    public static void setUp() {
        bundleEn = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        bundleFr = ResourceBundle.getBundle("i18n.messages", Locale.FRENCH);
        bundleEs = ResourceBundle.getBundle("i18n.messages", new Locale("es"));
        bundleDe = ResourceBundle.getBundle("i18n.messages", Locale.GERMAN);
        bundleZh = ResourceBundle.getBundle("i18n.messages", Locale.CHINESE);
    }

    @Test
    @DisplayName("Verify bundles load for all 5 supported languages")
    public void testBundlesExistAndLoad() {
        assertNotNull(bundleEn, "English resource bundle must be loaded");
        assertNotNull(bundleFr, "French resource bundle must be loaded");
        assertNotNull(bundleEs, "Spanish resource bundle must be loaded");
        assertNotNull(bundleDe, "German resource bundle must be loaded");
        assertNotNull(bundleZh, "Chinese resource bundle must be loaded");
    }

    @Test
    @DisplayName("Verify key parity: All keys in English must exist in FR, ES, DE, and ZH")
    public void testKeyParityAcrossLocales() {
        Set<String> enKeys = bundleEn.keySet();
        assertFalse(enKeys.isEmpty(), "English resource bundle must not be empty");

        List<String> missingInFr = new ArrayList<>();
        List<String> missingInEs = new ArrayList<>();
        List<String> missingInDe = new ArrayList<>();
        List<String> missingInZh = new ArrayList<>();

        for (String key : enKeys) {
            if (!bundleFr.containsKey(key) || bundleFr.getString(key).trim().isEmpty()) {
                missingInFr.add(key);
            }
            if (!bundleEs.containsKey(key) || bundleEs.getString(key).trim().isEmpty()) {
                missingInEs.add(key);
            }
            if (!bundleDe.containsKey(key) || bundleDe.getString(key).trim().isEmpty()) {
                missingInDe.add(key);
            }
            if (!bundleZh.containsKey(key) || bundleZh.getString(key).trim().isEmpty()) {
                missingInZh.add(key);
            }
        }

        assertTrue(missingInFr.isEmpty(), "Missing or empty FR keys: " + missingInFr);
        assertTrue(missingInEs.isEmpty(), "Missing or empty ES keys: " + missingInEs);
        assertTrue(missingInDe.isEmpty(), "Missing or empty DE keys: " + missingInDe);
        assertTrue(missingInZh.isEmpty(), "Missing or empty ZH keys: " + missingInZh);
    }

    @Test
    @DisplayName("Verify critical execution context keys exist across all bundles")
    public void testExecutionContextKeys() {
        String[] criticalKeys = {
            "tab.execution_context",
            "exec.title",
            "exec.mode",
            "exec.mode.cpu",
            "exec.mode.gpu",
            "exec.mode.cluster",
            "exec.cluster.title",
            "exec.cluster.start_master",
            "exec.cluster.join",
            "exec.cluster.test",
            "exec.cluster.refresh",
            "exec.btn.launch"
        };

        for (String key : criticalKeys) {
            assertTrue(bundleEn.containsKey(key), "EN bundle missing: " + key);
            assertTrue(bundleFr.containsKey(key), "FR bundle missing: " + key);
            assertTrue(bundleEs.containsKey(key), "ES bundle missing: " + key);
            assertTrue(bundleDe.containsKey(key), "DE bundle missing: " + key);
            assertTrue(bundleZh.containsKey(key), "ZH bundle missing: " + key);
        }
    }

    @Test
    @DisplayName("Verify Performance HUD keys exist across all bundles")
    public void testHudKeys() {
        String[] hudKeys = {
            "ui.hud.fps",
            "ui.hud.memory",
            "ui.hud.cells",
            "ui.hud.zoom"
        };

        for (String key : hudKeys) {
            assertTrue(bundleEn.containsKey(key), "EN bundle missing: " + key);
            assertTrue(bundleFr.containsKey(key), "FR bundle missing: " + key);
            assertTrue(bundleEs.containsKey(key), "ES bundle missing: " + key);
            assertTrue(bundleDe.containsKey(key), "DE bundle missing: " + key);
            assertTrue(bundleZh.containsKey(key), "ZH bundle missing: " + key);
        }
    }

    @Test
    @DisplayName("Test I18n manager utility class methods")
    public void testI18nManagerClass() {
        I18n.setLanguage(Language.ENGLISH);
        assertEquals(Language.ENGLISH, I18n.getCurrentLanguage());
        assertNotNull(I18n.get("app.title"));

        I18n.setLanguage(Language.FRENCH);
        assertEquals(Language.FRENCH, I18n.getCurrentLanguage());
        assertNotNull(I18n.get("app.title"));

        String resultWithDefault = I18n.getOrDefault("non_existent_key_xyz", "Default Custom Fallback");
        assertEquals("Default Custom Fallback", resultWithDefault);
    }

    @Test
    @DisplayName("Verify critical Planet Generator keys exist across all bundles")
    public void testPlanetGeneratorKeys() {
        String[] planetKeys = {
            "planet.preset",
            "planet.section.header",
            "planet.section.presets",
            "planet.section.general",
            "planet.section.astro",
            "planet.section.topo",
            "planet.section.atmosphere",
            "planet.section.climate",
            "planet.preview.title",
            "planet.param.body_type",
            "planet.body_type.planet",
            "planet.body_type.satellite",
            "planet.param.parent_mass",
            "planet.param.parent_dist",
            "planet.param.resolution",
            "planet.param.radius",
            "planet.param.day_length",
            "planet.param.axial_tilt",
            "planet.param.year_length",
            "planet.param.distance_sun",
            "planet.param.solar_lum",
            "planet.param.avg_temp",
            "planet.param.irradiance",
            "planet.param.seed",
            "planet.param.min_alt",
            "planet.param.max_alt",
            "planet.param.alt_range",
            "planet.param.water_level",
            "planet.param.noise_freq",
            "planet.param.noise_scale",
            "planet.param.temp_grad",
            "planet.param.oxygen",
            "planet.param.co2",
            "planet.param.albedo",
            "planet.param.atmo_pressure",
            "planet.radio.procedural",
            "planet.radio.import",
            "planet.radio.import_wms",
            "planet.map.preset_body",
            "planet.map.elevation",
            "planet.map.biomes",
            "planet.map.resources",
            "planet.map.climate",
            "planet.map.rainfall",
            "planet.map.seasonality",
            "planet.map.earth",
            "planet.map.mars",
            "planet.map.venus",
            "planet.map.moon",
            "planet.map.none",
            "planet.map.btn_load",
            "planet.combo.prompt_source",
            "planet.btn.export_procedural",
            "planet.btn.export_climate_temp",
            "planet.btn.export_climate_precip",
            "planet.btn.export_climate_season",
            "planet.view.heightmap",
            "planet.view.temperature",
            "planet.view.precipitation",
            "planet.view.seasonality",
            "planet.hint.map_resolution",
            "planet.dialog.export_heightmap",
            "planet.map.status_fetching",
            "planet.map.status_success",
            "planet.map.status_error",
            "planet.stats.ocean_land",
            "planet.short.day",
            "planet.short.tilt",
            "planet.short.year",
            "planet.short.dist",
            "planet.short.temp",
            "planet.tooltip.parent_mass",
            "planet.tooltip.parent_dist",
            "planet.tooltip.body_type",
            "planet.tooltip.radius",
            "planet.tooltip.day_length",
            "planet.tooltip.axial_tilt",
            "planet.tooltip.year_length",
            "planet.tooltip.distance_sun",
            "planet.tooltip.solar_lum",
            "planet.tooltip.avg_temp",
            "planet.tooltip.min_alt",
            "planet.tooltip.max_alt",
            "planet.tooltip.water_level",
            "planet.tooltip.noise_freq",
            "planet.tooltip.noise_scale",
            "planet.tooltip.seed",
            "planet.tooltip.seed_rand",
            "planet.tooltip.map_source",
            "planet.tooltip.elev_map",
            "planet.tooltip.fetch_online",
            "planet.tooltip.fetch_online_climate",
            "planet.tooltip.export_map",
            "planet.tooltip.elev_load",
            "planet.tooltip.elev_clear",
            "planet.tooltip.climate_load",
            "planet.tooltip.climate_clear",
            "planet.tooltip.rainfall_load",
            "planet.tooltip.rainfall_clear",
            "planet.tooltip.seasonality_load",
            "planet.tooltip.seasonality_clear",
            "planet.tooltip.preview",
            "planet.climate.temp.header",
            "planet.climate.precip.header",
            "planet.climate.season.header",
            "planet.climate.seed_label",
            "planet.climate.source_label",
            "planet.climate.temp.hint",
            "planet.climate.precip.hint",
            "planet.climate.season.hint",
            "planet.climate.temp.format",
            "planet.climate.precip.format",
            "planet.climate.season.format"
        };

        for (String key : planetKeys) {
            assertTrue(bundleEn.containsKey(key), "EN bundle missing: " + key);
            assertTrue(bundleFr.containsKey(key), "FR bundle missing: " + key);
            assertTrue(bundleEs.containsKey(key), "ES bundle missing: " + key);
            assertTrue(bundleDe.containsKey(key), "DE bundle missing: " + key);
            assertTrue(bundleZh.containsKey(key), "ZH bundle missing: " + key);
        }
    }

    @Test
    @DisplayName("Verify Globe 3D and Simulation UI keys exist across all 5 bundles")
    public void testGlobeAndSimulationUiKeys() {
        String[] simKeys = {
            "sim.header.scenario",
            "sim.header.date",
            "sim.header.tps",
            "sim.card.time",
            "sim.card.media",
            "sim.card.layers",
            "sim.layer.mode3d",
            "sim.tooltip.mode3d",
            "sim.layer.relief3d",
            "sim.tooltip.relief3d",
            "sim.layer.autorotate",
            "sim.tooltip.autorotate",
            "sim.layer.contours",
            "sim.tooltip.contours",
            "sim.layer.datacategory",
            "sim.tooltip.datacategory",
            "sim.tooltip.rewind",
            "sim.tooltip.fastrewind",
            "sim.tooltip.stepback",
            "sim.tooltip.start",
            "sim.tooltip.pause",
            "sim.tooltip.stop",
            "sim.tooltip.stepforward",
            "sim.tooltip.fastforward",
            "sim.tooltip.slider",
            "sim.btn.screenshot",
            "sim.tooltip.screenshot",
            "sim.btn.video",
            "sim.tooltip.video",
            "sim.status.dbcheck",
            "sim.legend.title"
        };

        for (String key : simKeys) {
            assertTrue(bundleEn.containsKey(key), "EN bundle missing: " + key);
            assertTrue(bundleFr.containsKey(key), "FR bundle missing: " + key);
            assertTrue(bundleEs.containsKey(key), "ES bundle missing: " + key);
            assertTrue(bundleDe.containsKey(key), "DE bundle missing: " + key);
            assertTrue(bundleZh.containsKey(key), "ZH bundle missing: " + key);
        }
    }
}

