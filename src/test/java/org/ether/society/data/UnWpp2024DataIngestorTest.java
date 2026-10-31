/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for {@link UnWpp2024DataIngestor}.
 */
public class UnWpp2024DataIngestorTest {

    @Test
    public void testIngestWorldPopulationGzip() throws Exception {
        File gzFile = new File("data/sources/un_wpp/WPP2024_TotalPopulationBySex.csv.gz");
        if (!gzFile.exists()) {
            return;
        }

        File outFile = new File("data/ether/earth/variables/projections/un_wpp_2024_world_projections_1d.json");
        UnWpp2024DataIngestor.ingestAndExportJson(gzFile, outFile);

        // Also update legacy mirror if directory exists
        File legacyFile = new File("data/variables/projections/un_wpp_2024_world_projections_1d.json");
        if (legacyFile.getParentFile().exists()) {
            UnWpp2024DataIngestor.ingestAndExportJson(gzFile, legacyFile);
        }

        assertTrue(outFile.exists(), "UN WPP 2024 json should be generated");
        assertTrue(outFile.length() > 500, "Output file should have content");

        var variants = UnWpp2024DataIngestor.ingestWorldProjections(gzFile);
        assertNotNull(variants.get("Medium"));
        assertTrue(variants.get("Medium").containsKey(2025));
        assertTrue(variants.get("Medium").containsKey(2100));

        double pop2025 = variants.get("Medium").get(2025).populationInMillions();
        double pop2100 = variants.get("Medium").get(2100).populationInMillions();

        assertTrue(pop2025 > 8000.0 && pop2025 < 8500.0, "2025 population should be ~8.23 billion");
        assertTrue(pop2100 > 9500.0 && pop2100 < 11000.0, "2100 population should be ~10.18 billion");
    }
}
