/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.data.sync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for {@link GitHubContentSyncService}.
 * Tests hash calculation, path filtering, and non-destructive conflict protection.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class GitHubContentSyncServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testIsAuthorizedSyncPath() {
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("data/presets/scenarios/earth_+2026_modern.json"));
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("data/presets/planets/earth.json"));
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("data/events/historical_events.json"));
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("data/history/timeline_rome.json"));
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("data/maps/ether/earth/0/elevation.png"));
        assertTrue(GitHubContentSyncService.isAuthorizedSyncPath("saves/run-alexander_hellenistic_334bce_forced/metadata.json"));

        // Reject unauthorized paths
        assertFalse(GitHubContentSyncService.isAuthorizedSyncPath("src/main/java/Main.java"));
        assertFalse(GitHubContentSyncService.isAuthorizedSyncPath("pom.xml"));
        assertFalse(GitHubContentSyncService.isAuthorizedSyncPath("data/cache/temp_tile.png"));
        assertFalse(GitHubContentSyncService.isAuthorizedSyncPath("saves/my_custom_user_save/metadata.json"));
    }

    @Test
    public void testSha256Computation() throws IOException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Ether Simulation Engine v1.0.0-beta.2");

        String hash = GitHubContentSyncService.computeSha256(testFile);
        assertNotNull(hash);
        assertFalse(hash.isBlank());
        assertEquals(64, hash.length(), "SHA-256 hash must be 64 hex characters");

        // Identical content must produce identical hash
        Path testFile2 = tempDir.resolve("sample2.txt");
        Files.writeString(testFile2, "Ether Simulation Engine v1.0.0-beta.2");
        assertEquals(hash, GitHubContentSyncService.computeSha256(testFile2));

        // Altered content must produce different hash
        Path testFile3 = tempDir.resolve("sample3.txt");
        Files.writeString(testFile3, "User Modified Scenario Parameters");
        assertNotEquals(hash, GitHubContentSyncService.computeSha256(testFile3));
    }
}
