/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.data.WikidataEventsFetcher;

import java.util.List;

/**
 * <h1>DefaultHistoricalEventsRegistry</h1>
 * <p>
 * Facade registry for human civilizational milestones, technological transitions,
 * and macro-historical markers loaded directly from the authoritative catalog file.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class DefaultHistoricalEventsRegistry {

    private DefaultHistoricalEventsRegistry() {
        // Static catalog class
    }

    /**
     * Returns list of canonical historical milestones for the chronicle.
     *
     * @return list of historical milestones
     */
    public static List<HistoricalEvent> getCanonicalEvents() {
        return WikidataEventsFetcher.loadChronicleMilestones();
    }
}
