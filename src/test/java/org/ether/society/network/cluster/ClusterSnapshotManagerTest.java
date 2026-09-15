/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterSnapshotManagerTest {

    private Path tempDir;
    private ClusterSnapshotManager snapshotManager;

    @BeforeEach
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("ether_snapshots_test");
        snapshotManager = new ClusterSnapshotManager(tempDir, 3);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    public void testSaveAndRestoreSnapshot() throws IOException {
        WorldBuffer buffer = new WorldBuffer(50);
        buffer.getElevation()[12] = 2500.0f;
        buffer.getTemperature()[12] = 14.2f;
        buffer.getBiomassHuman()[12] = 80000.0f;

        Path savedFile = snapshotManager.saveSnapshot(12345L, buffer);
        assertNotNull(savedFile);
        assertTrue(Files.exists(savedFile));

        WorldBuffer target = new WorldBuffer(50);
        WorldBufferWireCodec.ChunkPayload payload = snapshotManager.restoreSnapshot(savedFile, target);

        assertEquals(12345L, payload.getTickId());
        assertEquals(2500.0f, target.getElevation()[12], 1e-4);
        assertEquals(14.2f, target.getTemperature()[12], 1e-4);
        assertEquals(80000.0f, target.getBiomassHuman()[12], 1e-4);
    }

    @Test
    public void testPruningOldSnapshots() throws IOException {
        WorldBuffer buffer = new WorldBuffer(10);

        for (int i = 1; i <= 5; i++) {
            snapshotManager.saveSnapshot(i * 100L, buffer);
        }

        File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".gz"));
        assertNotNull(files);
        // maxRetainedSnapshots is 3
        assertEquals(3, files.length, "Should keep at most 3 latest snapshots");

        Path latest = snapshotManager.getLatestSnapshot();
        assertNotNull(latest);
        assertTrue(latest.getFileName().toString().contains("500"), "Latest snapshot should be for tick 500");
    }
}
