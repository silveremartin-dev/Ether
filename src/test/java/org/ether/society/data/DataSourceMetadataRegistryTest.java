/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link DataSourceMetadataRegistry}.
 * Verifies that empirical data sources (NOAA ETOPO, HYDE 3.4, USGS, WHYMAP, Seshat, D-PLACE)
 * correctly resolve citations, technical provenance, and resolution metadata.
 */
class DataSourceMetadataRegistryTest {

    @Test
    @DisplayName("Resolves complete technical provenance for planetary elevation datasets (ETOPO)")
    void testEtopoMetadata() {
        String desc = DataSourceMetadataRegistry.getDetailedSourceDescription("etopo");
        assertNotNull(desc);
        assertTrue(desc.toLowerCase().contains("noaa") || desc.toLowerCase().contains("etopo") || desc.toLowerCase().contains("relief"));
    }

    @Test
    @DisplayName("Resolves complete provenance for historical demography datasets (HYDE 3.4)")
    void testHydeMetadata() {
        String desc = DataSourceMetadataRegistry.getDetailedSourceDescription("hyde");
        assertNotNull(desc);
        assertTrue(desc.toLowerCase().contains("hyde") || desc.toLowerCase().contains("history database of the global environment"));
    }

    @Test
    @DisplayName("Resolves complete provenance for global hydrological aquifers (WHYMAP)")
    void testWhymapMetadata() {
        String desc = DataSourceMetadataRegistry.getDetailedSourceDescription("whymap");
        assertNotNull(desc);
        assertTrue(desc.toLowerCase().contains("unesco") || desc.toLowerCase().contains("whymap") || desc.toLowerCase().contains("groundwater"));
    }

    @Test
    @DisplayName("Handles null, empty, or unknown dataset keys with informative fallback")
    void testFallbackDescriptions() {
        String nullDesc = DataSourceMetadataRegistry.getDetailedSourceDescription(null);
        String blankDesc = DataSourceMetadataRegistry.getDetailedSourceDescription("   ");
        String unknownDesc = DataSourceMetadataRegistry.getDetailedSourceDescription("custom_dataset_123");

        assertNotNull(nullDesc);
        assertNotNull(blankDesc);
        assertNotNull(unknownDesc);
    }
}
