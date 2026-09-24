/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.ClimateEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DPlaceIngestionServiceTest {

    @Test
    public void testLoadAllSocieties() {
        List<DPlaceIngestionService.DPlaceSociety> societies = DPlaceIngestionService.loadAllSocieties();
        assertNotNull(societies, "Societies list should not be null");
        assertFalse(societies.isEmpty(), "Should load societies from EA, SCCS, and Binford");
        assertTrue(societies.size() > 1000, "Should load over 1,000 distinct pre-industrial societies");

        // Verify key ethnographic societies are present
        boolean hasKung = societies.stream().anyMatch(s -> s.name().contains("Kung"));
        assertTrue(hasKung, "!Kung society must be present in D-PLACE dataset");
    }

    @Test
    public void testLoadDPlaceMilestones() {
        List<ClimateEvent> events = DPlaceIngestionService.loadDPlaceMilestones();
        assertNotNull(events);
        assertFalse(events.isEmpty());

        for (ClimateEvent ev : events) {
            assertEquals("milestone_ethnography", ev.getType());
            assertNotNull(ev.getName());
            assertTrue(ev.getLatitude() >= -90.0 && ev.getLatitude() <= 90.0);
            assertTrue(ev.getLongitude() >= -180.0 && ev.getLongitude() <= 180.0);
        }
    }
}
