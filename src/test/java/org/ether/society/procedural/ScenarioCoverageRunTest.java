/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.model.Scenario;
import org.ether.society.persistence.PresetStorageService;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.SimulationSaveManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verification & Coverage Test Suite validating preset scenarios registration,
 * save metadata integrity, and canonical historical benchmark runs indexing.
 * Heavy computational runs are orchestrated via dedicated Google Cloud cluster jobs.
 */
public class ScenarioCoverageRunTest {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioCoverageRunTest.class);

    @Test
    @DisplayName("Verify that scenario presets and canonical simulation saves are correctly indexed")
    public void testPresetScenariosAndCanonicalSaveIndex() {
        SimulationSaveManager saveMgr = new SimulationSaveManager();
        List<SaveMetadata> existingSaves = saveMgr.listSaves();
        logger.info("Found {} canonical save(s) on disk.", existingSaves.size());

        List<Scenario> allScenarios = PresetStorageService.loadAllScenarios();
        assertNotNull(allScenarios, "Scenario presets must not be null");
        assertFalse(allScenarios.isEmpty(), "Scenario presets must not be empty");
        logger.info("Loaded {} scenario presets from registry.", allScenarios.size());

        // Validate that all existing saves have non-null IDs and valid metadata
        for (SaveMetadata save : existingSaves) {
            assertNotNull(save.getId(), "Save ID must not be null");
            assertNotNull(save.getName(), "Save name must not be null");
            assertTrue(save.getCellCount() > 0, "Save cell count must be positive");
        }
    }
}
