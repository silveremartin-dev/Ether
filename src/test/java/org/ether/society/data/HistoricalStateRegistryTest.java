/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Historical Integrity Test Suite for {@link HistoricalStateRegistry}.
 * Verifies piecewise-constant step-lookup of geopolitical epochs from treaties_and_transitions.json,
 * interval coverage, boundary conditions, and sovereignty seeds derivation.
 */
public class HistoricalStateRegistryTest {

    @Test
    @DisplayName("Verify HistoricalStateRegistry loads all epochs in chronological order")
    public void testRegistryChronologicalOrdering() {
        List<HistoricalStateRegistry.HistoricalEpoch> epochs = HistoricalStateRegistry.getAllEpochs();
        assertNotNull(epochs, "Epoch registry must not be null");
        assertFalse(epochs.isEmpty(), "Epoch registry must contain historical transition intervals");

        // Verify strictly monotonic tStart ascending
        for (int i = 0; i < epochs.size() - 1; i++) {
            HistoricalStateRegistry.HistoricalEpoch current = epochs.get(i);
            HistoricalStateRegistry.HistoricalEpoch next = epochs.get(i + 1);

            assertTrue(current.tStart() <= next.tStart(),
                    "Epochs must be sorted chronologically by tStart: " + current.id() + " vs " + next.id());
            assertTrue(current.tStart() < current.tEnd(),
                    "Epoch interval [tStart, tEnd) must have tStart < tEnd for " + current.id());
            assertNotNull(current.label(), "Epoch label must not be null");
            assertNotNull(current.polities(), "Polities list must not be null for " + current.id());
        }
    }

    @Test
    @DisplayName("Verify getActiveEpoch piecewise step-constant lookup")
    public void testGetActiveEpochLookup() {
        // Query prehistoric year
        HistoricalStateRegistry.HistoricalEpoch prehistoric = HistoricalStateRegistry.getActiveEpoch(-100000);
        assertNotNull(prehistoric);
        assertEquals(HistoricalStateRegistry.getAllEpochs().get(0).id(), prehistoric.id());

        // Query Classical Antiquity (~0 CE)
        HistoricalStateRegistry.HistoricalEpoch classical = HistoricalStateRegistry.getActiveEpoch(0);
        assertNotNull(classical);
        assertTrue(classical.tStart() <= 0 && classical.tEnd() > 0,
                "Classical epoch must bracket year 0 CE");

        // Query Modern Era (2026 CE)
        HistoricalStateRegistry.HistoricalEpoch modern = HistoricalStateRegistry.getActiveEpoch(2026);
        assertNotNull(modern);
        assertTrue(modern.tStart() <= 2026 && modern.tEnd() > 2026,
                "Modern epoch must bracket year 2026 CE");

        // Query Far Future
        HistoricalStateRegistry.HistoricalEpoch farFuture = HistoricalStateRegistry.getActiveEpoch(3000);
        assertNotNull(farFuture);
        List<HistoricalStateRegistry.HistoricalEpoch> all = HistoricalStateRegistry.getAllEpochs();
        assertEquals(all.get(all.size() - 1).id(), farFuture.id(),
                "Far future query must return the latest epoch");
    }

    @Test
    @DisplayName("Verify getEpochsInRange interval filtering")
    public void testGetEpochsInRange() {
        List<HistoricalStateRegistry.HistoricalEpoch> range =
                HistoricalStateRegistry.getEpochsInRange(1500, 1950);

        assertNotNull(range);
        assertFalse(range.isEmpty());

        for (HistoricalStateRegistry.HistoricalEpoch epoch : range) {
            assertTrue(epoch.tStart() <= 1950 && epoch.tEnd() > 1500,
                    "Every returned epoch must overlap [1500, 1950]");
        }
    }

    @Test
    @DisplayName("Verify getActiveSovereigntySeeds generates valid cultural seeds")
    public void testGetActiveSovereigntySeeds() {
        List<OrographicGlottologPropagator.CulturalSeed> seeds2026 =
                HistoricalStateRegistry.getActiveSovereigntySeeds(2026);

        assertNotNull(seeds2026, "Sovereignty seeds must not be null");
        assertFalse(seeds2026.isEmpty(), "Modern epoch must have defined sovereign polities");

        for (OrographicGlottologPropagator.CulturalSeed seed : seeds2026) {
            assertTrue(seed.lat() >= -90.0 && seed.lat() <= 90.0, "Latitude must be valid");
            assertTrue(seed.lon() >= -180.0 && seed.lon() <= 180.0, "Longitude must be valid");
            assertNotNull(seed.name(), "Seed polity name must not be null");
        }
    }
}
