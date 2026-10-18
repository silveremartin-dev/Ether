/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link SimulationReportGenerator}.
 * Validates report generation, Markdown document structure, UTF-8 encoding, and null resilience.
 */
class SimulationReportGeneratorTest {

    @Test
    @DisplayName("Report generator produces valid English Markdown report with null engine")
    void testGenerateReportWithNullEngine(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("report_null_engine.md").toFile();
        File generated = SimulationReportGenerator.generateReport(null, outputFile);

        assertNotNull(generated);
        assertTrue(generated.exists());
        assertTrue(generated.length() > 0);

        String content = Files.readString(generated.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("# 🌍 Ether - Cliodynamic & Thermodynamic Planetary Assessment Report"));
        assertTrue(content.contains("**Simulation Year:**"));
        assertTrue(content.contains("Assessment of Planetary Boundaries"));
        assertTrue(content.contains("Cliodynamic Advisor & Physical Alerts"));
    }

    @Test
    @DisplayName("Report generator defaults output file when null is passed")
    void testGenerateReportDefaultOutputFile() throws IOException {
        File generated = SimulationReportGenerator.generateReport(null, null);
        try {
            assertNotNull(generated);
            assertTrue(generated.exists());
            String content = Files.readString(generated.toPath(), StandardCharsets.UTF_8);
            assertTrue(content.contains("Assessment of Planetary Boundaries"));
        } finally {
            if (generated != null && generated.exists()) {
                generated.delete();
            }
        }
    }
}
