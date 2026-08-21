/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates zero-fallback paleoclimate dataset directories and WorldClim / PaleoCLIM disk structures.
 */
public class PaleoDataIngestionTest {

    @Test
    @DisplayName("Verify existence of paleoclimate dataset subdirectories")
    void testPaleoclimateDirectoriesExist() {
        assertTrue(DataDownloaderService.LOCAL_PALEOCLIM_DIR.exists(), "data/maps/paleoclim/ directory must exist");
        assertTrue(DataDownloaderService.LOCAL_CHELSA_DIR.exists(), "data/maps/chelsa/ directory must exist");
        assertTrue(DataDownloaderService.LOCAL_WORLDCLIM_DIR.exists(), "data/maps/worldclim/ directory must exist");
        assertTrue(DataDownloaderService.LOCAL_PALEOMAP_DIR.exists(), "data/maps/paleomap/ directory must exist");
        assertTrue(DataDownloaderService.LOCAL_PMIP_DIR.exists(), "data/maps/pmip/ directory must exist");
        assertTrue(DataDownloaderService.LOCAL_PANGEA_DIR.exists(), "data/maps/pangea/ directory must exist");
    }

    @Test
    @DisplayName("Verify WorldClim 2.1 biomes baseline file relocation")
    void testWorldClimBaselineFilePresent() {
        File baselineJson = new File(DataDownloaderService.LOCAL_WORLDCLIM_DIR, "worldclim_biomes_baseline.json");
        assertTrue(baselineJson.exists(), "worldclim_biomes_baseline.json must exist in data/maps/worldclim/");
        assertTrue(baselineJson.length() > 500, "worldclim_biomes_baseline.json must be a valid non-empty JSON file");
    }
}
