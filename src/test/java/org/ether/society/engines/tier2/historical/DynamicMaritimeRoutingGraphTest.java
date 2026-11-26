/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link DynamicMaritimeRoutingGraph} trans-oceanic navigation routing and physics
 * operating directly over Data-Oriented {@link WorldBuffer} arrays.
 */
public class DynamicMaritimeRoutingGraphTest {

    @BeforeEach
    void setUp() {
        DynamicMaritimeRoutingGraph.clearCache();
    }

    @Test
    @DisplayName("Level 1: Great Circle Distance - Exact spherical haversine formula validation")
    void testGreatCircleDistance() {
        double dist = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(343.5, dist, 5.0, "London-Paris spherical distance should match Earth haversine within 5km");

        double distZero = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(10.0, 20.0, 10.0, 20.0);
        assertEquals(0.0, distZero, 1e-6);

        double distMars = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(0.0, 0.0, 0.0, 180.0, 3389.5);
        assertEquals(Math.PI * 3389.5, distMars, 1.0);
    }

    @Test
    @DisplayName("Level 2: Maritime Routing - Builds WorldBuffer sea lanes, increases commercial flux, and conserves capital")
    void testProcessBufferMaritimeRoute() {
        WorldBuffer buffer = new WorldBuffer(2);
        buffer.getH3Indexes()[0] = 1001L;
        buffer.getH3Indexes()[1] = 1002L;
        Arrays.fill(buffer.getNeighborIndexes()[0], -1);
        Arrays.fill(buffer.getNeighborIndexes()[1], -1);
        buffer.getNeighborIndexes()[0][0] = 1;
        buffer.getNeighborIndexes()[1][0] = 0;

        for (int i = 0; i < 2; i++) {
            buffer.getBiomes()[i] = (byte) Biome.OCEAN.ordinal();
            buffer.getElevation()[i] = -50.0f;
            buffer.getHumanPopulation()[i] = 100.0f;
            buffer.getResourceCapital()[i] = 100.0f;
            buffer.getTemperature()[i] = 20.0f;
        }

        DynamicMaritimeRoutingGraph.processMaritimeBuffer(buffer, 30.0);

        assertFalse(DynamicMaritimeRoutingGraph.getRoutingGraph().isEmpty(), "Active maritime lane must be constructed");
        assertTrue(buffer.getCommercialFluxVolume()[0] > 0.0f, "Active maritime lane must facilitate commercial flux");
        assertTrue(buffer.getCommercialFluxVolume()[1] > 0.0f, "Destination port must receive maritime commercial flux");
        assertEquals(100.0f, buffer.getResourceCapital()[0], 1e-4f, "Maritime routing must conserve physical capital without phantom minting");
        assertEquals(100.0f, buffer.getResourceCapital()[1], 1e-4f, "Maritime routing must conserve physical capital at destination port");
    }
}
