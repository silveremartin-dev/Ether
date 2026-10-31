/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for IAMC SSP CSV Ingestion into standardized 1D projection datasets.
 */
public class IamcSspCsvDataIngestorTest {

    @Test
    @DisplayName("Verify parsing and export of IIASA SSP World CSV")
    void testIngestIiasaWorldCsv() throws IOException {
        File csv = new File("data/sources/iiasa_ssp/iiasa_ssp_world_full.csv");
        assertTrue(csv.exists(), "iiasa_ssp_world_full.csv must exist");

        List<IamcSspCsvDataIngestor.IngestedSeries> series = IamcSspCsvDataIngestor.parseIamcCsv(csv, "World");
        assertFalse(series.isEmpty(), "Ingested series must not be empty");

        File outDir = new File("data/variables/projections");
        IamcSspCsvDataIngestor.exportStandardizedProjectionFiles(series, outDir);

        File ssp1 = new File(outDir, "ssp1_iamc_ingested_1d.json");
        File ssp2 = new File(outDir, "ssp2_iamc_ingested_1d.json");
        assertTrue(ssp1.exists() || ssp2.exists(), "At least one ingested SSP JSON must exist");

        System.out.println("✅ Ingested " + series.size() + " series from official IIASA SSP database!");
    }
}
