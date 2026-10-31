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
 * Verification test for {@link KnmiClimateDataIngestor}.
 */
public class KnmiClimateDataIngestorTest {

    @Test
    public void testIngestAllKnmiCmip6Scenarios() throws Exception {
        File knmiDir = new File("data/sources/knmi_climate");
        if (!knmiDir.exists()) {
            return;
        }

        File outFile = new File("data/ether/earth/variables/projections/cmip6_knmi_temperatures_1d.json");
        KnmiClimateDataIngestor.ingestDirectoryAndExportJson(knmiDir, outFile);

        // Also update legacy mirror if directory exists
        File legacyFile = new File("data/variables/projections/cmip6_knmi_temperatures_1d.json");
        if (legacyFile.getParentFile().exists()) {
            KnmiClimateDataIngestor.ingestDirectoryAndExportJson(knmiDir, legacyFile);
        }

        assertTrue(outFile.exists(), "Output file must be generated");
        assertTrue(outFile.length() > 500, "Output file must contain substantial json content");

        // Test individual ingestion of SSP2-4.5
        File ssp245File = new File(knmiDir, "icmip6_tas_mon_modmean_ssp245_0-360E_-90-90N_firstyear-lastyear_n_su.dat.txt");
        if (ssp245File.exists()) {
            KnmiClimateDataIngestor.ScenarioClimateSeries series = KnmiClimateDataIngestor.ingestKnmiDatFile(ssp245File, "ssp245");
            assertNotNull(series);
            assertEquals("ssp245", series.scenarioId());
            assertTrue(series.preIndustrialBaselineMean() > 13.0 && series.preIndustrialBaselineMean() < 14.5);
            assertTrue(series.annualPoints().containsKey(2025));
            assertTrue(series.annualPoints().containsKey(2100));

            // Verify warming trajectory
            double anom2025 = series.annualPoints().get(2025).temperatureAnomaly();
            double anom2100 = series.annualPoints().get(2100).temperatureAnomaly();
            assertTrue(anom2025 > 1.0 && anom2025 < 2.0, "2025 anomaly should be ~1.5C");
            assertTrue(anom2100 > 2.5 && anom2100 < 4.0, "2100 anomaly for SSP2-4.5 should be ~3.15C");
        }
    }
}
