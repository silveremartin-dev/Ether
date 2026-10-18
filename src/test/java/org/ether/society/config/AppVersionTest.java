/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link AppVersion}.
 * Verifies version resolution, regex parsing of beta and RC versions,
 * and user-facing badge generation.
 */
class AppVersionTest {

    @Test
    @DisplayName("formatDisplayVersion formats standard beta releases into concise badges")
    void testFormatDisplayVersionBeta() {
        assertEquals("1.0 b2", AppVersion.formatDisplayVersion("1.0.0-beta.2"));
        assertEquals("1.0 b3", AppVersion.formatDisplayVersion("1.0.0-beta.3"));
        assertEquals("2.1 b1", AppVersion.formatDisplayVersion("2.1-b1"));
    }

    @Test
    @DisplayName("formatDisplayVersion formats release candidates into concise badges")
    void testFormatDisplayVersionRc() {
        assertEquals("1.0 rc1", AppVersion.formatDisplayVersion("1.0.0-rc.1"));
        assertEquals("1.2 rc4", AppVersion.formatDisplayVersion("1.2-rc4"));
    }

    @Test
    @DisplayName("formatDisplayVersion preserves standard GA versions and handles null/blank")
    void testFormatDisplayVersionGeneral() {
        assertEquals("1.0.0", AppVersion.formatDisplayVersion("1.0.0"));
        assertEquals("1.0", AppVersion.formatDisplayVersion(null));
        assertEquals("1.0", AppVersion.formatDisplayVersion("   "));
    }

    @Test
    @DisplayName("AppVersion resolves current project version dynamically")
    void testProjectVersionResolution() {
        String raw = AppVersion.getRawVersion();
        String display = AppVersion.getDisplayVersion();
        String full = AppVersion.getFullDisplayVersion();
        String title = AppVersion.getReleaseTitle();

        assertNotNull(raw);
        assertNotNull(display);
        assertTrue(raw.contains("1.0.0"));
        assertTrue(display.contains("1.0"));
        assertTrue(full.startsWith("v"));
        assertTrue(title.startsWith("Release "));
    }
}
