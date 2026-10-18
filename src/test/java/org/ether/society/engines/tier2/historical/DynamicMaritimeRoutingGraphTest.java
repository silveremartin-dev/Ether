/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DynamicMaritimeRoutingGraph trans-oceanic navigation routing and physics.
 */
public class DynamicMaritimeRoutingGraphTest {

    @BeforeEach
    void setUp() {
        DynamicMaritimeRoutingGraph.clearCache();
    }

    @Test
    @DisplayName("Level 1: Great Circle Distance - Exact spherical haversine formula validation")
    void testGreatCircleDistance() {
        // Distance between London (51.5074, -0.1278) and Paris (48.8566, 2.3522) ~ 343 km
        double dist = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(51.5074, -0.1278, 48.8566, 2.3522);
        assertEquals(343.5, dist, 5.0, "London-Paris spherical distance should match Earth haversine within 5km");

        // Distance to self is 0.0
        double distZero = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(10.0, 20.0, 10.0, 20.0);
        assertEquals(0.0, distZero, 1e-6);

        // Custom planetary radius (e.g. Mars R=3389.5 km)
        double distMars = DynamicMaritimeRoutingGraph.calculateGreatCircleDistance(0.0, 0.0, 0.0, 180.0, 3389.5);
        assertEquals(Math.PI * 3389.5, distMars, 1.0);
    }

    @Test
    @DisplayName("Level 2: Maritime Routing - Dynamically builds routes, reduces movement friction and boosts capital")
    void testProcessHybridMaritimeRoute() {
        List<H3Cell> cells = new ArrayList<>();

        // Port A (coastal)
        H3Cell portA = new H3Cell(1001L, 0.0, 0.0);
        portA.setIsCoastal(true);
        portA.setBiome(Biome.OCEAN);
        portA.setElevation(-50.0);
        portA.setTechnologyLevel(5.0);
        portA.setResourceCapital(100.0);
        portA.setMovementFriction(1.0);
        portA.setTemperature(20.0);
        cells.add(portA);

        // Port B (coastal, ~222 km away at 2 degrees latitude)
        H3Cell portB = new H3Cell(1002L, 2.0, 0.0);
        portB.setIsCoastal(true);
        portB.setBiome(Biome.OCEAN);
        portB.setElevation(-50.0);
        portB.setTechnologyLevel(5.0);
        portB.setResourceCapital(100.0);
        portB.setMovementFriction(1.0);
        portB.setTemperature(20.0);
        cells.add(portB);

        // Run processHybrid over 30 days
        DynamicMaritimeRoutingGraph.processHybrid(cells, 30.0);

        // Both nodes are within range (< 300km) so routes should be established
        // Hydrodynamic friction should be reduced by 70% (down to 0.3)
        assertEquals(0.30, portA.getMovementFriction(), 1e-4, "Active maritime lane should decrease movement friction to 0.30");
        assertEquals(0.30, portB.getMovementFriction(), 1e-4);

        // Capital should increase with connectivity factor (1.0 + 0.05 * 1 = 1.05 -> capital *= (1.0 + 0.02 * 1.05 * 1.0) = 1.021)
        assertTrue(portA.getResourceCapital() > 100.0, "Capital should receive trade boost from maritime connectivity");
        assertEquals(102.1, portA.getResourceCapital(), 0.1);
    }
}
