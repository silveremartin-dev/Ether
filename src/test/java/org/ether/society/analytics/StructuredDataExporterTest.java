/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link StructuredDataExporter}.
 * Validates cross-platform export of simulation statistical series to CSV, TSV, JSON, and NDJSON formats.
 */
class StructuredDataExporterTest {

    private StatisticalReplayEngine.ReconstitutedSeries createSampleSeries() {
        StatisticalReplayEngine.ReconstitutedSeries series =
                new StatisticalReplayEngine.ReconstitutedSeries("test_gdp", "Global GDP", "sum(capital)", "T$");
        series.addPoint(100L, 50.5);
        series.addPoint(200L, 120.8);
        return series;
    }

    @Test
    @DisplayName("exportSeriesToFile writes valid CSV with header and data rows")
    void testExportCsv(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("series.csv").toFile();
        StructuredDataExporter.exportSeriesToFile(createSampleSeries(), csvFile, StructuredDataExporter.ExportFormat.CSV);

        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("tick,value"));
        assertTrue(content.contains("100,50.500000"));
        assertTrue(content.contains("200,120.800000"));
    }

    @Test
    @DisplayName("exportSeriesToFile writes valid JSON with summary metrics")
    void testExportJson(@TempDir Path tempDir) throws IOException {
        File jsonFile = tempDir.resolve("series.json").toFile();
        StructuredDataExporter.exportSeriesToFile(createSampleSeries(), jsonFile, StructuredDataExporter.ExportFormat.JSON);

        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("\"id\": \"test_gdp\""));
        assertTrue(content.contains("\"min\": 50.500000"));
        assertTrue(content.contains("\"max\": 120.800000"));
    }

    @Test
    @DisplayName("exportSeriesToFile writes valid NDJSON line-delimited records")
    void testExportNdJson(@TempDir Path tempDir) throws IOException {
        File ndjsonFile = tempDir.resolve("series.ndjson").toFile();
        StructuredDataExporter.exportSeriesToFile(createSampleSeries(), ndjsonFile, StructuredDataExporter.ExportFormat.NDJSON_LINES);

        assertTrue(ndjsonFile.exists());
        var lines = Files.readAllLines(ndjsonFile.toPath());
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"tick\":100"));
        assertTrue(lines.get(1).contains("\"tick\":200"));
    }

    @Test
    @DisplayName("exportSeriesToFile writes valid TSV tab-delimited records")
    void testExportTsv(@TempDir Path tempDir) throws IOException {
        File tsvFile = tempDir.resolve("series.tsv").toFile();
        StructuredDataExporter.exportSeriesToFile(createSampleSeries(), tsvFile, StructuredDataExporter.ExportFormat.TSV);

        assertTrue(tsvFile.exists());
        String content = Files.readString(tsvFile.toPath());
        assertTrue(content.contains("tick\tvalue"));
        assertTrue(content.contains("100\t50.500000"));
    }
}
