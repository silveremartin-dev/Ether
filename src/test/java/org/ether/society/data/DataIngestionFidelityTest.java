/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Verification Suite for Maps Data Repatriation & Ingestion Integrity.
 * Verifies that all 18 active provider directories in data/maps/ have authentic,
 * valid provider.json metadata files and valid file bindings.
 *
 * @author Silvere Martin-Michiellot
 */
public class DataIngestionFidelityTest {

    private static final String MAPS_BASE_PATH = "data/maps";

    private static final String[] EXPECTED_ACTIVE_PROVIDERS = {
            "usgs",
            "gebco",
            "whymap_groundwater",
            "usgs_mrds",
            "bgr_germany",
            "iaea_nfcis",
            "wep_world_energy",
            "lpi_lunar",
            "nasa_pds",
            "hyde34",
            "seshat",
            "archaeoglobe",
            "naturalearth",
            "maddison",
            "worldclim",
            "paleomap",
            "pangea",
            "paleoclim"
    };

    @Test
    @DisplayName("Verify All Active Maps Provider Metadata JSONs")
    public void testAllActiveProvidersMetadataIntegrity() throws Exception {
        File mapsFolder = new File(MAPS_BASE_PATH);
        assertTrue(mapsFolder.exists() && mapsFolder.isDirectory(), "Base maps directory data/maps/ must exist");

        for (String providerId : EXPECTED_ACTIVE_PROVIDERS) {
            File providerDir = new File(mapsFolder, providerId);
            assertTrue(providerDir.exists() && providerDir.isDirectory(),
                    "Provider directory data/maps/" + providerId + " must exist");

            File providerJsonFile = new File(providerDir, "provider.json");
            assertTrue(providerJsonFile.exists(), "provider.json must exist in data/maps/" + providerId);

            String content = Files.readString(providerJsonFile.toPath());
            assertTrue(content.contains("\"provider_id\":"), "provider_id must be specified in " + providerId);
            assertTrue(content.contains("\"name\":"), "name must be specified in " + providerId);
            assertTrue(content.contains("\"download_url\":"), "download_url must be specified in " + providerId);
            assertTrue(content.contains("\"status\": \"AUTHENTIC\""), "status must be AUTHENTIC in " + providerId);
        }
    }

    @Test
    @DisplayName("Verify Redundant Empty Providers Removed")
    public void testRedundantEmptyProvidersAbsence() {
        File mapsFolder = new File(MAPS_BASE_PATH);
        String[] deprecatedProviders = {"pmip", "chelsa", "esa_geospatial"};

        for (String providerId : deprecatedProviders) {
            File providerDir = new File(mapsFolder, providerId);
            assertFalse(providerDir.exists(),
                    "Redundant empty directory data/maps/" + providerId + " should be removed");
        }
    }

    @Test
    @DisplayName("Verify Dynamic Sea Level Transition Engine (Glacial Emergence & Marine Submersion)")
    public void testDynamicSeaLevelTransitionEngine() {
        org.ether.society.database.H3Cell shallowShelfCell = new org.ether.society.database.H3Cell(2001L, 54.0, 3.0); // Doggerland North Sea (-50m)
        shallowShelfCell.setElevation(-50.0);
        shallowShelfCell.setBiome(org.ether.society.model.Biome.OCEAN);

        org.ether.society.database.H3Cell coastalLandCell = new org.ether.society.database.H3Cell(2002L, 52.0, 4.0); // Coastal Netherlands (+5m)
        coastalLandCell.setElevation(5.0);
        coastalLandCell.setBiome(org.ether.society.model.Biome.PLAINS);

        java.util.List<org.ether.society.database.H3Cell> testCells = java.util.List.of(shallowShelfCell, coastalLandCell);

        // 1. Apply Glacial Maximum regression (-120m)
        org.ether.society.procedural.SeaLevelTransitionEngine.applySeaLevelTransition(testCells, -120.0);

        assertNotEquals(org.ether.society.model.Biome.OCEAN, shallowShelfCell.getBiome(),
                "Continental shelf at -50m elevation must emerge from ocean when sea level drops by -120m");
        assertEquals(org.ether.society.model.Biome.PLAINS, coastalLandCell.getBiome(),
                "Coastal land at +5m remains land during sea level drop");

        // 2. Apply Super-Greenhouse submersion (+50m)
        org.ether.society.procedural.SeaLevelTransitionEngine.applySeaLevelTransition(testCells, +50.0);

        assertEquals(org.ether.society.model.Biome.OCEAN, shallowShelfCell.getBiome(),
                "Shelf at -50m must submerge under +50m sea level rise");
        assertEquals(org.ether.society.model.Biome.OCEAN, coastalLandCell.getBiome(),
                "Coastal land at +5m elevation must submerge into ocean under +50m sea level rise");
    }
}
