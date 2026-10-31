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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for USGS Minerals Ingestor.
 */
public class UsgsMineralsSummaryIngestorTest {

    @Test
    @DisplayName("Verify ingestion of USGS MCS 2026 minerals data")
    void testParseUsgsMinerals() throws IOException {
        File csv = new File("data/sources/usgs_minerals/MCS2026_Commodities_Data.csv");
        assertTrue(csv.exists(), "MCS2026_Commodities_Data.csv must exist");

        Map<String, UsgsMineralsSummaryIngestor.CommodityStats> map = UsgsMineralsSummaryIngestor.parseUsgsMinerals(csv);
        assertFalse(map.isEmpty(), "Parsed commodities must not be empty");

        assertTrue(map.containsKey("Phosphate Rock"), "Must contain Phosphate Rock");
        UsgsMineralsSummaryIngestor.CommodityStats phos = map.get("Phosphate Rock");
        assertTrue(phos.worldProductionMetricTons() > 100_000_000.0, "World phosphate production must be > 100 Mt");
        assertTrue(phos.worldReservesMetricTons() > 1_000_000_000.0, "World phosphate reserves must be > 1 Gt");

        System.out.println("✅ USGS Ingestion successful: " + phos.commodity() 
                + " | Production: " + (phos.worldProductionMetricTons() / 1e6) + " Mt"
                + " | Reserves: " + (phos.worldReservesMetricTons() / 1e6) + " Mt");
    }
}
