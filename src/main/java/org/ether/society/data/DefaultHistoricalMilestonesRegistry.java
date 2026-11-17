/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.ClimateEvent;

import java.util.List;

/**
 * <h1>DefaultHistoricalMilestonesRegistry</h1>
 * <p>
 * Facade registry for canonical planetary milestones and natural shocks,
 * delegating directly to the authoritative catalog file.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class DefaultHistoricalMilestonesRegistry {

    private DefaultHistoricalMilestonesRegistry() {}

    /**
     * Builds and returns the comprehensive catalog from the authoritative data store.
     *
     * @return list of canonical climate events
     */
    public static List<ClimateEvent> buildComprehensiveCatalog() {
        return WikidataEventsFetcher.getOrBuildFullCatalog();
    }
}
