/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EtherJsonUtilTest {

    @Test
    public void testAtomicWriteAndReadSaveMetadata(@TempDir Path tempDir) throws IOException {
        Path metadataFile = tempDir.resolve("metadata.json");

        SaveMetadata meta = new SaveMetadata(
                "TEST-RUN-001",
                "Test Calibration Run",
                2026,
                10,
                "Test Scenario",
                4096,
                4
        );
        meta.setTimestamp(LocalDateTime.of(2026, 10, 9, 22, 30, 0));

        // Atomic write
        EtherJsonUtil.writePrettyAtomic(metadataFile, meta);

        assertTrue(Files.exists(metadataFile), "metadata.json must exist after atomic write");
        assertTrue(Files.size(metadataFile) > 100, "metadata.json must contain serialized content");

        // Validate content
        String jsonContent = Files.readString(metadataFile);
        assertTrue(jsonContent.contains("TEST-RUN-001"), "JSON must contain ID");
        assertTrue(jsonContent.contains("2026-10-09T22:30:00"), "JSON must properly format LocalDateTime without truncating");

        // Read back
        Optional<SaveMetadata> restored = EtherJsonUtil.readSafely(metadataFile, SaveMetadata.class);
        assertTrue(restored.isPresent(), "Must deserialize cleanly");
        assertEquals("TEST-RUN-001", restored.get().getId());
        assertEquals("Test Calibration Run", restored.get().getName());
        assertEquals(2026, restored.get().getYear());
        assertEquals(4096, restored.get().getCellCount());
        assertNotNull(restored.get().getTimestamp());
    }

    @Test
    public void testAllExistingSavesCanBeListedWithoutCorruption() {
        SimulationSaveManager saveManager = new SimulationSaveManager();
        List<SaveMetadata> saves = saveManager.listSaves();
        assertNotNull(saves);
        assertFalse(saves.isEmpty(), "Saves list should contain canonical saves");

        for (SaveMetadata sm : saves) {
            assertNotNull(sm.getId(), "Save ID must not be null");
            assertNotNull(sm.getName(), "Save Name must not be null");
        }
    }
}
