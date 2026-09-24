/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.ClimateEvent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WikidataEventsFetcherTest {

    @Test
    public void testGetOrBuildFullCatalog() {
        List<ClimateEvent> catalog = WikidataEventsFetcher.getOrBuildFullCatalog();
        assertNotNull(catalog);
        assertFalse(catalog.isEmpty(), "Catalog should contain milestones and historical events");

        File catalogFile = new File(WikidataEventsFetcher.CATALOG_FILE_PATH);
        assertTrue(catalogFile.exists(), "Catalog JSON file should be saved on disk");
        assertTrue(catalogFile.length() > 0, "Catalog file should not be empty");

        boolean hasOutAf = catalog.stream().anyMatch(e -> e.getName().contains("Afrique"));
        assertTrue(hasOutAf, "Catalog must include Out of Africa milestone");
    }
}
